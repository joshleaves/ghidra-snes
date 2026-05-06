package ghidra_snes.ghidra;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoryMapUtilsTest {
  @Test
  @DisplayName("System banks are low banks $00-$3F and mirrors $80-$BF")
  void systemBanksAreLowBanksAndMirrors() {
    assertTrue(MemoryMapUtils.isSystemBank(0x00));
    assertTrue(MemoryMapUtils.isSystemBank(0x3f));
    assertTrue(MemoryMapUtils.isSystemBank(0x80));
    assertTrue(MemoryMapUtils.isSystemBank(0xbf));

    assertFalse(MemoryMapUtils.isSystemBank(-1));
    assertFalse(MemoryMapUtils.isSystemBank(0x40));
    assertFalse(MemoryMapUtils.isSystemBank(0x7f));
    assertFalse(MemoryMapUtils.isSystemBank(0xc0));
    assertFalse(MemoryMapUtils.isSystemBank(0x100));
  }
}
