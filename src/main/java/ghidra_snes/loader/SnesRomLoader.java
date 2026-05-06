/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.loader;

import ghidra.app.util.MemoryBlockUtils;
import ghidra.app.util.bin.ByteProvider;
import ghidra.app.util.importer.MessageLog;
import ghidra.app.util.opinion.AbstractProgramLoader;
import ghidra.app.util.opinion.LoadException;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.app.util.opinion.Loaded;
import ghidra.app.util.opinion.LoaderTier;
import ghidra.program.database.mem.FileBytes;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressOverflowException;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.util.exception.CancelledException;
import ghidra.util.task.TaskMonitor;
import ghidra_snes.SnesCartridge;
import ghidra_snes.common.RomType;
import ghidra_snes.common.RomType.MappingChunk;
import ghidra_snes.common.registers.Vectors;
import ghidra_snes.ghidra.LanguageHelper;
import ghidra_snes.ghidra.MemoryMap;
import ghidra_snes.options.SnesOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

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

  /**
   * Ghidra glue
   *
   * First ROM checker/parser. If it's treated as `Raw`, we don't have `LoadSpec`.
   */
  @Override
  public Collection<LoadSpec> findSupportedLoadSpecs(ByteProvider provider) throws IOException {
    SnesCartridge cartridge = new SnesCartridge(provider);

    if (cartridge.getRomType() == RomType.Raw) {
      return List.of();
    }

    List<LoadSpec> loadSpecs = LanguageHelper.find65816LoadSpecs(this, this.getLanguageService());
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
    SnesCartridge cartridge = new SnesCartridge(provider);
    if (cartridge.getRomType() == RomType.Raw) {
      throw new LoadException("Cannot load unrecognized ROM type");
    }

    // TODO: Augment data with metadata from Super Famicom.bml
    // ie:
    // SnesCartridge cartridge = new SnesCartridge(provider).with_bml();
    long romSize = cartridge.getRomSizeBytes();
    if (romSize <= 0) {
      throw new LoadException("ROM file contains no data after header adjustment.");
    }

    settings
        .log()
        .appendMsg(
            String.format(
                "%s: mode=%s source=%s romOffset=0x%X romSize=0x%X",
                LOADER_NAME,
                cartridge.getRomType(),
                cartridge.getMetadataSource(),
                cartridge.getRomOffset(),
                romSize));

    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    FileBytes romFileBytes;
    try (InputStream is = provider.getInputStream(0)) {
      romFileBytes =
          memory.createFileBytes(provider.getName(), 0, provider.length(), is, settings.monitor());
    }

    RomType romType = cartridge.getRomType();
    for (MappingChunk chunk : romType.mappingChunks(romSize)) {
      mapChunkToCanonicalSpace(
          program,
          space,
          provider,
          romFileBytes,
          String.format("bank_%02x_%s", chunk.bank(), romType.toString().toLowerCase()),
          chunk.cpuAddress(),
          // We need to add the ROM offset in case there's a copier header
          cartridge.getRomOffset() + chunk.fileOffset(),
          chunk.requestedSize(),
          settings.monitor(),
          settings.log());
    }

    try {
      MemoryMap.createBlockSystemRegion(program);
      MemoryMap.createBlockWram(program);
      MemoryMap.createBlockHighHalfRomMirrors(program, romType, 0x00);
      Vectors.createVectorLabels(program);

      SnesOptions.initializeFromCartridge(program, cartridge);
    } catch (Exception e) {
      settings
          .log()
          .appendMsg("Could not add SNES helper mappings/labels (" + e.getMessage() + ")");
    }
  }

  /**
   * Creates an initialized memory block backed by the original ROM FileBytes.
   *
   * Using FileBytes rather than an InputStream-backed block lets Ghidra display byte sources as
   * {@code filename[offset, length]} in the Memory Map.
   */
  private void mapChunkToCanonicalSpace(
      Program program,
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
    if (fileOffset >= provider.length()) {
      return;
    }

    long size = Math.min(requestedSize, provider.length() - fileOffset);

    Address start = space.getAddress(cpuAddress);
    try {
      MemoryBlockUtils.createInitializedBlock(
          program,
          false,
          blockName,
          start,
          romFileBytes,
          fileOffset,
          size,
          String.format("ROM file offset 0x%06X", fileOffset),
          "ROM:Canonical",
          true,
          false,
          true,
          log);
    } catch (AddressOverflowException e) {
      throw new IOException("Failed to map block " + blockName, e);
    }
  }
}
