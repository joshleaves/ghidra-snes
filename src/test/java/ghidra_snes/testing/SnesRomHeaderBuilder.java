/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.testing;

import ghidra.app.util.bin.ByteArrayProvider;
import ghidra_snes.common.RomMapType;
import ghidra_snes.common.rom.Checksum;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Builder for synthetic SNES internal ROM headers.
 *
 * <p>This helper owns the raw byte offsets used by detector/loader unit tests. Keep new synthetic
 * header fixtures here instead of copying {@code $FFD5}, checksum, or reset-vector writes into
 * individual tests.
 *
 * <p>By default, generated headers use plain ROM-only LoROM-compatible metadata. Tests that need
 * enhancement-chip behavior should set those fields explicitly in the test itself instead of making
 * this generic helper pretend to build SPC7110/S-DD1/SA-1 cartridges.
 */
public final class SnesRomHeaderBuilder {
  private SnesRomHeaderBuilder() {}

  private static final int DEFAULT_LOROM_MAP_MODE = 0x20;
  private static final int DEFAULT_HARDWARE_BYTE = 0x00;
  private static final int DEFAULT_ROM_SIZE_BYTE = 0x06;
  private static final int DEFAULT_RAM_SIZE_BYTE = 0x00;
  private static final int DEFAULT_COUNTRY_CODE = 0x00;
  private static final int DEFAULT_DEVELOPER_ID = 0x33;
  private static final int DEFAULT_ROM_VERSION = 0x00;
  private static final int DEFAULT_RESET_VECTOR = 0x8000;

  /** Writes a standard minimal LoROM header for tests that do not care about mapper variants. */
  public static void writeStandardLoRomHeaderAtTitleOffset(byte[] rom, long titleOffset, String title) {
    writeHeaderAtTitleOffset(rom, titleOffset, title, DEFAULT_LOROM_MAP_MODE);
  }

  /**
   * Writes a minimal internal header at the SNES title offset.
   *
   * <p>The {@code titleOffset} is the public SNES header offset such as {@code $7FC0} or
   * {@code $FFC0}; it is not the {@code SnesRomHeader.location()} value, which starts one byte
   * earlier at the optional custom-chip byte. Call {@link #finalizeChecksumAtTitleOffsets} after
   * all synthetic headers have been written when the detector must accept the ROM.
   */
  public static void writeHeaderAtTitleOffset(
      byte[] rom, long titleOffset, String title, int mapMode) {
    writeHeaderAtTitleOffset(rom, titleOffset, title, mapMode, DEFAULT_RAM_SIZE_BYTE);
  }

  /**
   * Writes a minimal internal header at the SNES title offset with an explicit SRAM-size byte.
   *
   * <p>The generated metadata is intentionally sparse: it only covers fields needed by unit tests
   * that exercise header detection and parsing. Tests for richer cartridge metadata should extend
   * this builder rather than adding local byte-patching helpers.
   */
  public static void writeHeaderAtTitleOffset(
      byte[] rom, long titleOffset, String title, int mapMode, int sramSizeByte) {
    int offset = Math.toIntExact(titleOffset);
    byte[] titleBytes = title.getBytes(StandardCharsets.US_ASCII);
    System.arraycopy(titleBytes, 0, rom, offset, Math.min(titleBytes.length, 21));
    Arrays.fill(rom, offset + Math.min(titleBytes.length, 21), offset + 21, (byte) 0x20);

    rom[offset + 0x15] = (byte) mapMode;
    rom[offset + 0x16] = (byte) DEFAULT_HARDWARE_BYTE;
    rom[offset + 0x17] = (byte) DEFAULT_ROM_SIZE_BYTE;
    rom[offset + 0x18] = (byte) sramSizeByte;
    rom[offset + 0x19] = (byte) DEFAULT_COUNTRY_CODE;
    rom[offset + 0x1a] = (byte) DEFAULT_DEVELOPER_ID;
    rom[offset + 0x1b] = (byte) DEFAULT_ROM_VERSION;

    writeLittleEndianShort(rom, offset + 0x3c, DEFAULT_RESET_VECTOR);
  }

  /**
   * Writes matching checksum and complement values for one or more synthetic headers.
   *
   * <p>Pass every header title offset present in the same ROM image. The method first resets all
   * checksum fields to a neutral placeholder so ambiguous-header tests can make multiple headers
   * equally valid.
   */
  public static void finalizeChecksumAtTitleOffsets(byte[] rom, long... titleOffsets)
      throws IOException {
    finalizeChecksumAtTitleOffsetsFromRomOffset(rom, 0, titleOffsets);
  }

  /**
   * Writes matching checksum and complement values for a ROM image with a file prefix.
   *
   * <p>Use this for synthetic SMC/copier-headered files. The {@code titleOffsets} are still
   * absolute file offsets, while {@code romOffset} marks where checksum-covered ROM payload starts.
   */
  public static void finalizeChecksumAtTitleOffsetsFromRomOffset(
      byte[] rom, long romOffset, long... titleOffsets) throws IOException {
    for (long titleOffset : titleOffsets) {
      int offset = Math.toIntExact(titleOffset);
      writeLittleEndianShort(rom, offset + 0x1c, 0xffff);
      writeLittleEndianShort(rom, offset + 0x1e, 0x0000);
    }

    long romSize = rom.length - romOffset;
    int checksum = Checksum.snesChecksum(new ByteArrayProvider(rom), romOffset, romSize, romSize, RomMapType.LoROM);
    int complement = checksum ^ 0xffff;

    for (long titleOffset : titleOffsets) {
      int offset = Math.toIntExact(titleOffset);
      writeLittleEndianShort(rom, offset + 0x1c, complement);
      writeLittleEndianShort(rom, offset + 0x1e, checksum);
    }
  }

  private static void writeLittleEndianShort(byte[] rom, int offset, int value) {
    rom[offset] = (byte) (value & 0xff);
    rom[offset + 1] = (byte) ((value >> 8) & 0xff);
  }
}
