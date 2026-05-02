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
import snescommon.SnesCommon;

/**
 * SNES ROM loader.
 *
 * Detects LoROM/HiROM layouts, maps ROM banks into CPU address space,
 * and initializes common SNES memory regions (WRAM, MMIO).
 *
 * Also extracts metadata such as title and SRAM size and stores them
 * as hidden ProgramUserData options.
 */
public class SnesRomLoader extends AbstractProgramLoader {
  private static final String LOADER_NAME = "SNES ROM Loader";
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

  /**
   * Main loader entry point.
   *
   * Detects mapping mode, maps ROM into memory, initializes MMIO/WRAM,
   * and stores metadata into ProgramUserData.
   */
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
    long headerOffset = getHeaderOffsetForTitle(detection);
    String title = readTitle(provider, headerOffset);
    int sramSize = readSramSize(provider, headerOffset);

    storeSnesRomOptions(program, detection, title, sramSize);

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

    if (detection.mode != MappingMode.UNKNOWN) {
      try {
        mapMmio(program, settings.log());
        SnesCommon.createVectorLabels(program);
      } catch (Exception e) {
        settings
            .log()
            .appendMsg("Could not add SNES helper mappings/labels (" + e.getMessage() + ")");
      }
    }

    mapWram(program, settings.log());
  }

  /**
   * Stores SNES metadata and default memory toggle states into ProgramUserData.
   *
   * @param program target program
   * @param detection mapping detection result
   * @param title ROM title
   * @param sramSize SRAM size in bytes
   */
  private void storeSnesRomOptions(
      Program program, DetectionResult detection, String title, int sramSize) {
    ProgramUserData userData = program.getProgramUserData();
    int transactionId = userData.startTransaction();

    try {
      Options opts = userData.getOptions(SnesCommon.OPTIONS_NAME);
      opts.setString(SnesCommon.OPT_CART_MAPPER, detection.mode.label);
      opts.setString(SnesCommon.OPT_CART_TITLE, title);
      opts.setInt(SnesCommon.OPT_CART_SRAM_SIZE, sramSize);
      opts.setBoolean(SnesCommon.OPT_ROM_HAS_SMC_HEADER, detection.romOffset == SMC_HEADER_SIZE);
      opts.setBoolean(SnesCommon.OPT_MEMORY_DISPLAY_MMIO, true);
      opts.setBoolean(SnesCommon.OPT_MEMORY_DISPLAY_SRAM, false);
      opts.setBoolean(SnesCommon.OPT_MEMORY_DISPLAY_WRAM, true);
      opts.setBoolean(SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS, false);
    } finally {
      userData.endTransaction(transactionId);
    }
  }

  /**
   * Returns the absolute SNES internal header offset for metadata reads.
   *
   * The title, SRAM size, checksum, and vectors live inside the internal ROM header. The offset
   * differs between LoROM and HiROM and must also account for an optional 0x200-byte SMC header.
   *
   * @param detection mapping detection result
   * @return absolute file offset of the internal header
   */
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

  /**
   * Reads the 21-byte SNES title field from the internal ROM header.
   *
   * Non-printable bytes are replaced with spaces so malformed titles do not leak control
   * characters into logs or metadata.
   *
   * @param provider ROM byte provider
   * @param headerOffset absolute file offset of the internal header
   * @return trimmed ROM title, or an empty string if unavailable
   */
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

  /**
   * Reads the cartridge SRAM size from the SNES internal header.
   *
   * The SRAM size byte is stored at header offset {@code 0x18}. For standard SNES headers, this
   * value encodes {@code 2^N KiB}; {@code 0} means no SRAM. The returned value is stored in hidden
   * ProgramUserData so the UI plugin can enable or disable the SRAM mapping action.
   *
   * @param provider ROM byte provider
   * @param headerOffset absolute file offset of the internal header
   * @return SRAM size in bytes, or 0 if the header declares no SRAM
   */
  private int readSramSize(ByteProvider provider, long headerOffset) throws IOException {
    if (headerOffset < 0 || headerOffset + 0x19L > provider.length()) {
      return 0;
    }

    int sramExp = u8(provider, headerOffset + 0x18);
    if (sramExp == 0) {
      return 0;
    }

    // size = 2^N KB
    return (1 << sramExp) * 1024;
  }

  /**
   * Finds the preferred SNES 65816 language/compiler spec pair.
   *
   * The loader first asks for the exact bundled language id. If it cannot be found, it falls back
   * to a query for any compatible 65816 little-endian 24-bit SNES language. If no language module
   * is installed, the caller will provide an incomplete LoadSpec so Ghidra can still present the
   * loader as a candidate.
   *
   * @return matching load specs, possibly empty
   */
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

  /**
   * Detects the most likely SNES mapping mode and optional SMC header offset.
   *
   * The detector scores LoROM and HiROM header candidates at file offset 0 and, when the file size
   * suggests it, at offset 0x200 for copier-headered `.smc` dumps. The highest-scoring candidate is
   * returned.
   *
   * @param provider ROM byte provider
   * @return best mapping detection result
   */
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

  /**
   * Scores a possible SNES internal header.
   *
   * The score is heuristic, not a full validation. It checks whether the title looks printable,
   * whether the mapper nibble matches the expected LoROM/HiROM family, whether checksum and
   * complement agree, and whether the reset vector looks plausible.
   *
   * @param provider ROM byte provider
   * @param headerOffset absolute file offset of the candidate internal header
   * @param isLoRom true when scoring as LoROM, false when scoring as HiROM
   * @return heuristic confidence score
   */
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

  /**
   * Maps the ROM as a single raw block when LoROM/HiROM detection is inconclusive.
   *
   * This fallback keeps the import usable without pretending that the SNES bus layout is known.
   */
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

  /**
   * Maps a LoROM image into canonical SNES CPU ROM banks.
   *
   * LoROM uses 32 KiB ROM banks. The canonical ROM window lives at {@code 80-FF:8000-FFFF};
   * lower banks {@code 00-7D:8000-FFFF} are mirrors and can be added later by the UI plugin.
   * Blocks are backed by FileBytes so Ghidra's Memory Map shows the original filename and file
   * offsets.
   */
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
    for (long i = 0; i < banks; i++) {
      long bank = 0x80L + i;
      if (bank > 0xffL) {
        break;
      }

      long fileOffset = romOffset + (i * 0x8000L);
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

  /**
   * Maps a HiROM image into canonical SNES CPU ROM banks.
   *
   * HiROM uses 64 KiB ROM banks. The canonical ROM window lives at {@code C0-FF:0000-FFFF};
   * lower banks {@code 40-7F:0000-FFFF} are mirrors and can be added later by the UI plugin.
   * Blocks are backed by FileBytes so Ghidra's Memory Map shows the original filename and file
   * offsets.
   */
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
      long bank = 0xc0L + i;
      if (bank > 0xffL) {
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

  /**
   * Creates SNES MMIO blocks and labels via the shared SNES helpers.
   *
   * Errors are logged but do not abort the import, because ROM mapping is still useful even if
   * auxiliary MMIO regions fail to be created.
   */
  private void mapMmio(Program program, MessageLog log) {
    try {
      SnesCommon.createMemoryBlocksMmio(program);
    } catch (Exception e) {
      log.appendMsg("Error adding MMIO blocks (" + e.getMessage() + ")");
    }
  }

  /**
   * Creates the main SNES WRAM block via the shared SNES helpers.
   *
   * WRAM creation errors are treated as import errors because WRAM is part of the default SNES bus
   * view used by this loader.
   */
  private void mapWram(Program program, MessageLog log) throws IOException {
    try {
      SnesCommon.createMemoryBlockWram(program);
    } catch (MemoryConflictException e) {
      log.appendMsg("Error creating WRAM block: (" + e.getMessage() + ")");
    } catch (Exception e) {
      throw new IOException("Failed to create WRAM block", e);
    }
  }

  /**
   * Creates an initialized memory block backed by the original ROM FileBytes.
   *
   * Using FileBytes rather than an InputStream-backed block lets Ghidra display byte sources as
   * {@code filename[offset, length]} in the Memory Map.
   */
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

  /**
   * Reads an unsigned byte from the ROM.
   */
  private int u8(ByteProvider provider, long offset) throws IOException {
    return provider.readByte(offset) & 0xff;
  }

  /**
   * Reads an unsigned little-endian 16-bit value from the ROM.
   */
  private int u16(ByteProvider provider, long offset) throws IOException {
    return u8(provider, offset) | (u8(provider, offset + 1) << 8);
  }
}
