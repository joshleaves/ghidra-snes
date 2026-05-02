/* (C) Arnaud 'red' Rouyer 2026 */
package snescommon;

import java.util.List;

public final class SnesVectors {
  private SnesVectors() {}

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
}
