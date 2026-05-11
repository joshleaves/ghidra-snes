/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.testing;

import ghidra_snes.common.SnesRomHeader;

/** Factories for lightweight {@link SnesRomHeader} records used by mapping unit tests. */
public final class SnesRomHeaderFactory {
  private SnesRomHeaderFactory() {}

  /**
   * Builds an empty header record at a parsed-header location.
   *
   * <p>This location is the byte consumed by {@code SnesRomHeader.location()}, not the public SNES
   * title offset. Use this only when a test depends on header placement but not on header contents.
   */
  public static SnesRomHeader emptyHeaderAt(long location) {
    return new SnesRomHeader(location, new byte[Math.toIntExact(SnesRomHeader.HEADER_SIZE)]);
  }
}
