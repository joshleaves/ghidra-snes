/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import ghidra_snes.common.cart.CoprocessorType;
import ghidra_snes.common.cart.HardwareFeatures;

/**
 * Parsed SNES cartridge hardware descriptor.
 *
 * <p>The cartridge header stores two related but distinct pieces of information:
 *
 * <ul>
 *   <li>{@code $FFD6}: broad hardware features, such as RAM, battery, and coprocessor presence.
 *   <li>{@code $FFBF}: custom coprocessor subtype, only meaningful for {@code $Fx} hardware bytes.
 * </ul>
 *
 * <p>{@link CartType} keeps those header-derived concepts together without deciding the canonical
 * ROM mapping by itself. Canonical ROM mapping is still driven by {@link RomMapType}; this descriptor
 * is meant for cartridge-specific behavior such as mirrors, extra hardware windows, and future
 * board quirks.
 */
public record CartType(HardwareFeatures hardwareFeatures, CoprocessorType coprocessorType) {
  public static CartType fromBytes(int hardwareByte, int customByte) {
    HardwareFeatures hardwareFeatures = HardwareFeatures.fromByte(hardwareByte);
    CoprocessorType coprocessorType = CoprocessorType.fromByte(hardwareByte, customByte);

    return new CartType(hardwareFeatures, coprocessorType);
  }

  public static CartType fromBytes(byte hardwareByte, byte customByte) {
    return fromBytes(Byte.toUnsignedInt(hardwareByte), Byte.toUnsignedInt(customByte));
  }

  public boolean hasRam() {
    return hardwareFeatures.hasRam();
  }

  public boolean hasBattery() {
    return hardwareFeatures.hasBattery();
  }

  public boolean hasCoprocessor() {
    return hardwareFeatures.hasCoprocessor();
  }

  public boolean isKnown() {
    return hardwareFeatures != HardwareFeatures.UNKNOWN && coprocessorType != CoprocessorType.UNKNOWN;
  }
}
