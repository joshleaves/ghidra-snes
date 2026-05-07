package ghidra_snes;

import static org.junit.jupiter.api.Assertions.*;

import ghidra.app.util.bin.ByteArrayProvider;
import ghidra_snes.SnesCartridge.MetadataSource;
import ghidra_snes.common.RomType;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnesCartridgeTest {
  @Test
  @DisplayName("LoROM internal headers populate cartridge metadata")
  void loRomInternalHeadersPopulateMetadata() throws Exception {
    byte[] rom = new byte[0x8000];
    TestUtils.writeHeader(rom, SnesCartridge.LOROM_HEADER_OFFSET, "UNIT TEST ROM", 0x20, 0x03);

    var cartridge = new SnesCartridge(new ByteArrayProvider("unit.sfc", rom));
    var header = cartridge.getRomHeader().orElseThrow();

    assertAll(
        () -> assertFalse(cartridge.hasCopierHeader()),
        () -> assertEquals(0, cartridge.getRomOffset()),
        () -> assertEquals(0x8000, cartridge.getRomSizeBytes()),
        () -> assertEquals(RomType.LoROM, cartridge.getRomType()),
        () -> assertEquals(MetadataSource.INTERNAL_HEADER, cartridge.getMetadataSource()),
        () -> assertEquals("UNIT TEST ROM", cartridge.getRomLabel().orElseThrow()),
        () -> assertEquals(sha256(rom, 0), cartridge.getSha256()),
        () -> assertEquals("UNIT TEST ROM", header.title()),
        () -> assertEquals(0x20, header.mapMode()),
        () -> assertEquals(0x02, header.romTypeByte()),
        () -> assertEquals(0x09, header.romSizeByte()),
        () -> assertEquals(0x03, header.sramSizeByte()),
        () -> assertEquals(8 * 1024, header.getSramSizeBytes()),
        () -> assertEquals(0x1234, header.checksumComplement()),
        () -> assertEquals(0xedcb, header.checksum()),
        () -> assertTrue(header.hasValidChecksumPair()),
        () -> assertEquals(0x8000, header.nativeResetVector()),
        () -> assertEquals(SnesCartridge.LOROM_HEADER_OFFSET, header.fileOffset()));
  }

  @Test
  @DisplayName("HiROM headers are detected at $00FFC0")
  void hiRomHeadersAreDetectedAtFfc0() throws Exception {
    byte[] rom = new byte[0x10000];
    TestUtils.writeHeader(rom, SnesCartridge.HIROM_HEADER_OFFSET, "HIROM TEST", 0x21);

    var cartridge = new SnesCartridge(new ByteArrayProvider("hirom.sfc", rom));

    assertAll(
        () -> assertEquals(RomType.HiROM, cartridge.getRomType()),
        () -> assertTrue(cartridge.getHiRomScore() > cartridge.getLoRomScore()),
        () ->
            assertEquals(
                SnesCartridge.HIROM_HEADER_OFFSET,
                cartridge.getRomHeader().orElseThrow().fileOffset()));
  }

  @Test
  @DisplayName("Ambiguous LoROM and HiROM headers are not guessed")
  void ambiguousLoRomAndHiRomHeadersAreNotGuessed() throws Exception {
    byte[] rom = new byte[0x10000];
    TestUtils.writeHeader(rom, SnesCartridge.LOROM_HEADER_OFFSET, "AMBIGUOUS LOROM", 0x20);
    TestUtils.writeHeader(rom, SnesCartridge.HIROM_HEADER_OFFSET, "AMBIGUOUS HIROM", 0x21);

    var cartridge = new SnesCartridge(new ByteArrayProvider("ambiguous.sfc", rom));

    assertAll(
        () -> assertEquals(RomType.Raw, cartridge.getRomType()),
        () -> assertEquals(MetadataSource.UNKNOWN, cartridge.getMetadataSource()),
        () -> assertEquals(cartridge.getLoRomScore(), cartridge.getHiRomScore()),
        () -> assertTrue(cartridge.getLoRomScore() > 0),
        () -> assertTrue(cartridge.getRomHeader().isEmpty()));
  }

  @Test
  @DisplayName("Copier headers are skipped for size and hashing")
  void copierHeadersAreSkippedForSizeAndHashing() throws Exception {
    byte[] rom = new byte[0x8200];
    Arrays.fill(rom, 0, (int) SnesCartridge.COPIER_HEADER_SIZE, (byte) 0x7e);
    TestUtils.writeHeader(
        rom,
        SnesCartridge.COPIER_HEADER_SIZE + SnesCartridge.LOROM_HEADER_OFFSET,
        "SMC HEADER TEST",
        0x20);

    var cartridge = new SnesCartridge(new ByteArrayProvider("smc.sfc", rom));

    assertAll(
        () -> assertTrue(cartridge.hasCopierHeader()),
        () -> assertEquals(SnesCartridge.COPIER_HEADER_SIZE, cartridge.getRomOffset()),
        () -> assertEquals(0x8000, cartridge.getRomSizeBytes()),
        () -> assertEquals(RomType.LoROM, cartridge.getRomType()),
        () ->
            assertEquals(
                sha256(rom, (int) SnesCartridge.COPIER_HEADER_SIZE), cartridge.getSha256()));
  }

  @Test
  @DisplayName("Unrecognized ROMs keep raw metadata")
  void unrecognizedRomsKeepRawMetadata() throws Exception {
    byte[] rom = new byte[0x1000];

    var cartridge = new SnesCartridge(new ByteArrayProvider("unknown.bin", rom));

    assertAll(
        () -> assertFalse(cartridge.hasCopierHeader()),
        () -> assertEquals(0, cartridge.getRomOffset()),
        () -> assertEquals(rom.length, cartridge.getRomSizeBytes()),
        () -> assertEquals(RomType.Raw, cartridge.getRomType()),
        () -> assertEquals(MetadataSource.UNKNOWN, cartridge.getMetadataSource()),
        () -> assertEquals("unknown.bin", cartridge.getRomLabel().orElseThrow()),
        () -> assertTrue(cartridge.getRomHeader().isEmpty()));
  }

  @Test
  @DisplayName("SRAM byte exponent zero maps to no SRAM")
  void sramByteExponentZeroMapsToNoSram() {
    var header = new SnesCartridge.SnesRomHeader("", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    assertEquals(0, header.getSramSizeBytes());
  }

  private static String sha256(byte[] data, int offset) throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    digest.update(data, offset, data.length - offset);
    return HexFormat.of().formatHex(digest.digest());
  }
}
