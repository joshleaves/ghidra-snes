/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.registers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VectorsTest {
  @Test
  @DisplayName("Vector register table has unique names and addresses")
  void vectorRegisterTableHasUniqueNamesAndAddresses() {
    var names = new HashSet<String>();
    var addresses = new HashSet<Long>();

    for (Vectors.VectorRegister register : Vectors.VECTOR_REGISTERS) {
      assertTrue(names.add(register.name()), "duplicate vector name: " + register.name());
      assertTrue(
          addresses.add(register.address()), "duplicate vector address: " + register.address());
    }
  }

  @Test
  @DisplayName("Vector register table includes reset and interrupt vectors")
  void vectorRegisterTableIncludesResetAndInterruptVectors() {
    assertEquals(10, Vectors.VECTOR_REGISTERS.size());
    assertTrue(
        Vectors.VECTOR_REGISTERS.contains(
            new Vectors.VectorRegister("VEC_RESET_EMULATION", 0x00fffc)));
    assertTrue(
        Vectors.VECTOR_REGISTERS.contains(new Vectors.VectorRegister("VEC_IRQ_NATIVE", 0x00ffee)));
  }
}
