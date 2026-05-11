/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ghidra_snes.common.RomMapType;
import ghidra_snes.common.SnesRomHeader;
import ghidra_snes.testing.ByteProviderFactory;
import ghidra_snes.testing.SnesRomHeaderBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnesCartridgeTest {
  @Test
  @DisplayName("Detects a standard 0x200-byte copier header")
  void detectsStandardCopierHeader() throws Exception {
    byte[] file = new byte[(int) (SnesCartridge.COPIER_HEADER_SIZE + 0x10000)];
    long titleOffset = SnesCartridge.COPIER_HEADER_SIZE + SnesRomHeader.LOROM_HEADER_OFFSET;
    SnesRomHeaderBuilder.writeHeaderAtTitleOffset(file, titleOffset, "SMC HEADER TEST", 0x20);
    SnesRomHeaderBuilder.finalizeChecksumAtTitleOffsetsFromRomOffset(
        file, SnesCartridge.COPIER_HEADER_SIZE, titleOffset);

    SnesCartridge cartridge =
        new SnesCartridge(ByteProviderFactory.namedRom("copier-header.smc", file));

    assertTrue(cartridge.hasCopierHeader());
    assertEquals(SnesCartridge.COPIER_HEADER_SIZE, cartridge.getRomOffset());
    assertEquals(0x10000, cartridge.getRomSizeBytes());
    assertEquals(RomMapType.LoROM, cartridge.getRomMapType());
  }

  @Test
  @DisplayName("Rejects non-standard bank remainders instead of treating them as copier headers")
  void rejectsNonStandardBankRemainders() throws Exception {
    byte[] file = new byte[0x10000 + 0x100];
    long unsupportedOffset = 0x100;
    long titleOffset = unsupportedOffset + SnesRomHeader.LOROM_HEADER_OFFSET;
    SnesRomHeaderBuilder.writeHeaderAtTitleOffset(file, titleOffset, "BAD HEADER TEST", 0x20);
    SnesRomHeaderBuilder.finalizeChecksumAtTitleOffsetsFromRomOffset(
        file, unsupportedOffset, titleOffset);

    assertThrows(
        IllegalStateException.class,
        () -> new SnesCartridge(ByteProviderFactory.namedRom("bad-offset.sfc", file)));
  }
}
