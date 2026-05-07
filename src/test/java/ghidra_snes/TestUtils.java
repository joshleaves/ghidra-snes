/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class TestUtils {
  private TestUtils() {}

  public static void writeHeader(byte[] rom, long headerOffset, String title, int mapMode) {
    writeHeader(rom, headerOffset, title, mapMode, 0);
  }

  public static void writeHeader(
      byte[] rom, long headerOffset, String title, int mapMode, int sramSizeByte) {
    int offset = Math.toIntExact(headerOffset);
    Arrays.fill(rom, offset, offset + 21, (byte) 0x20);

    byte[] titleBytes = title.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(titleBytes, 0, rom, offset, Math.min(titleBytes.length, 21));

    rom[offset + 0x15] = (byte) mapMode;
    rom[offset + 0x16] = 0x02;
    rom[offset + 0x17] = 0x09;
    rom[offset + 0x18] = (byte) sramSizeByte;
    rom[offset + 0x19] = 0x01;
    rom[offset + 0x1a] = 0x33;
    rom[offset + 0x1b] = 0x00;
    writeU16(rom, offset + 0x1c, 0x1234);
    writeU16(rom, offset + 0x1e, 0xedcb);
    writeU16(rom, offset + 0x3c, 0x8000);
  }

  private static void writeU16(byte[] data, int offset, int value) {
    data[offset] = (byte) value;
    data[offset + 1] = (byte) (value >>> 8);
  }
}
