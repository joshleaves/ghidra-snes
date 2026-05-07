/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import ghidra.app.util.bin.ByteProvider;
import ghidra_snes.common.BoardType;
import ghidra_snes.common.ChipType;
import ghidra_snes.common.RomType;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Immutable-ish SNES cartridge metadata extracted from a ROM ByteProvider.
 *
 * <p>This class is responsible for identifying/describing the cartridge only. It intentionally does
 * not create Ghidra memory blocks. Mapping decisions belong in the loader and mapping helpers.
 */
public final class SnesCartridge {
  public static final long COPIER_HEADER_SIZE = 0x200L;
  public static final long LOROM_HEADER_OFFSET = 0x7fc0L;
  public static final long HIROM_HEADER_OFFSET = 0xffc0L;
  public static final long EXHIROM_HEADER_OFFSET = 0x40ffc0L;

  private static final int MIN_CONFIDENCE_SCORE = 16;

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
  private final String sha256;
  private final RomType romType;
  private final String romLabel;
  private final Optional<SnesRomHeader> romHeader;
  private final BoardType boardType;
  private final List<ChipType> chips;
  private final MetadataSource metadataSource;
  private final int loRomScore;
  private final int hiRomScore;
  private final int exHiRomScore;

  public SnesCartridge(ByteProvider provider) throws IOException {
    this.provider = provider;

    DetectionResult detection = detectBestMapping(provider);
    this.hasCopierHeader = detection.romOffset == COPIER_HEADER_SIZE;
    this.romOffset = detection.romOffset;
    this.romSize = Math.max(0, provider.length() - detection.romOffset);
    this.sha256 = computeSha256(provider, detection.romOffset);

    SnesRomHeader detectedHeader =
        detection.hasConfidentMapping() ? readHeader(provider, detection.headerOffset()) : null;
    this.romHeader = Optional.ofNullable(detectedHeader);

    if (detection.bestScore() >= MIN_CONFIDENCE_SCORE && detection.mode != DetectionMode.UNKNOWN) {
      this.romType = detection.toRomType();
      this.romLabel = titleOrProviderName(detectedHeader, provider);
      this.boardType = BoardType.UNKNOWN;
      this.chips = List.of();
      this.metadataSource = MetadataSource.INTERNAL_HEADER;
    } else {
      this.romType = RomType.Raw;
      this.romLabel = provider.getName();
      this.boardType = BoardType.UNKNOWN;
      this.chips = List.of();
      this.metadataSource = MetadataSource.UNKNOWN;
    }

    this.loRomScore = detection.loScore;
    this.hiRomScore = detection.hiScore;
    this.exHiRomScore = detection.exHiScore;
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

  public long getRomSizeBytes() {
    return romSize;
  }

  public double getRomSizeMegabytes() {
    return romSize / (1024.0 * 1024.0);
  }

  public String getSha256() {
    return sha256;
  }

  public RomType getRomType() {
    return romType;
  }

  public Optional<String> getRomLabel() {
    if (romLabel == null || romLabel.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(romLabel);
  }

  public Optional<SnesRomHeader> getRomHeader() {
    return romHeader;
  }

  public BoardType getBoardType() {
    return boardType;
  }

  public List<ChipType> getChips() {
    return Collections.unmodifiableList(chips);
  }

  public MetadataSource getMetadataSource() {
    return metadataSource;
  }

  public int getLoRomScore() {
    return loRomScore;
  }

  public int getHiRomScore() {
    return hiRomScore;
  }

  public int getExHiRomScore() {
    return exHiRomScore;
  }

  private static String titleOrProviderName(SnesRomHeader header, ByteProvider provider) {
    if (header != null && !header.title().isBlank()) {
      return header.title();
    }
    return provider.getName();
  }

  private static DetectionResult detectBestMapping(ByteProvider provider) throws IOException {
    List<Long> candidateOffsets = new ArrayList<>();
    candidateOffsets.add(0L);
    if (provider.length() >= COPIER_HEADER_SIZE
        && (provider.length() % 0x8000L) == COPIER_HEADER_SIZE) {
      candidateOffsets.add(COPIER_HEADER_SIZE);
    }

    DetectionResult best = null;
    for (long romOffset : candidateOffsets) {
      int lo = scoreHeader(provider, romOffset + LOROM_HEADER_OFFSET, HeaderKind.LoROM);
      int hi = scoreHeader(provider, romOffset + HIROM_HEADER_OFFSET, HeaderKind.HiROM);
      int exHi = scoreHeader(provider, romOffset + EXHIROM_HEADER_OFFSET, HeaderKind.ExHiROM);

      DetectionMode mode = uniquelyBestDetectionMode(lo, hi, exHi);
      long headerOffset = headerOffsetForMode(romOffset, mode);

      DetectionResult candidate = new DetectionResult(mode, romOffset, headerOffset, lo, hi, exHi);
      if (best == null || candidate.bestScore() > best.bestScore()) {
        best = candidate;
      }
    }

    if (best == null) {
      return new DetectionResult(DetectionMode.UNKNOWN, 0L, LOROM_HEADER_OFFSET, -999, -999, -999);
    }
    return best;
  }

  private static DetectionMode uniquelyBestDetectionMode(int loScore, int hiScore, int exHiScore) {
    int bestScore = Math.max(loScore, Math.max(hiScore, exHiScore));
    int winners = 0;
    winners += loScore == bestScore ? 1 : 0;
    winners += hiScore == bestScore ? 1 : 0;
    winners += exHiScore == bestScore ? 1 : 0;

    if (winners != 1) {
      return DetectionMode.UNKNOWN;
    }
    if (loScore == bestScore) {
      return DetectionMode.LoROM;
    }
    if (hiScore == bestScore) {
      return DetectionMode.HiROM;
    }
    return DetectionMode.ExHiROM;
  }

  private static long headerOffsetForMode(long romOffset, DetectionMode mode) {
    return switch (mode) {
      case LoROM -> romOffset + LOROM_HEADER_OFFSET;
      case HiROM -> romOffset + HIROM_HEADER_OFFSET;
      case ExHiROM -> romOffset + EXHIROM_HEADER_OFFSET;
      case UNKNOWN -> romOffset + LOROM_HEADER_OFFSET;
    };
  }

  private static int scoreHeader(ByteProvider provider, long headerOffset, HeaderKind kind)
      throws IOException {
    if (headerOffset < 0 || (headerOffset + 0x40L) > provider.length()) {
      return -999;
    }

    int score = 0;

    for (int i = 0; i < 21; i++) {
      int b = u8(provider, headerOffset + i);
      if (b >= 0x20 && b <= 0x7e) {
        score++;
      } else {
        score -= 2;
      }
    }

    int mapMode = u8(provider, headerOffset + 0x15);
    int lowNibble = mapMode & 0x0f;
    if (kind == HeaderKind.LoROM && (lowNibble == 0x0 || lowNibble == 0x3)) {
      score += 8;
    }
    if ((kind == HeaderKind.HiROM || kind == HeaderKind.ExHiROM)
        && (lowNibble == 0x1 || lowNibble == 0x5)) {
      score += 8;
    }

    int complement = u16(provider, headerOffset + 0x1c);
    int checksum = u16(provider, headerOffset + 0x1e);
    if (((complement ^ checksum) & 0xffff) == 0xffff) {
      score += 8;
    }

    int resetVector = u16(provider, headerOffset + 0x3c);
    if (resetVector >= 0x8000 && resetVector <= 0xffff) {
      score += 8;
    }
    if (resetVector == 0xffff || resetVector == 0x0000) {
      score -= 8;
    }

    return score;
  }

  private static SnesRomHeader readHeader(ByteProvider provider, long headerOffset)
      throws IOException {
    if (headerOffset < 0 || headerOffset + 0x40L > provider.length()) {
      return null;
    }

    String title = readTitle(provider, headerOffset);
    int mapMode = u8(provider, headerOffset + 0x15);
    int romTypeByte = u8(provider, headerOffset + 0x16);
    int romSizeByte = u8(provider, headerOffset + 0x17);
    int sramSizeByte = u8(provider, headerOffset + 0x18);
    int country = u8(provider, headerOffset + 0x19);
    int licensee = u8(provider, headerOffset + 0x1a);
    int version = u8(provider, headerOffset + 0x1b);
    int checksumComplement = u16(provider, headerOffset + 0x1c);
    int checksum = u16(provider, headerOffset + 0x1e);
    int nativeResetVector = u16(provider, headerOffset + 0x3c);

    return new SnesRomHeader(
        title,
        mapMode,
        romTypeByte,
        romSizeByte,
        sramSizeByte,
        country,
        licensee,
        version,
        checksumComplement,
        checksum,
        nativeResetVector,
        headerOffset);
  }

  private static String readTitle(ByteProvider provider, long headerOffset) throws IOException {
    if (headerOffset < 0 || headerOffset + 21L > provider.length()) {
      return "";
    }

    byte[] titleBytes = provider.readBytes(headerOffset, 21);
    StringBuilder title = new StringBuilder();
    for (byte b : titleBytes) {
      int c = b & 0xff;
      if (c == 0x00) {
        break;
      }
      if (c >= 0x20 && c <= 0x7e) {
        title.append((char) c);
      } else {
        title.append(' ');
      }
    }
    return title.toString().trim();
  }

  private static String computeSha256(ByteProvider provider, long romOffset) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      long remaining = provider.length() - romOffset;
      long offset = romOffset;
      byte[] buffer;

      while (remaining > 0) {
        int chunkSize = (int) Math.min(remaining, 64 * 1024L);
        buffer = provider.readBytes(offset, chunkSize);
        digest.update(buffer);
        offset += chunkSize;
        remaining -= chunkSize;
      }

      return HexFormat.of().formatHex(digest.digest()).toLowerCase(Locale.ROOT);
    } catch (NoSuchAlgorithmException e) {
      throw new IOException("SHA-256 is not available", e);
    }
  }

  private static int u8(ByteProvider provider, long offset) throws IOException {
    return provider.readByte(offset) & 0xff;
  }

  private static int u16(ByteProvider provider, long offset) throws IOException {
    return u8(provider, offset) | (u8(provider, offset + 1) << 8);
  }

  private enum DetectionMode {
    LoROM,
    HiROM,
    ExHiROM,
    UNKNOWN
  }

  private enum HeaderKind {
    LoROM,
    HiROM,
    ExHiROM
  }

  private record DetectionResult(
      DetectionMode mode,
      long romOffset,
      long headerOffset,
      int loScore,
      int hiScore,
      int exHiScore) {
    boolean hasConfidentMapping() {
      return mode != DetectionMode.UNKNOWN && bestScore() >= MIN_CONFIDENCE_SCORE;
    }

    int bestScore() {
      return Math.max(loScore, Math.max(hiScore, exHiScore));
    }

    RomType toRomType() {
      return switch (mode) {
        case LoROM -> RomType.LoROM;
        case HiROM -> RomType.HiROM;
        case ExHiROM -> RomType.ExHiROM;
        case UNKNOWN -> RomType.Raw;
      };
    }
  }

  public record SnesRomHeader(
      String title,
      int mapMode,
      int romTypeByte,
      int romSizeByte,
      int sramSizeByte,
      int country,
      int licensee,
      int version,
      int checksumComplement,
      int checksum,
      int nativeResetVector,
      long fileOffset) {
    public boolean hasValidChecksumPair() {
      return ((checksumComplement ^ checksum) & 0xffff) == 0xffff;
    }

    public int getSramSizeBytes() {
      if (sramSizeByte == 0) {
        return 0;
      }
      return (1 << sramSizeByte) * 1024;
    }
  }
}
