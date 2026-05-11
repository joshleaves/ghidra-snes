/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.cart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CoprocessorTypeTest {
  @Order(1)
  @DisplayName("Header bytes without a coprocessor type map to NONE")
  @Test
  void headerBytesWithoutCoprocessorTypeMapToNone() {
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x00, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x01, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x02, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x03, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x04, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x05, 0x00));
    assertEquals(CoprocessorType.NONE, CoprocessorType.fromByte(0x06, 0x00));
  }

  @Order(2)
  @DisplayName("Maps known coprocessor type ranges")
  @Test
  void mapsKnownCoprocessorTypeRanges() {
    int[] validLowNibbles = {0x03, 0x04, 0x05, 0x06};
    for (int low: validLowNibbles) {
      assertEquals(CoprocessorType.GSU,     CoprocessorType.fromByte(0x10 | low, 0x00));
      assertEquals(CoprocessorType.OBC1,    CoprocessorType.fromByte(0x20 | low, 0x00));
      assertEquals(CoprocessorType.SA1,     CoprocessorType.fromByte(0x30 | low, 0x00));
      assertEquals(CoprocessorType.S_DD1,   CoprocessorType.fromByte(0x40 | low, 0x00));
      assertEquals(CoprocessorType.S_RTC,   CoprocessorType.fromByte(0x50 | low, 0x00));
      assertEquals(CoprocessorType.OTHER,   CoprocessorType.fromByte(0xe0 | low, 0x00));
      assertEquals(CoprocessorType.SPC7110, CoprocessorType.fromByte(0xf0 | low, 0x00));
      assertEquals(CoprocessorType.ST010,   CoprocessorType.fromByte(0xf0 | low, 0x01));
      assertEquals(CoprocessorType.ST018,   CoprocessorType.fromByte(0xf0 | low, 0x02));
      assertEquals(CoprocessorType.CX4,     CoprocessorType.fromByte(0xf0 | low, 0x03));
    }
  }

  @Order(3)
  @DisplayName("Unknown coprocessor type ranges map to UNKNOWN")
  @Test
  void unknownCoprocessorTypeRangesMapToUnknown() {
    int[] unknownHighNibbles = {0x60, 0x70, 0x80, 0x90, 0xa0, 0xb0, 0xc0, 0xd0};

    for (int high : unknownHighNibbles) {
      assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(high | 0x03, 0x00));
      assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(high | 0x04, 0x00));
      assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(high | 0x05, 0x00));
      assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(high | 0x06, 0x00));
    }
  }

  @Order(4)
  @DisplayName("Header bytes without coprocessor feature bits map to UNKNOWN")
  @Test
  void headerBytesWithoutCoprocessorFeatureBitsMapToUnknown() {
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x07, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x08, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x0f, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x10, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x52, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0xf7, 0x00));
  }

  @Order(5)
  @DisplayName("Out-of-range coprocessor type values map to UNKNOWN")
  @Test
  void outOfRangeValuesMapToUnknown() {
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(-1, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0x100, 0x00));
    assertEquals(CoprocessorType.UNKNOWN, CoprocessorType.fromByte(0xf4, 0x04));
  }

  @Order(6)
  @DisplayName("Signed Java bytes are decoded as unsigned header bytes")
  @Test
  void signedJavaBytesAreDecodedAsUnsignedHeaderBytes() {
    assertEquals(CoprocessorType.CX4, CoprocessorType.fromByte((byte) 0xf6, (byte) 0x03));
  }
}
