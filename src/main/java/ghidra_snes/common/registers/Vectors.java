/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.registers;

import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.ProgramContext;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryAccessException;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.symbol.SourceType;
import ghidra_snes.ghidra.MemoryMapUtils;
import java.math.BigInteger;
import java.util.List;

public final class Vectors {
  private Vectors() {}

  public record VectorRegister(String name, long address) {}

  /**
   * Vector Registers
   */
  public static final List<VectorRegister> VECTOR_REGISTERS = List.of(
      new VectorRegister("VEC_IRQ_EMULATION", 0x00FFFE),
      new VectorRegister("VEC_RESET_EMULATION", 0x00FFFC),
      new VectorRegister("VEC_NMI_EMULATION", 0x00FFFA),
      new VectorRegister("VEC_ABORT_EMULATION", 0x00FFF8),
      new VectorRegister("VEC_COP_EMULATION", 0x00FFF4),
      new VectorRegister("VEC_IRQ_NATIVE", 0x00FFEE),
      new VectorRegister("VEC_NMI_NATIVE", 0x00FFEA),
      new VectorRegister("VEC_ABORT_NATIVE", 0x00FFE8),
      new VectorRegister("VEC_BRK_NATIVE", 0x00FFE6),
      new VectorRegister("VEC_COP_NATIVE", 0x00FFE4)
    );

  /**
   * Creates labels for SNES Vector registers.
   *
   * Labels are only created if they do not already exist. The register
   * definitions are sourced from {@link ghidra_snes.common.registers.Vectors#VECTOR_REGISTERS}.
   *
   * @param program the current program
   * @return number of labels created
   */
  public static int createVectorLabels(Program program) throws Exception {
    int created = 0;
    for (VectorRegister register : Vectors.VECTOR_REGISTERS) {
      created += MemoryMapUtils.ensureLabel(program, register.name(), register.address());
    }

    return created;
  }

  /** A vector whose handler is game code, and whether the CPU is in emulation mode when it runs. */
  private record EntryVector(String vector, String handler, boolean emulation) {}

  // Reset always starts in emulation mode (E=M=X=1). Games run native, so only the native interrupt vectors carry
  // live handlers; their M/X widths are whatever the game left, and the handler's own REP/SEP sets them.
  private static final List<EntryVector> ENTRY_VECTORS = List.of(
      new EntryVector("VEC_RESET_EMULATION", "reset", true),
      new EntryVector("VEC_NMI_NATIVE", "nmi", false),
      new EntryVector("VEC_IRQ_NATIVE", "irq", false),
      new EntryVector("VEC_BRK_NATIVE", "brk", false),
      new EntryVector("VEC_COP_NATIVE", "cop", false));

  /**
   * Creates entry points and functions at the reset and native interrupt handlers, with the processor mode set at
   * each one. Labels alone leave analysis with no entry points, so nothing was disassembled; and without the mode
   * the reset code decodes with 16-bit immediates although the CPU starts with 8-bit registers.
   *
   * Vectors that are empty (0x0000/0xFFFF) or point outside ROM ($8000-$FFFF of bank 00, e.g. a WRAM trampoline)
   * are skipped.
   *
   * @param program the current program
   * @return number of handler functions created
   */
  public static int createVectorEntryPoints(Program program) throws Exception {
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    Memory memory = program.getMemory();
    int created = 0;
    for (EntryVector entry : ENTRY_VECTORS) {
      long vectorAddress = VECTOR_REGISTERS.stream()
          .filter(register -> register.name().equals(entry.vector()))
          .findFirst().orElseThrow().address();
      int target;
      try {
        target = memory.getShort(space.getAddress(vectorAddress)) & 0xFFFF;
      } catch (MemoryAccessException e) {
        continue;
      }
      if (target == 0x0000 || target == 0xFFFF || target < 0x8000) {
        continue;
      }
      Address handler = space.getAddress(target);
      MemoryBlock block = memory.getBlock(handler);
      if (block == null || !block.isExecute()) {
        continue;
      }

      setMode(program.getProgramContext(), handler, entry.emulation());
      program.getSymbolTable().addExternalEntryPoint(handler);
      if (program.getFunctionManager().getFunctionAt(handler) == null) {
        program.getFunctionManager()
            .createFunction(entry.handler(), handler, new AddressSet(handler), SourceType.IMPORTED);
        created++;
      }
    }
    return created;
  }

  private static void setMode(ProgramContext context, Address at, boolean emulation) throws Exception {
    BigInteger flag = emulation ? BigInteger.ONE : BigInteger.ZERO;
    context.setValue(context.getRegister("ctx_EF"), at, at, flag);
    if (emulation) {
      context.setValue(context.getRegister("ctx_MF"), at, at, BigInteger.ONE);
      context.setValue(context.getRegister("ctx_XF"), at, at, BigInteger.ONE);
    }
  }
}
