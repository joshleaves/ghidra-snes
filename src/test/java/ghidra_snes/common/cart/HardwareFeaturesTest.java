/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HardwareFeaturesTest {
  @Order(1)
  @DisplayName("Maps known hardware features values")
  @Test
  void mapsKnownCustomChipValues() {
    assertEquals(HardwareFeatures.ROM_ONLY,              HardwareFeatures.fromByte(0x00));
    assertEquals(HardwareFeatures.ROM_RAM,               HardwareFeatures.fromByte(0x01));
    assertEquals(HardwareFeatures.ROM_RAM_BATTERY,       HardwareFeatures.fromByte(0x02));
    assertEquals(HardwareFeatures.ROM_COPRO,             HardwareFeatures.fromByte(0x03));
    assertEquals(HardwareFeatures.ROM_COPRO_RAM,         HardwareFeatures.fromByte(0x04));
    assertEquals(HardwareFeatures.ROM_COPRO_RAM_BATTERY, HardwareFeatures.fromByte(0x05));
    assertEquals(HardwareFeatures.ROM_COPRO_BATTERY,     HardwareFeatures.fromByte(0x06));
  }

  @Order(2)
  @Test
  @DisplayName("Known coprocessor hardware ranges decode correctly")
  void knownCoprocessorHardwareRangesDecodeCorrectly() {
    int[] validHighNibbles = {0x00, 0x10, 0x20, 0x30, 0x40, 0x50, 0xE0, 0xF0};

    for (int high : validHighNibbles) {
      assertEquals(HardwareFeatures.ROM_COPRO,             HardwareFeatures.fromByte(high | 0x03));
      assertEquals(HardwareFeatures.ROM_COPRO_RAM,         HardwareFeatures.fromByte(high | 0x04));
      assertEquals(HardwareFeatures.ROM_COPRO_RAM_BATTERY, HardwareFeatures.fromByte(high | 0x05));
      assertEquals(HardwareFeatures.ROM_COPRO_BATTERY,     HardwareFeatures.fromByte(high | 0x06));
    }
  }

  @Order(3)
  @DisplayName("Unknown hardware features values map to UNKNOWN")
  @Test
  void unknownValuesMapToUnknown() {
    IntStream.rangeClosed(0x07, 0xff)
      .filter(value -> {
        int lowNibble = value & 0x0f;
        return lowNibble < 0x03 || lowNibble > 0x06;
      })
      .forEach(value -> assertEquals(HardwareFeatures.UNKNOWN, HardwareFeatures.fromByte(value)));
  }

  @Order(4)
  @DisplayName("Out-of-range hardware feature values map to UNKNOWN")
  @Test
  void outOfRangeValuesMapToUnknown() {
    assertEquals(HardwareFeatures.UNKNOWN, HardwareFeatures.fromByte(-1));
    assertEquals(HardwareFeatures.UNKNOWN, HardwareFeatures.fromByte(0x100));
  }

  @Order(5)
  @DisplayName("Signed Java bytes are decoded as unsigned header bytes")
  @Test
  void signedJavaBytesAreDecodedAsUnsignedHeaderBytes() {
    assertEquals(HardwareFeatures.ROM_COPRO_BATTERY, HardwareFeatures.fromByte((byte) 0xf6));
  }
}
