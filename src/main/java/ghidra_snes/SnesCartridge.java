/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import ghidra.app.util.bin.ByteProvider;
import ghidra_snes.common.RomMapType;
import ghidra_snes.common.SnesRomHeader;
import ghidra_snes.common.SnesRomHeaderDetector;
import java.io.IOException;

/**
 * Immutable-ish SNES cartridge metadata extracted from a ROM ByteProvider.
 *
 * <p>This class is responsible for identifying/describing the cartridge only. It intentionally does
 * not create Ghidra memory blocks. Mapping decisions belong in the loader and mapping helpers.
 */
public final class SnesCartridge {
  public static final int BANK_SIZE = 0x8000;
  public static final long COPIER_HEADER_SIZE = 0x200L;
  public static final long LOROM_HEADER_OFFSET = 0x7fc0L;
  public static final long HIROM_HEADER_OFFSET = 0xffc0L;
  public static final long EXHIROM_HEADER_OFFSET = 0x40ffc0L;

  public enum MetadataSource {
    BML,
    INTERNAL_HEADER,
    HEURISTIC,
    UNKNOWN
  }

  private final ByteProvider provider;
  private final boolean hasCopierHeader;
  private final long romOffset;
  private final long romSize;
  private final RomMapType romMapType;
  private final SnesRomHeader romHeader;
  private final MetadataSource metadataSource;

  public SnesCartridge(ByteProvider provider) throws IOException {
    this.provider = provider;
    this.romHeader = SnesRomHeaderDetector.autoDetectRomHeader(provider);

    this.romOffset = detectRomOffset(this.provider.length());
    this.romSize = this.provider.length() - this.romOffset;
    this.hasCopierHeader = romOffset != 0;
    this.romMapType = this.romHeader.romMapType();
    this.metadataSource = MetadataSource.INTERNAL_HEADER;
  }

  /**
   * Returns the supported ROM payload offset for a loaded file size.
   *
   * <p>Clean SNES dumps are bank-aligned. Dumps with copier/SMC headers have exactly one extra
   * {@code 0x200}-byte prefix. Other remainders are intentionally rejected because the loader cannot
   * safely distinguish them from truncated or otherwise malformed files.
   *
   * @param fileSize full provider size in bytes
   * @return {@code 0} for clean ROMs or {@code 0x200} for copier-headered ROMs
   * @throws IllegalStateException if the file size has an unsupported bank remainder
   */
  public static long detectRomOffset(long fileSize) {
    long romOffset = fileSize % BANK_SIZE;

    if (romOffset == 0 || romOffset == COPIER_HEADER_SIZE) {
      return romOffset;
    }

    throw new IllegalStateException(
        String.format(
            "Unsupported SNES ROM file size 0x%X: expected bank-aligned ROM data with optional"
                + " 0x%X-byte copier header",
            fileSize, COPIER_HEADER_SIZE));
  }

  public ByteProvider getProvider() {
    return provider;
  }

  public boolean hasCopierHeader() {
    return hasCopierHeader;
  }

  public long getRomOffset() {
    return romOffset;
  }

  public RomMapType getRomMapType() {
    return romMapType;
  }

  public SnesRomHeader getRomHeader() {
    return romHeader;
  }

  public long getRomSizeBytes() {
    return romSize;
  }

  public MetadataSource getMetadataSource() {
    return metadataSource;
  }
}
