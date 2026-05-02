/* (C) Arnaud 'red' Rouyer 2026 */
package snesloader;

import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.importer.MessageLog;
import ghidra.app.util.opinion.AbstractProgramLoader;
import ghidra.app.util.opinion.LoadException;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.app.util.opinion.Loaded;
import ghidra.app.util.opinion.LoaderTier;
import ghidra.framework.options.Options;
import ghidra.framework.store.LockException;
import ghidra.program.database.mem.FileBytes;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressOverflowException;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.lang.Endian;
import ghidra.program.model.lang.LanguageCompilerSpecPair;
import ghidra.program.model.lang.LanguageCompilerSpecQuery;
import ghidra.program.model.lang.LanguageNotFoundException;
import ghidra.program.model.lang.Processor;
import ghidra.program.model.lang.ProcessorNotFoundException;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.ProgramUserData;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryConflictException;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Loader for SNES ROM dumps. It auto-detects LoROM/HiROM, maps ROM banks into
 * CPU-like addresses, and creates WRAM.
 */
public class SnesRomLoader extends AbstractProgramLoader {

  private static final String LOADER_NAME = "SNES ROM Loader";
  private static final String OPTIONS_NAME = "SNES ROM";
  private static final long SMC_HEADER_SIZE = 0x200L;
  private static final long LOROM_HEADER_OFFSET = 0x7fc0L;
  private static final long HIROM_HEADER_OFFSET = 0xffc0L;
  private static final int MIN_CONFIDENCE_SCORE = 16;
  private static final String SNES_65816_LANGUAGE_ID = "65816:LE:24:snes";
  private static final String SNES_65816_COMPILER_SPEC_ID = "default";

  private enum MappingMode {
    LOROM("LoROM"),
    HIROM("HiROM"),
    UNKNOWN("Unknown");

    private final String label;

    MappingMode(String label) {
      this.label = label;
    }
  }

  private static final class DetectionResult {
    private final MappingMode mode;
    private final long romOffset;
    private final int loScore;
    private final int hiScore;

    DetectionResult(MappingMode mode, long romOffset, int loScore, int hiScore) {
      this.mode = mode;
      this.romOffset = romOffset;
      this.loScore = loScore;
      this.hiScore = hiScore;
    }

    private int bestScore() {
      return Math.max(loScore, hiScore);
    }
  }

  @Override
  public String getName() {
    return LOADER_NAME;
  }

  @Override
  public LoaderTier getTier() {
    return LoaderTier.SPECIALIZED_TARGET_LOADER;
  }

  @Override
  public int getTierPriority() {
    return 50;
  }

  @Override
  public Collection<LoadSpec> findSupportedLoadSpecs(ByteProvider provider) throws IOException {
    List<LoadSpec> loadSpecs = new ArrayList<>();
    DetectionResult detection = detectBestMapping(provider);

    if (detection.mode == MappingMode.UNKNOWN || detection.bestScore() < MIN_CONFIDENCE_SCORE) {
      return loadSpecs;
    }

    loadSpecs.addAll(find65816LoadSpecs());
    if (loadSpecs.isEmpty()) {
      loadSpecs.add(new LoadSpec(this, 0, true));
    }
    return loadSpecs;
  }

  @Override
  protected List<Loaded<Program>> loadProgram(ImporterSettings settings)
      throws IOException, CancelledException {
    Program program = createProgram(settings);
    Loaded<Program> loaded = new Loaded<>(program, settings);

    boolean success = false;
    try {
      loadInto(program, settings);
      createDefaultMemoryBlocks(program, settings);
      success = true;
      return List.of(loaded);
    } finally {
      if (!success) {
        loaded.close();
      }
    }
  }

  @Override
  protected void loadProgramInto(Program program, ImporterSettings settings)
      throws IOException, LoadException, CancelledException {
    ByteProvider provider = settings.provider();
    DetectionResult detection = detectBestMapping(provider);
    long romSize = provider.length() - detection.romOffset;
    if (romSize <= 0) {
      throw new LoadException("ROM file contains no data after header adjustment.");
    }
    String title = readTitle(provider, getHeaderOffsetForTitle(detection));
    storeSnesRomOptions(program, detection, title);

    settings
        .log()
        .appendMsg(
            String.format(
                "%s: mode=%s loScore=%d hiScore=%d romOffset=0x%X romSize=0x%X",
                LOADER_NAME,
                detection.mode.label,
                detection.loScore,
                detection.hiScore,
                detection.romOffset,
                romSize));

    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    FileBytes romFileBytes;
    try (InputStream is = provider.getInputStream(0)) {
      romFileBytes =
          memory.createFileBytes(provider.getName(), 0, provider.length(), is, settings.monitor());
    }

    if (detection.mode == MappingMode.LOROM) {

      mapLoRom(
          program,
          memory,
          space,
          provider,
          romFileBytes,
          detection.romOffset,
          romSize,
          settings.monitor(),
          settings.log());
      mapMmio(memory, space, settings.log());
    } else if (detection.mode == MappingMode.HIROM) {
      mapHiRom(
          program,
          memory,
          space,
          provider,
          romFileBytes,
          detection.romOffset,
          romSize,
          settings.monitor(),
          settings.log());
      mapMmio(memory, space, settings.log());
    } else {
      settings.log().appendMsg("Unable to confidently detect LoROM/HiROM; mapping as raw ROM.");
      mapRawRom(
          program,
          memory,
          space,
          provider,
          romFileBytes,
          detection.romOffset,
          romSize,
          settings.monitor(),
          settings.log());
    }

    mapWram(memory, space, settings.log());
  }

  private void storeSnesRomOptions(Program program, DetectionResult detection, String title) {
    ProgramUserData userData = program.getProgramUserData();
    int transactionId = userData.startTransaction();

    try {
      Options opts = userData.getOptions(OPTIONS_NAME);
      opts.setString("cart.mapper", detection.mode.label);
      opts.setString("cart.title", title);
      opts.setBoolean("rom.hasSmcHeader", detection.romOffset == SMC_HEADER_SIZE);
      opts.setBoolean("memory.display_mmio", true);
      opts.setBoolean("memory.display_wram", true);
      opts.setBoolean("memory.display_mirrors", false);
    } finally {
      userData.endTransaction(transactionId);
    }
  }

  private long getHeaderOffsetForTitle(DetectionResult detection) {
    if (detection.mode == MappingMode.HIROM) {
      return detection.romOffset + HIROM_HEADER_OFFSET;
    }
    if (detection.mode == MappingMode.LOROM) {
      return detection.romOffset + LOROM_HEADER_OFFSET;
    }
    if (detection.hiScore > detection.loScore) {
      return detection.romOffset + HIROM_HEADER_OFFSET;
    }
    return detection.romOffset + LOROM_HEADER_OFFSET;
  }

  private String readTitle(ByteProvider provider, long headerOffset) throws IOException {
    if (headerOffset < 0 || headerOffset + 21L > provider.length()) {
      return "";
    }

    byte[] titleBytes = provider.readBytes(headerOffset, 21);
    StringBuilder title = new StringBuilder();
    for (byte b : titleBytes) {
      int c = b & 0xff;
      if (c == 0x00) {
        break;
      }
      if (c >= 0x20 && c <= 0x7e) {
        title.append((char) c);
      } else {
        title.append(' ');
      }
    }
    return title.toString().trim();
  }

  private List<LoadSpec> find65816LoadSpecs() {
    List<LoadSpec> specs = new ArrayList<>();
    getLanguageService(); // Ensure processors are loaded.

    try {
      LanguageCompilerSpecPair preferredPair =
          new LanguageCompilerSpecPair(SNES_65816_LANGUAGE_ID, SNES_65816_COMPILER_SPEC_ID);
      if (getLanguageService().getLanguage(preferredPair.languageID) != null) {
        specs.add(new LoadSpec(this, 0, preferredPair, true));
        return specs;
      }

      Processor processor = Processor.toProcessor("65816");
      LanguageCompilerSpecQuery query =
          new LanguageCompilerSpecQuery(processor, Endian.LITTLE, 24, "snes", null);
      List<LanguageCompilerSpecPair> pairs =
          getLanguageService().getLanguageCompilerSpecPairs(query);
      for (LanguageCompilerSpecPair pair : pairs) {
        specs.add(new LoadSpec(this, 0, pair, true));
      }
    } catch (LanguageNotFoundException ignored) {
      // 65816 language module isn't installed. We'll use incomplete LoadSpec fallback.
    } catch (ProcessorNotFoundException ignored) {
      // 65816 processor module isn't installed. We'll use incomplete LoadSpec fallback.
    }

    return specs;
  }

  private DetectionResult detectBestMapping(ByteProvider provider) throws IOException {
    List<Long> candidateOffsets = new ArrayList<>();
    candidateOffsets.add(0L);
    if (provider.length() >= SMC_HEADER_SIZE && (provider.length() % 0x8000L) == SMC_HEADER_SIZE) {
      candidateOffsets.add(SMC_HEADER_SIZE);
    }

    DetectionResult best = null;
    for (long romOffset : candidateOffsets) {
      int lo = scoreHeader(provider, romOffset + LOROM_HEADER_OFFSET, true);
      int hi = scoreHeader(provider, romOffset + HIROM_HEADER_OFFSET, false);

      MappingMode mode = MappingMode.UNKNOWN;
      if (lo > hi) {
        mode = MappingMode.LOROM;
      } else if (hi > lo) {
        mode = MappingMode.HIROM;
      }

      DetectionResult candidate = new DetectionResult(mode, romOffset, lo, hi);
      if (best == null || candidate.bestScore() > best.bestScore()) {
        best = candidate;
      }
    }

    if (best == null) {
      return new DetectionResult(MappingMode.UNKNOWN, 0L, Integer.MIN_VALUE, Integer.MIN_VALUE);
    }
    return best;
  }

  private int scoreHeader(ByteProvider provider, long headerOffset, boolean isLoRom)
      throws IOException {
    if (headerOffset < 0 || (headerOffset + 0x40L) > provider.length()) {
      return -999;
    }

    int score = 0;

    // Title: 21 mostly printable ASCII bytes.
    for (int i = 0; i < 21; i++) {
      int b = u8(provider, headerOffset + i);
      if (b >= 0x20 && b <= 0x7e) {
        score++;
      } else {
        score -= 2;
      }
    }

    int mapMode = u8(provider, headerOffset + 0x15);
    int lowNibble = mapMode & 0x0f;
    if (isLoRom && (lowNibble == 0x0 || lowNibble == 0x3)) {
      score += 8;
    }
    if (!isLoRom && (lowNibble == 0x1 || lowNibble == 0x5)) {
      score += 8;
    }

    int complement = u16(provider, headerOffset + 0x1c);
    int checksum = u16(provider, headerOffset + 0x1e);
    if (((complement ^ checksum) & 0xffff) == 0xffff) {
      score += 8;
    }

    int resetVector = u16(provider, headerOffset + 0x3c);
    if (resetVector >= 0x8000 && resetVector <= 0xffff) {
      score += 8;
    }
    if (resetVector == 0xffff || resetVector == 0x0000) {
      score -= 8;
    }

    return score;
  }

  private void mapRawRom(
      Program program,
      Memory memory,
      AddressSpace space,
      ByteProvider provider,
      FileBytes romFileBytes,
      long romOffset,
      long romSize,
      TaskMonitor monitor,
      MessageLog log)
      throws IOException, CancelledException {
    mapInitializedBlock(
        program,
        memory,
        space,
        provider,
        romFileBytes,
        "rom",
        0x000000L,
        romOffset,
        romSize,
        monitor,
        log);
  }

  private void mapLoRom(
      Program program,
      Memory memory,
      AddressSpace space,
      ByteProvider provider,
      FileBytes romFileBytes,
      long romOffset,
      long romSize,
      TaskMonitor monitor,
      MessageLog log)
      throws IOException, CancelledException {
    long banks = (romSize + 0x7fffL) / 0x8000L;
    for (long bank = 0; bank < banks && bank <= 0x7dL; bank++) {
      long fileOffset = romOffset + (bank * 0x8000L);
      long cpuAddress = (bank << 16) | 0x8000L;
      mapInitializedBlock(
          program,
          memory,
          space,
          provider,
          romFileBytes,
          String.format("bank_%02x_lorom", bank),
          cpuAddress,
          fileOffset,
          0x8000L,
          monitor,
          log);
    }
  }

  private void mapHiRom(
      Program program,
      Memory memory,
      AddressSpace space,
      ByteProvider provider,
      FileBytes romFileBytes,
      long romOffset,
      long romSize,
      TaskMonitor monitor,
      MessageLog log)
      throws IOException, CancelledException {
    long banks = (romSize + 0xffffL) / 0x10000L;
    for (long i = 0; i < banks; i++) {
      long bank = 0x40L + i;
      if (bank > 0x7fL) {
        break;
      }

      long fileOffset = romOffset + (i * 0x10000L);
      long cpuAddress = bank << 16;
      mapInitializedBlock(
          program,
          memory,
          space,
          provider,
          romFileBytes,
          String.format("bank_%02x_hirom", bank),
          cpuAddress,
          fileOffset,
          0x10000L,
          monitor,
          log);
    }
  }

  private void mapMmio(Memory memory, AddressSpace space, MessageLog log) {
    record MmioRegion(String name, long start, long size) {}
    List<MmioRegion> regions =
        List.of(
            new MmioRegion("snes_mmio_ppu", 0x002100, 0x40),
            new MmioRegion("snes_mmio_apu", 0x002140, 0x40),
            new MmioRegion("snes_mmio_wram", 0x002180, 0x04),
            new MmioRegion("snes_mmio_cpu", 0x004200, 0x20),
            new MmioRegion("snes_mmio_dma", 0x004300, 0x80));

    for (MmioRegion r : regions) {
      try {
        MemoryBlock block =
            memory.createUninitializedBlock(r.name(), space.getAddress(r.start()), r.size(), false);
        block.setRead(true);
        block.setWrite(true);
        block.setExecute(false);
      } catch (Exception e) {
        log.appendMsg("MMIO skip: " + r.name() + " (" + e.getMessage() + ")");
      }
    }
  }

  private void mapWram(Memory memory, AddressSpace space, MessageLog log) throws IOException {
    long cpuAddress = 0x7e0000L;
    long size = 0x20000L;

    Address start = space.getAddress(cpuAddress);
    try {
      MemoryBlock block = memory.createUninitializedBlock("bank_7e_wram", start, size, false);
      block.setRead(true);
      block.setWrite(true);
      block.setExecute(false);
      log.appendMsg(String.format("%-14s CPU 0x%06X size 0x%X", "snes_wram", cpuAddress, size));
    } catch (MemoryConflictException e) {
      log.appendMsg("skip conflict: snes_wram at 0x" + Long.toHexString(cpuAddress));
    } catch (LockException | AddressOverflowException e) {
      throw new IOException("Failed to create WRAM block", e);
    }
  }

  private void mapInitializedBlock(
      Program program,
      Memory memory,
      AddressSpace space,
      ByteProvider provider,
      FileBytes romFileBytes,
      String blockName,
      long cpuAddress,
      long fileOffset,
      long requestedSize,
      TaskMonitor monitor,
      MessageLog log)
      throws IOException, CancelledException {
    if (requestedSize <= 0 || fileOffset >= provider.length()) {
      return;
    }
    long size = Math.min(requestedSize, provider.length() - fileOffset);
    if (size <= 0) {
      return;
    }

    Address start = space.getAddress(cpuAddress);
    try {
      MemoryBlock block =
          memory.createInitializedBlock(blockName, start, romFileBytes, fileOffset, size, false);
      block.setRead(true);
      block.setWrite(false);
      block.setExecute(true);
      block.setComment(String.format("SNES ROM file offset 0x%06X", fileOffset));
      log.appendMsg(
          String.format(
              "%-14s file[0x%06X,+0x%X] -> CPU 0x%06X", blockName, fileOffset, size, cpuAddress));
    } catch (MemoryConflictException e) {
      log.appendMsg("skip conflict: " + blockName + " at 0x" + Long.toHexString(cpuAddress));
    } catch (LockException | AddressOverflowException e) {
      throw new IOException("Failed to map block " + blockName, e);
    }
  }

  private int u8(ByteProvider provider, long offset) throws IOException {
    return provider.readByte(offset) & 0xff;
  }

  private int u16(ByteProvider provider, long offset) throws IOException {
    return u8(provider, offset) | (u8(provider, offset + 1) << 8);
  }
}
