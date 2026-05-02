/* (C) Arnaud 'red' Rouyer 2026 */
package snescommon;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryConflictException;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.SymbolTable;
import snescommon.SnesMmio.*;
import snescommon.SnesVectors.VectorRegister;

public final class SnesCommon {

  public static final String OPTIONS_NAME = "SNES ROM";

  public static final String OPT_CART_MAPPER = "cart.mapper";
  public static final String OPT_CART_TITLE = "cart.title";
  public static final String OPT_CART_SRAM_SIZE = "cart.sram_size";
  public static final String OPT_ROM_HAS_SMC_HEADER = "rom.hasSmcHeader";
  public static final String OPT_MEMORY_DISPLAY_MIRRORS = "memory.display_mirrors";
  public static final String OPT_MEMORY_DISPLAY_MMIO = "memory.display_mmio";
  public static final String OPT_MEMORY_DISPLAY_SRAM = "memory.display_sram";
  public static final String OPT_MEMORY_DISPLAY_WRAM = "memory.display_wram";

  private SnesCommon() {}

  /**
   * Creates SNES MMIO memory blocks if they do not already exist.
   *
   * This method is idempotent: existing blocks are left untouched.
   * The MMIO layout is defined in {@link SnesMmio#MMIO_REGIONS}.
   *
   * @param program the current program
   * @return number of blocks created
   */
  public static int createMemoryBlocksMmio(Program program) throws Exception {
    int created = 0;

    for (MmioRegion mmio : SnesMmio.MMIO_REGIONS) {
      created += ensureUninitializedBlock(program, mmio.name(), mmio.start(), mmio.size());
    }

    return created;
  }

  /**
   * Creates the main SNES WRAM memory block (7E–7F) if missing.
   *
   * The block is created as uninitialized, read/write, non-executable memory.
   *
   * @param program the current program
   * @return 1 if created, 0 if already present
   */
  public static int createMemoryBlockWram(Program program) throws Exception {
    return ensureUninitializedBlock(program, "snes_wram", 0x7e0000, 0x20000);
  }

  /**
   * Creates labels for SNES MMIO registers.
   *
   * Labels are only created if they do not already exist. The register
   * definitions are sourced from {@link SnesMmio#MMIO_REGISTERS}.
   *
   * @param program the current program
   * @return number of labels created
   */
  public static int createMmioLabels(Program program) throws Exception {
    int created = 0;
    for (MmioRegister register : SnesMmio.MMIO_REGISTERS) {
      created += ensureLabel(program, register.name(), register.address());
    }

    return created;
  }

  /**
   * Creates labels for SNES Vector registers.
   *
   * Labels are only created if they do not already exist. The register
   * definitions are sourced from {@link SnesVectors#VECTOR_REGISTERS}.
   *
   * @param program the current program
   * @return number of labels created
   */
  public static int createVectorLabels(Program program) throws Exception {
    int created = 0;
    for (VectorRegister register : SnesVectors.VECTOR_REGISTERS) {
      created += SnesCommon.ensureLabel(program, register.name(), register.address());
    }

    return created;
  }

  /**
   * Ensures an uninitialized memory block exists at the given address.
   *
   * If a block with the same name already exists, no action is taken.
   * The block is created as read/write and non-executable.
   *
   * @param program the current program
   * @param name block name
   * @param start start address (flat 24-bit SNES address)
   * @param size size in bytes
   * @return 1 if created, 0 if already exists
   */
  public static int ensureUninitializedBlock(Program program, String name, long start, long size)
      throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();

    if (memory.getBlock(name) != null) {
      return 0;
    }

    MemoryBlock block = memory.createUninitializedBlock(name, space.getAddress(start), size, false);
    block.setRead(true);
    block.setWrite(true);
    block.setExecute(false);
    return 1;
  }

  /**
   * Ensures a mapped memory block exists at the given address.
   *
   * If a block with the same name already exists, no action is taken.
   * The block is created as read/executable and non-writable.
   *
   * @param program the current program
   * @param name block name
   * @param start start address (flat 24-bit SNES address)
   * @param mappedAddress source memory block address
   * @param size size in bytes
   * @param comment Comment string
   * @return 1 if created, 0 if already exists
   */
  public static int ensureMappedBlock(
      Program program, String name, long start, Address mappedAddress, long size, String comment)
      throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();

    if (memory.getBlock(name) != null) {
      return 0;
    }

    if (size <= 0 || size > Integer.MAX_VALUE) {
      return 0;
    }

    Address startAddress = space.getAddress(start);
    try {
      MemoryBlock block =
          memory.createByteMappedBlock(name, startAddress, mappedAddress, size, false);
      block.setComment(comment);
      block.setRead(true);
      block.setWrite(false);
      block.setExecute(true);

      return 1;
    } catch (MemoryConflictException e) {
      return 0;
    }
  }

  /**
   * Ensures a global label exists at the given address.
   *
   * If a label with the same name already exists at the address, no
   * action is taken.
   *
   * @param program the current program
   * @param name label name
   * @param address address value
   * @return 1 if created, 0 if already exists
   */
  public static int ensureLabel(Program program, String name, long address) throws Exception {
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    SymbolTable symbols = program.getSymbolTable();
    Address addr = space.getAddress(address);

    if (symbols.getGlobalSymbol(name, addr) != null) {
      return 0;
    }

    symbols.createLabel(addr, name, SourceType.IMPORTED);
    return 1;
  }
}
