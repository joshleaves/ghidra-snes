/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnesRomHeaderTest {
  @Test
  @DisplayName("Zero SRAM size byte means no cartridge RAM")
  void zeroSramSizeByteMeansNoCartridgeRam() {
    SnesRomHeader header = headerWithRamSizeByte(0x00);

    assertEquals(0, header.ramSize());
  }

  @Test
  @DisplayName("Non-zero SRAM size byte decodes as a KiB power of two")
  void nonZeroSramSizeByteDecodesAsKiBPowerOfTwo() {
    assertEquals(8 * 1024, headerWithRamSizeByte(0x03).ramSize());
    assertEquals(32 * 1024, headerWithRamSizeByte(0x05).ramSize());
  }

  private static SnesRomHeader headerWithRamSizeByte(int ramSizeByte) {
    byte[] bytes = new byte[Math.toIntExact(SnesRomHeader.HEADER_SIZE)];
    bytes[SnesRomHeader.OFFSET_RAM_SIZE - SnesRomHeader.OFFSET_CUSTOM_CHIP] = (byte) ramSizeByte;
    return new SnesRomHeader(SnesRomHeader.LOROM_HEADER_OFFSET - 1, bytes);
  }
}
