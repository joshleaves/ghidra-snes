/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.registers;

import ghidra.program.model.listing.Program;
import ghidra_snes.ghidra.MemoryMapUtils;
import java.util.List;

public final class Vectors {
  private Vectors() {}

  public record VectorRegister(String name, long address) {}

  /**
   * Vector Registers
   */
  public static final List<VectorRegister> VECTOR_REGISTERS =
      List.of(
          new VectorRegister("VEC_IRQ_EMULATION", 0x00FFFE),
          new VectorRegister("VEC_RESET_EMULATION", 0x00FFFC),
          new VectorRegister("VEC_NMI_EMULATION", 0x00FFFA),
          new VectorRegister("VEC_ABORT_EMULATION", 0x00FFF8),
          new VectorRegister("VEC_COP_EMULATION", 0x00FFF4),
          new VectorRegister("VEC_IRQ_NATIVE", 0x00FFEE),
          new VectorRegister("VEC_NMI_NATIVE", 0x00FFEA),
          new VectorRegister("VEC_ABORT_NATIVE", 0x00FFE8),
          new VectorRegister("VEC_BRK_NATIVE", 0x00FFE6),
          new VectorRegister("VEC_COP_NATIVE", 0x00FFE4));

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
}
