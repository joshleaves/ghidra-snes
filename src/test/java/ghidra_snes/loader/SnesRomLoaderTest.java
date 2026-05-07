package ghidra_snes.loader;

import static org.junit.jupiter.api.Assertions.*;

import ghidra.app.util.bin.ByteArrayProvider;
import ghidra_snes.SnesCartridge;
import ghidra_snes.TestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnesRomLoaderTest {
  @Test
  @DisplayName("Ambiguous LoROM and HiROM headers do not produce a load spec")
  void ambiguousLoRomAndHiRomHeadersDoNotProduceLoadSpec() throws Exception {
    byte[] rom = new byte[0x10000];
    TestUtils.writeHeader(rom, SnesCartridge.LOROM_HEADER_OFFSET, "AMBIGUOUS LOROM", 0x20);
    TestUtils.writeHeader(rom, SnesCartridge.HIROM_HEADER_OFFSET, "AMBIGUOUS HIROM", 0x21);

    var loader = new SnesRomLoader();
    var loadSpecs = loader.findSupportedLoadSpecs(new ByteArrayProvider("ambiguous.sfc", rom));

    assertTrue(loadSpecs.isEmpty());
  }
}
