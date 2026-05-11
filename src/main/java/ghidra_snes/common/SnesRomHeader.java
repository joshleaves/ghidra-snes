/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import ghidra_snes.common.cart.CoprocessorType;
import ghidra_snes.common.cart.HardwareFeatures;

/**
 * Parsed SNES internal ROM header.
 *
 * <p>The stored byte array starts at {@code $FFBF}, one byte before the public header title at
 * {@code $FFC0}, so custom-chip metadata can be decoded without a separate read.
 *
 * <p>Source: <a href="https://snes.nesdev.org/wiki/ROM_header#Cartridge_header">SnesDev: ROM
 * header</a>
 */
public record SnesRomHeader(long location, byte[] romHeaderBytes) {
  public static final long LOROM_HEADER_OFFSET = 0x007fc0L;
  public static final long HIROM_HEADER_OFFSET = 0x00ffc0L;
  public static final long EXHIROM_HEADER_OFFSET = 0x40ffc0L;
  public static final long HEADER_SIZE = 0x10000 - 0xffbf;

  public static final int OFFSET_CUSTOM_CHIP  = 0xffbf;
  public static final int OFFSET_CART_TITLE   = 0xffc0;
  public static final int OFFSET_ROM_TYPE     = 0xffd5;
  public static final int OFFSET_CHIPSET      = 0xffd6;
  public static final int OFFSET_ROM_SIZE     = 0xffd7;
  public static final int OFFSET_RAM_SIZE     = 0xffd8;
  public static final int OFFSET_COUNTRY_CODE = 0xffd9;
  public static final int OFFSET_DEV_ID       = 0xffda;
  public static final int OFFSET_ROM_VERSION  = 0xffdb;
  public static final int OFFSET_CHKSM_CMP    = 0xffdc;
  public static final int OFFSET_CHKSM        = 0xffde;

  private int readUnsignedByte(int offset) {
    return romHeaderBytes[offset - OFFSET_CUSTOM_CHIP] & 0xff;
  }

  private int readUnsignedShort(int offset) {
    int low = readUnsignedByte(offset);
    int high = readUnsignedByte(offset + 1);
    return low | (high << 8);
  }

  public RomMapType romMapType() {
    return switch (readUnsignedByte(OFFSET_ROM_TYPE) & 0x0f) {
      case 0x0 -> RomMapType.LoROM;
      case 0x1 -> RomMapType.HiROM;
      case 0x2 -> RomMapType.S_DD1;
      case 0x3 -> RomMapType.SA_1;
      case 0x5 -> RomMapType.ExHiROM;
      case 0xA -> RomMapType.SPC7110;
      default -> RomMapType.UNKNOWN;
    };
  }

  public byte[] titleBytes() {
    return Arrays.copyOfRange(
      romHeaderBytes,
      OFFSET_CART_TITLE - OFFSET_CUSTOM_CHIP,
      OFFSET_ROM_TYPE - OFFSET_CUSTOM_CHIP
    );
  }

  public String titleString() {
    byte[] titleBytes = titleBytes();
    return new String(titleBytes, StandardCharsets.US_ASCII).trim();
  }

  /** Returns true when the 21-byte title field contains printable ASCII or spaces only. */
  public boolean hasPrintableTitle() {
    for (byte b : titleBytes()) {
      int value = b & 0xff;
      if (value < 0x20 || value > 0x7e) {
        return false;
      }
    }
    return true;
  }

  public int checksum() {
    return readUnsignedShort(OFFSET_CHKSM);
  }

  public int complementChecksum() {
    return readUnsignedShort(OFFSET_CHKSM_CMP);
  }

  public long romSize() {
    return 1024L << readUnsignedByte(OFFSET_ROM_SIZE);
  }

  /**
   * Returns the declared cartridge RAM size in bytes.
   *
   * <p>Unlike ROM size, a zero SRAM-size byte means no cartridge RAM.
   */
  public long ramSize() {
    int ramSizeByte = readUnsignedByte(OFFSET_RAM_SIZE);
    if (ramSizeByte == 0) {
      return 0;
    }
    return 1024L << ramSizeByte;
  }

  public CartType cartType() {
    int hardwareByte = readUnsignedByte(OFFSET_CHIPSET);
    int customChipByte = readUnsignedByte(OFFSET_CUSTOM_CHIP);
    return CartType.fromBytes(hardwareByte, customChipByte);
  }

  public HardwareFeatures hardwareFeatures() {
    return cartType().hardwareFeatures();
  }

  public CoprocessorType coprocessorType() {
    return cartType().coprocessorType();
  }
}
