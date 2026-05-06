/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ghidra;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryConflictException;
import ghidra.program.model.symbol.SourceType;
import ghidra.program.model.symbol.SymbolTable;

public class MemoryMapUtils {
  public static boolean isSystemBank(int bank) {
    return (bank >= 0x00 && bank <= 0x3f) || (bank >= 0x80 && bank <= 0xbf);
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
  public static int ensureUninitializedBlock(
      Program program, String name, long start, long size, String source, String comment)
      throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();

    if (memory.getBlock(name) != null) {
      return 0;
    }

    MemoryBlock block = memory.createUninitializedBlock(name, space.getAddress(start), size, false);
    block.setSourceName(source);
    block.setComment(comment);
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
      Program program,
      String name,
      long start,
      Address mappedAddress,
      long size,
      String source,
      String comment)
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
      block.setSourceName(source);
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
