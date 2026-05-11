/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.rom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ghidra_snes.common.RomMapType;
import ghidra_snes.testing.ByteProviderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChecksumTest {
  @Test
  @DisplayName("Empty or negative ROM sizes checksum to zero")
  void emptyOrNegativeRomSizesChecksumToZero() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0x01, 0x02, 0x03);

    assertEquals(0x0000, Checksum.snesChecksum(provider, 0, 0, provider.length(), RomMapType.LoROM));
    assertEquals(0x0000, Checksum.snesChecksum(provider, 0, -1, provider.length(), RomMapType.LoROM));
  }

  @Test
  @DisplayName("Power-of-two ROM sizes checksum the raw ROM bytes")
  void powerOfTwoRomSizesChecksumRawRomBytes() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0x01, 0x02, 0x03, 0x04);

    assertEquals(0x000a, Checksum.snesChecksum(provider, 0, 4, 4, RomMapType.LoROM));
  }

  @Test
  @DisplayName("ROM offsets are honored")
  void romOffsetsAreHonored() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0xff, 0xff, 0x01, 0x02, 0x03, 0x04);

    assertEquals(0x000a, Checksum.snesChecksum(provider, 2, 4, 4, RomMapType.LoROM));
  }

  @Test
  @DisplayName("Non-power-of-two ROM sizes mirror the trailing remainder")
  void nonPowerOfTwoRomSizesMirrorTrailingRemainder() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0x01, 0x02, 0x10);

    // 3 bytes declared as 4 bytes => base bytes plus the trailing byte mirrored once.
    assertEquals(0x0023, Checksum.snesChecksum(provider, 0, 3, 4, RomMapType.LoROM));
  }

  @Test
  @DisplayName("Non-power-of-two remainder is padded to the next power of two before mirroring")
  void nonPowerOfTwoRemainderIsPaddedBeforeMirroring() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0x01, 0x02, 0x03, 0x04, 0x10, 0x20, 0x30);

    // 7 bytes declared as 8 bytes => the upper-half remainder starts at byte 4.
    // The checksum fills the missing byte by mirroring the first byte of that remainder.
    assertEquals(0x007a, Checksum.snesChecksum(provider, 0, 7, 8, RomMapType.LoROM));
  }

  @Test
  @DisplayName("Checksum wraps to 16 bits")
  void checksumWrapsToSixteenBits() throws Exception {
    var provider = ByteProviderFactory.fromUnsignedBytes(0xff, 0xff, 0xff);

    // 3 bytes declared as 4 bytes => trailing byte is mirrored once:
    // 0x01fe + 0x00ff + 0x00ff = 0x03fc.
    assertEquals(0x03fc, Checksum.snesChecksum(provider, 0, 3, 4, RomMapType.LoROM));
  }
}
