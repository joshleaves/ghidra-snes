/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ghidra_snes.common.cart.CoprocessorType;
import ghidra_snes.common.cart.HardwareFeatures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CartTypeTest {
  @Test
  @DisplayName("Builds SA-1 cart metadata from header bytes")
  void buildsSa1CartMetadataFromHeaderBytes() {
    CartType cartType = CartType.fromBytes(0x35, 0x00);

    assertEquals(HardwareFeatures.ROM_COPRO_RAM_BATTERY, cartType.hardwareFeatures());
    assertEquals(CoprocessorType.SA1, cartType.coprocessorType());
    assertTrue(cartType.hasRam());
    assertTrue(cartType.hasBattery());
    assertTrue(cartType.hasCoprocessor());
    assertTrue(cartType.isKnown());
  }

  @Test
  @DisplayName("Builds custom CX4 cart metadata from header bytes")
  void buildsCustomCx4CartMetadataFromHeaderBytes() {
    CartType cartType = CartType.fromBytes(0xf3, 0x03);

    assertEquals(HardwareFeatures.ROM_COPRO, cartType.hardwareFeatures());
    assertEquals(CoprocessorType.CX4, cartType.coprocessorType());
    assertFalse(cartType.hasRam());
    assertFalse(cartType.hasBattery());
    assertTrue(cartType.hasCoprocessor());
    assertTrue(cartType.isKnown());
  }

  @Test
  @DisplayName("Unknown header bytes produce unknown cart metadata")
  void unknownHeaderBytesProduceUnknownCartMetadata() {
    CartType cartType = CartType.fromBytes(0x77, 0xff);

    assertEquals(HardwareFeatures.UNKNOWN, cartType.hardwareFeatures());
    assertEquals(CoprocessorType.UNKNOWN, cartType.coprocessorType());
    assertFalse(cartType.hasRam());
    assertFalse(cartType.hasBattery());
    assertFalse(cartType.hasCoprocessor());
    assertFalse(cartType.isKnown());
  }

  @Test
  @DisplayName("Signed Java bytes are decoded as unsigned header bytes")
  void signedJavaBytesAreDecodedAsUnsignedHeaderBytes() {
    CartType cartType = CartType.fromBytes((byte) 0xf5, (byte) 0x03);

    assertEquals(HardwareFeatures.ROM_COPRO_RAM_BATTERY, cartType.hardwareFeatures());
    assertEquals(CoprocessorType.CX4, cartType.coprocessorType());
    assertTrue(cartType.hasRam());
    assertTrue(cartType.hasBattery());
    assertTrue(cartType.hasCoprocessor());
    assertTrue(cartType.isKnown());
  }
}
