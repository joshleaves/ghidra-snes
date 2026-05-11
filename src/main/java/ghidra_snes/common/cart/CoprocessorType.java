/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.cart;

/**
 * SNES cartridge coprocessor types decoded from ROM header byte {@code $FFD6}.
 *
 * <p>The high nibble identifies the coprocessor family, while the low nibble
 * describes the cartridge memory configuration:
 *
 * <ul>
 *   <li>{@code x0-x2}: no coprocessor
 *   <li>{@code x3-x6}: coprocessor variants with optional RAM/battery
 * </ul>
 *
 * <p>When the coprocessor family nibble is {@code 0xF}, the custom chip type is
 * further specified by header byte {@code $FFBF}.
 *
 * <p>Source: <a href="https://snes.nesdev.org/wiki/ROM_header">SnesDev: ROM header</a>
 */
public enum CoprocessorType {
  /** No coprocessor present. */
  NONE,

  /** DSP series coprocessors (DSP-1, DSP-2, DSP-3, DSP-4). */
  DSP,

  /** Graphics Support Unit (aka Super FX). */
  GSU,

  /** Object Controller chip (only used by Metal Combat). */
  OBC1,

  /** Nintendo SA-1 enhancement chip. */
  SA1,

  /** S-DD1 decompression chip. */
  S_DD1,

  /** Sharp real-time clock chip. */
  S_RTC,

  /** Other or undocumented enhancement hardware. */
  OTHER,

  // Custom chip types identified through $FFBF.
  /** SPC7110 data decompression chip. */
  SPC7110,

  /** ST010/ST011 coprocessor. */
  ST010,

  /** ST018 coprocessor. */
  ST018,

  /** Capcom CX4 math coprocessor. */
  CX4,

  /** Unsupported or unrecognized coprocessor type. */
  UNKNOWN;

  /**
   * Decodes a SNES cartridge coprocessor type from ROM header bytes.
   *
   * @param value raw {@code $FFD6} cartridge hardware byte
   * @param customByte raw {@code $FFBF} custom chip byte
   * @return decoded coprocessor type, or {@link #UNKNOWN} if unsupported
   */
  public static CoprocessorType fromByte(int value, int customByte) {
    if (value < 0x00 || value > 0xff) {
      return UNKNOWN;
    }
    // Header mapping / cartridge configuration bits.
    int lowNibble = value & 0x0f;
    // Coprocessor family bits.
    int highNibble = (value >> 4) & 0x0f;

    return switch (value) {
      case 0x00, 0x01, 0x02 -> NONE;
      default -> switch (lowNibble) {
        case 0x03, 0x04, 0x05, 0x06 -> switch (highNibble) {
          case 0x0 -> NONE;
          case 0x1 -> GSU;
          case 0x2 -> OBC1;
          case 0x3 -> SA1;
          case 0x4 -> S_DD1;
          case 0x5 -> S_RTC;
          case 0xe -> OTHER;
          // Custom coprocessors are further identified through $FFBF.
          case 0xf -> switch(customByte) {
            case 0x00 -> SPC7110;
            case 0x01 -> ST010;
            case 0x02 -> ST018;
            case 0x03 -> CX4;
            default -> UNKNOWN;
          };
          default -> UNKNOWN;
        };
        default -> UNKNOWN;
      };
    };
  }

  /** Convenience overload accepting signed Java bytes. */
  public static CoprocessorType fromByte(byte value, byte customByte) {
    return fromByte(Byte.toUnsignedInt(value), Byte.toUnsignedInt(customByte));
  }
}
