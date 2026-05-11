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
import ghidra_snes.SnesCartridge;
import ghidra_snes.common.RomMapType;
import ghidra_snes.common.RomMapType.MappingChunk;
import ghidra_snes.common.registers.Vectors;
import ghidra_snes.ghidra.LanguageHelper;
import ghidra_snes.ghidra.MemoryMap;
import ghidra_snes.options.SnesOptions;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;


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

  @Override
  public boolean supportsLoadIntoProgram(Program _program) {
    return false;
  }

  @Override
  protected void loadProgramInto(Program program, ImporterSettings settings)
      throws IOException, LoadException, CancelledException {
    // Unsupported.
  }

  @Override
  public Collection<LoadSpec> findSupportedLoadSpecs(ByteProvider provider) throws IOException {
    SnesCartridge cartridge;
    try {
      cartridge = new SnesCartridge(provider);
    } catch (IllegalStateException e) {
      return List.of();
    }

    if (cartridge.getRomMapType() == RomMapType.UNKNOWN) {
      return List.of();
    }

    List<LoadSpec> loadSpecs = LanguageHelper.find65816LoadSpecs(this, this.getLanguageService());
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
    int tx = program.startTransaction("Load SNES ROM");
    try {
      loadRom(program, settings);
      success = true;
      return List.of(loaded);
    } finally {
      program.endTransaction(tx, success);
      if (!success) {
        loaded.close();
      }
    }
  }

  protected void loadRom(Program program, ImporterSettings settings)
      throws IOException, LoadException, CancelledException {
    ByteProvider provider = settings.provider();
    SnesCartridge cartridge;
    try {
      cartridge = new SnesCartridge(provider);
    } catch (IllegalStateException e) {
      throw new LoadException("Cannot load unrecognized ROM type", e);
    }

    if (cartridge.getRomMapType() == RomMapType.UNKNOWN) {
      throw new LoadException("Cannot load unrecognized ROM type");
    }

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
                cartridge.getRomMapType(),
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

    RomMapType romType = cartridge.getRomMapType();
    for (MappingChunk chunk : romType.mappingChunks(romSize)) {
      mapChunkToCanonicalSpace(
          program,
          space,
          provider,
          romFileBytes,
          String.format("bank_%02x_%s", chunk.bank(), romType.toString().toLowerCase()),
          chunk.cpuAddress(),
          cartridge.getRomOffset() + chunk.fileOffset(),
          chunk.requestedSize(),
          settings.log());
    }

    try {
      MemoryMap.createBlockSystemRegion(program);
      MemoryMap.createBlockWram(program);
      MemoryMap.createBlockHighHalfRomMirrors(program, romType, cartridge.getRomHeader(), 0x00);
      Vectors.createVectorLabels(program);

      SnesOptions.initializeFromCartridge(program, cartridge);
    } catch (Exception e) {
      settings
          .log()
          .appendMsg("Could not add SNES helper mappings/labels (" + e.getMessage() + ")");
    }
  }

  private void mapChunkToCanonicalSpace(
      Program program,
      AddressSpace space,
      ByteProvider provider,
      FileBytes romFileBytes,
      String blockName,
      long cpuAddress,
      long fileOffset,
      long requestedSize,
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
