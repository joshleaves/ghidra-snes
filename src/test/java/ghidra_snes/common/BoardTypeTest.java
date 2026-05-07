/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardTypeTest {
  @Test
  @DisplayName("Missing board strings resolve to UNKNOWN")
  void missingBoardStringsResolveToUnknown() {
    assertEquals(BoardType.UNKNOWN, BoardType.fromString(null));
    assertEquals(BoardType.UNKNOWN, BoardType.fromString(""));
    assertEquals(BoardType.UNKNOWN, BoardType.fromString("   "));
  }

  @Test
  @DisplayName("Board parsing accepts printed names and casing variants")
  void boardParsingAcceptsPrintedNamesAndCasingVariants() {
    assertEquals(BoardType.SHVC_1A0N_01, BoardType.fromString("shvc-1a0n-01"));
    assertEquals(BoardType.SHVC_2A3M_01_A, BoardType.fromString("SHVC-2A3M-01#A"));
  }

  @Test
  @DisplayName("Unknown board strings resolve to UNKNOWN")
  void unknownBoardStringsResolveToUnknown() {
    assertEquals(BoardType.UNKNOWN, BoardType.fromString("SHVC-NOT-A-BOARD"));
  }

  @Test
  @DisplayName("Board names are formatted like printed PCB labels")
  void boardNamesAreFormattedLikePrintedPcbLabels() {
    assertEquals("SHVC-1A0N-01", BoardType.SHVC_1A0N_01.toString());
    assertEquals("SHVC-2A3M-01#A", BoardType.SHVC_2A3M_01_A.toString());
  }
}
