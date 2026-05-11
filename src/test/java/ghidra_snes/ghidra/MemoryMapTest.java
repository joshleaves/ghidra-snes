/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ghidra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ghidra_snes.common.RomMapType;
import ghidra_snes.common.SnesRomHeader;
import ghidra_snes.testing.SnesRomHeaderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoryMapTest {
  @Test
  @DisplayName("First LoROM mirror is backed by canonical bank $80")
  void firstLoRomMirrorIsBackedByCanonicalBank80() {
    SnesRomHeader header = SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.LOROM_HEADER_OFFSET - 1);

    assertEquals(0x808000L, MemoryMap.firstBankMirrorStart(RomMapType.LoROM, header));
    assertEquals(0x808000L, MemoryMap.canonicalRomMirrorStart(RomMapType.LoROM, header, 0x00));
  }

  @Test
  @DisplayName("First HiROM mirror is backed by canonical bank $C0 high half")
  void firstHiRomMirrorIsBackedByCanonicalBankC0HighHalf() {
    SnesRomHeader header = SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.HIROM_HEADER_OFFSET - 1);

    assertEquals(0xc08000L, MemoryMap.firstBankMirrorStart(RomMapType.HiROM, header));
    assertEquals(0xc08000L, MemoryMap.canonicalRomMirrorStart(RomMapType.HiROM, header, 0x00));
  }

  @Test
  @DisplayName("First ExHiROM mirror is backed by canonical bank $40 high half")
  void firstExHiRomMirrorIsBackedByCanonicalBank40HighHalf() {
    SnesRomHeader header =
        SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.EXHIROM_HEADER_OFFSET - 1);

    assertEquals(0x408000L, MemoryMap.firstBankMirrorStart(RomMapType.ExHiROM, header));
    assertEquals(0x408000L, MemoryMap.canonicalRomMirrorStart(RomMapType.ExHiROM, header, 0x00));
  }

  @Test
  @DisplayName("Non-zero LoROM mirror banks use canonical LoROM addresses")
  void nonZeroLoRomMirrorBanksUseCanonicalLoRomAddresses() {
    SnesRomHeader header = SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.LOROM_HEADER_OFFSET - 1);

    assertEquals(0x818000L, MemoryMap.canonicalRomMirrorStart(RomMapType.LoROM, header, 0x01));
    assertEquals(0xfd8000L, MemoryMap.canonicalRomMirrorStart(RomMapType.LoROM, header, 0x7d));
    assertNull(MemoryMap.canonicalRomMirrorStart(RomMapType.LoROM, header, 0x7e));
  }

  @Test
  @DisplayName("Non-zero HiROM-like mirror banks use canonical HiROM high-half addresses")
  void nonZeroHiRomLikeMirrorBanksUseCanonicalHiRomHighHalfAddresses() {
    SnesRomHeader header = SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.LOROM_HEADER_OFFSET - 1);

    assertEquals(0xc18000L, MemoryMap.canonicalRomMirrorStart(RomMapType.HiROM, header, 0x01));
    assertEquals(0xff8000L, MemoryMap.canonicalRomMirrorStart(RomMapType.HiROM, header, 0x3f));
    assertEquals(0xc18000L, MemoryMap.canonicalRomMirrorStart(RomMapType.ExHiROM, header, 0x01));
    assertEquals(0xc18000L, MemoryMap.canonicalRomMirrorStart(RomMapType.SA_1, header, 0x01));
    assertEquals(0xc18000L, MemoryMap.canonicalRomMirrorStart(RomMapType.SPC7110, header, 0x01));
    assertEquals(0xc18000L, MemoryMap.canonicalRomMirrorStart(RomMapType.S_DD1, header, 0x01));
    assertNull(MemoryMap.canonicalRomMirrorStart(RomMapType.HiROM, header, 0x40));
  }

  @Test
  @DisplayName("Unknown ROM mapping type only maps the first bank anchor")
  void unknownRomMappingTypeOnlyMapsFirstBankAnchor() {
    SnesRomHeader header = SnesRomHeaderFactory.emptyHeaderAt(SnesRomHeader.LOROM_HEADER_OFFSET - 1);

    assertEquals(0x808000L, MemoryMap.canonicalRomMirrorStart(RomMapType.UNKNOWN, header, 0x00));
    assertNull(MemoryMap.canonicalRomMirrorStart(RomMapType.UNKNOWN, header, 0x01));
  }
}
