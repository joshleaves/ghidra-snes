/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.cart;

/**
 * SNES cartridge hardware feature combinations decoded from ROM header byte
 * {@code $FFD6}.
 *
 * <p>The low nibble describes the cartridge memory configuration:
 *
 * <ul>
 *   <li>{@code x0}: ROM only
 *   <li>{@code x1}: ROM + RAM
 *   <li>{@code x2}: ROM + RAM + battery
 *   <li>{@code x3}: ROM + coprocessor
 *   <li>{@code x4}: ROM + coprocessor + RAM
 *   <li>{@code x5}: ROM + coprocessor + RAM + battery
 *   <li>{@code x6}: ROM + coprocessor + battery
 * </ul>
 *
 * <p>The high nibble identifies the coprocessor family when present.
 *
 * <p>Source: <a href="https://snes.nesdev.org/wiki/ROM_header">SnesDev: ROM header</a>
 */
public enum HardwareFeatures {
  /** Cartridge only contains ROM. */
  ROM_ONLY,

  /** Cartridge contains ROM and battery-less save RAM. */
  ROM_RAM,

  /** Cartridge contains ROM, save RAM, and battery backup. */
  ROM_RAM_BATTERY,

  /** Cartridge contains ROM and a coprocessor. */
  ROM_COPRO,

  /** Cartridge contains ROM, a coprocessor, and RAM. */
  ROM_COPRO_RAM,

  /** Cartridge contains ROM, a coprocessor, RAM, and battery backup. */
  ROM_COPRO_RAM_BATTERY,

  /** Cartridge contains ROM, a coprocessor, and battery backup. */
  ROM_COPRO_BATTERY,

  /** Unsupported or unrecognized hardware configuration. */
  UNKNOWN;

  /**
   * Decodes cartridge hardware features from ROM header byte {@code $FFD6}.
   *
   * @param value raw cartridge hardware byte
   * @return decoded hardware feature set, or {@link #UNKNOWN} if unsupported
   */
  public static HardwareFeatures fromByte(int value) {
    if (value < 0x00 || value > 0xff) {
      return UNKNOWN;
    }

    // Coprocessor family bits.
    int highNibble = (value >> 4) & 0x0f;

    // Mapping mode and cartridge configuration bits.
    int lowNibble = value & 0x0f;

    return switch (value) {
      case 0x00 -> ROM_ONLY;
      case 0x01 -> ROM_RAM;
      case 0x02 -> ROM_RAM_BATTERY;
      default -> switch (highNibble) {
        case 0x0, 0x1, 0x2, 0x3, 0x4, 0x5, 0xE, 0xF -> switch (lowNibble) {
          case 0x03 -> ROM_COPRO;
          case 0x04 -> ROM_COPRO_RAM;
          case 0x05 -> ROM_COPRO_RAM_BATTERY;
          case 0x06 -> ROM_COPRO_BATTERY;
          default -> UNKNOWN;
        };
        default -> UNKNOWN;
      };
    };
  }

  /** Convenience overload accepting a signed Java byte. */
  public static HardwareFeatures fromByte(byte value) {
    return fromByte(Byte.toUnsignedInt(value));
  }

  /** Returns whether the cartridge includes battery-backed persistent storage. */
  public boolean hasBattery() {
    return switch (this) {
      case ROM_RAM_BATTERY, ROM_COPRO_RAM_BATTERY, ROM_COPRO_BATTERY -> true;
      default -> false;
    };
  }

  /** Returns whether the cartridge includes additional RAM. */
  public boolean hasRam() {
    return switch (this) {
      case ROM_RAM, ROM_RAM_BATTERY, ROM_COPRO_RAM, ROM_COPRO_RAM_BATTERY -> true;
      default -> false;
    };
  }

  /** Returns whether the cartridge includes an enhancement coprocessor. */
  public boolean hasCoprocessor() {
    return switch (this) {
      case ROM_COPRO, ROM_COPRO_BATTERY, ROM_COPRO_RAM, ROM_COPRO_RAM_BATTERY -> true;
      default -> false;
    };
  }
}
