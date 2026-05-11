/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.loader;

import static org.junit.jupiter.api.Assertions.*;

import ghidra_snes.common.SnesRomHeader;
import ghidra_snes.testing.ByteProviderFactory;
import ghidra_snes.testing.SnesRomHeaderBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnesRomLoaderTest {
  @Test
  @DisplayName("Ambiguous LoROM and HiROM headers do not produce a load spec")
  void ambiguousLoRomAndHiRomHeadersDoNotProduceLoadSpec() throws Exception {
    byte[] rom = new byte[0x10000];
    SnesRomHeaderBuilder.writeHeaderAtTitleOffset(
        rom, SnesRomHeader.LOROM_HEADER_OFFSET, "AMBIGUOUS LOROM", 0x20);
    SnesRomHeaderBuilder.writeHeaderAtTitleOffset(
        rom, SnesRomHeader.HIROM_HEADER_OFFSET, "AMBIGUOUS HIROM", 0x21);
    SnesRomHeaderBuilder.finalizeChecksumAtTitleOffsets(
        rom, SnesRomHeader.LOROM_HEADER_OFFSET, SnesRomHeader.HIROM_HEADER_OFFSET);

    var loader = new SnesRomLoader();
    var loadSpecs = loader.findSupportedLoadSpecs(ByteProviderFactory.namedRom("ambiguous.sfc", rom));

    assertTrue(loadSpecs.isEmpty());
  }

  @Test
  @DisplayName("Unrecognized ROMs do not produce a load spec")
  void unrecognizedRomsDoNotProduceLoadSpec() throws Exception {
    byte[] rom = new byte[0x10000];

    var loader = new SnesRomLoader();
    var loadSpecs = loader.findSupportedLoadSpecs(ByteProviderFactory.namedRom("unknown.bin", rom));

    assertTrue(loadSpecs.isEmpty());
  }
}
