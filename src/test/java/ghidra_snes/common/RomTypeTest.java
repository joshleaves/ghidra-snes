/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RomTypeTest {
  @Test
  @DisplayName("LoROM maps first chunk to bank $80")
  void loRomMapsFirstChunkToBank80() {
    var chunks = RomType.LoROM.mappingChunks(0x8000).iterator();

    assertTrue(chunks.hasNext());

    var chunk = chunks.next();

    assertEquals(0x80, chunk.bank());
    assertEquals(0x000000, chunk.fileOffset());
    assertEquals(0x808000, chunk.cpuAddress());
    assertEquals(0x8000, chunk.requestedSize());

    assertFalse(chunks.hasNext());
  }

  @Test
  @DisplayName("LoROM maps sequential banks and keeps the final partial chunk size")
  void loRomMapsSequentialBanksAndPartialFinalChunk() {
    var chunks = chunksFor(RomType.LoROM, 0x8000L + 0x1234L);

    assertEquals(
        List.of(
            new RomType.MappingChunk(0x80, 0x000000, 0x808000, 0x8000),
            new RomType.MappingChunk(0x81, 0x008000, 0x818000, 0x1234)),
        chunks);
  }

  @Test
  @DisplayName("HiROM maps full 64 KiB banks from $C0")
  void hiRomMapsFullBanksFromC0() {
    var chunks = chunksFor(RomType.HiROM, 0x10000L * 2L);

    assertEquals(
        List.of(
            new RomType.MappingChunk(0xc0, 0x000000, 0xc00000, 0x10000),
            new RomType.MappingChunk(0xc1, 0x010000, 0xc10000, 0x10000)),
        chunks);
  }

  @Test
  @DisplayName("ExHiROM continues from $C0-$FF into $40-$7F")
  void exHiRomContinuesIntoSecondBankRange() {
    var chunks = chunksFor(RomType.ExHiROM, 0x10000L * 65L);

    assertEquals(65, chunks.size());
    assertEquals(new RomType.MappingChunk(0xc0, 0x000000, 0xc00000, 0x10000), chunks.get(0));
    assertEquals(new RomType.MappingChunk(0xff, 0x3f0000, 0xff0000, 0x10000), chunks.get(63));
    assertEquals(new RomType.MappingChunk(0x40, 0x400000, 0x400000, 0x10000), chunks.get(64));
  }

  @Test
  @DisplayName("Mapping stops when ROM data exceeds the addressable bank range")
  void mappingStopsAtConfiguredBankRanges() {
    var chunks = chunksFor(RomType.HiROM, 0x10000L * 80L);

    assertEquals(64, chunks.size());
    assertEquals(new RomType.MappingChunk(0xff, 0x3f0000, 0xff0000, 0x10000), chunks.get(63));
  }

  @Test
  @DisplayName("Zero and negative ROM sizes do not produce mapping chunks")
  void nonPositiveRomSizesDoNotProduceChunks() {
    assertTrue(chunksFor(RomType.LoROM, 0).isEmpty());
    assertTrue(chunksFor(RomType.LoROM, -1).isEmpty());
  }

  @Test
  @DisplayName("Raw ROMs cannot be mapped")
  void rawRomCannotBeMapped() {
    assertThrows(IllegalStateException.class, () -> RomType.Raw.mappingChunks(0x8000));
  }

  @Test
  @DisplayName("Iterator throws after the last chunk")
  void iteratorThrowsAfterLastChunk() {
    var chunks = RomType.LoROM.mappingChunks(0x8000).iterator();

    assertTrue(chunks.hasNext());
    chunks.next();
    assertFalse(chunks.hasNext());
    assertThrows(NoSuchElementException.class, chunks::next);
  }

  private static List<RomType.MappingChunk> chunksFor(RomType romType, long romSize) {
    return StreamSupport.stream(romType.mappingChunks(romSize).spliterator(), false).toList();
  }
}
