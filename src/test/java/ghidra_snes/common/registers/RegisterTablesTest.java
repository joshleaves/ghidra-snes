/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.registers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegisterTablesTest {
  @Test
  @DisplayName("MMIO register table has unique names and addresses")
  void mmioRegisterTableHasUniqueNamesAndAddresses() {
    var names = new HashSet<String>();
    var addresses = new HashSet<Long>();

    for (Mmio.MmioRegister register : Mmio.MMIO_REGISTERS) {
      assertTrue(names.add(register.name()), "duplicate MMIO name: " + register.name());
      assertTrue(
          addresses.add(register.address()), "duplicate MMIO address: " + register.address());
    }
  }

  @Test
  @DisplayName("MMIO register table includes important register boundaries")
  void mmioRegisterTableIncludesImportantRegisterBoundaries() {
    assertEquals(new Mmio.MmioRegister("INIDISP", 0x2100), Mmio.MMIO_REGISTERS.get(0));
    assertTrue(Mmio.MMIO_REGISTERS.contains(new Mmio.MmioRegister("NMITIMEN", 0x4200)));
    assertTrue(Mmio.MMIO_REGISTERS.contains(new Mmio.MmioRegister("MDMAEN", 0x420b)));
    assertEquals(
        new Mmio.MmioRegister("NLTR7", 0x437a),
        Mmio.MMIO_REGISTERS.get(Mmio.MMIO_REGISTERS.size() - 1));
  }

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
