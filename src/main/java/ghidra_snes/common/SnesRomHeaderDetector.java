/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import ghidra.app.util.bin.ByteProvider;
import ghidra_snes.SnesCartridge;
import ghidra_snes.common.rom.Checksum;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Detects and parses SNES internal ROM headers. */
public final class SnesRomHeaderDetector {
  private static final long MAX_REASONABLE_ROM_SIZE = 0x00c00000L;

  private SnesRomHeaderDetector() {}

  /**
   * Finds the single valid SNES internal ROM header in a byte provider.
   *
   * <p>The detector checks the standard LoROM, HiROM, and ExHiROM header locations, adjusted for a
   * supported copier/SMC header when present. A ROM is accepted only when exactly one candidate
   * matches its checksum/complement, declared ROM size, mapper byte, and printable title.
   *
   * @param provider ROM byte source
   * @return the detected header
   * @throws IOException if ROM bytes cannot be read
   * @throws IllegalStateException if no header or multiple headers are valid
   */
  public static SnesRomHeader autoDetectRomHeader(ByteProvider provider) throws IOException {
    long fileSize = provider.length();
    long romOffset = SnesCartridge.detectRomOffset(fileSize);
    long romSize = fileSize - romOffset;

    long[] headerLocations = {
      romOffset + SnesRomHeader.LOROM_HEADER_OFFSET - 1,
      romOffset + SnesRomHeader.HIROM_HEADER_OFFSET - 1,
      romOffset + SnesRomHeader.EXHIROM_HEADER_OFFSET - 1,
    };

    List<SnesRomHeader> validHeaders = new ArrayList<>();
    for (long location : headerLocations) {
      if (location + SnesRomHeader.HEADER_SIZE > fileSize) {
        continue;
      }

      SnesRomHeader header = new SnesRomHeader(location, provider.readBytes(location, SnesRomHeader.HEADER_SIZE));
      // Check 1: Header checksum + complement
      if (((header.checksum() ^ header.complementChecksum()) & 0xffff) != 0xffff) {
        continue;
      }
      // Check 2: Known RomMapType
      if (header.romMapType() == RomMapType.UNKNOWN) {
        continue;
      }
      // Check 3: Valid ROM size
      if (header.romSize() < romSize) {
        continue;
      }
      // Check 4: No extravagant ROM size (12M)
      if (header.romSize() >= MAX_REASONABLE_ROM_SIZE) {
        continue;
      }
      // Check 5: Printable title
      if (!header.hasPrintableTitle()) {
        continue;
      }
      // Check 6: Checksum verification against file data
      int romChecksum = Checksum.snesChecksum(provider, romOffset, romSize, header.romSize(), header.romMapType());
      if (header.checksum() != romChecksum) {
        continue;
      }
      validHeaders.add(header);
    }

    if (validHeaders.isEmpty()) {
      throw new IllegalStateException("No valid SNES ROM header found");
    }

    if (validHeaders.size() > 1) {
      throw new IllegalStateException("Multiple valid SNES ROM headers found");
    }

    return validHeaders.get(0);
  }
}
