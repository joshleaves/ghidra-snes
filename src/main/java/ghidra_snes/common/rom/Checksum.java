/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.rom;

import ghidra.app.util.bin.ByteProvider;
import java.io.IOException;
import ghidra_snes.common.RomMapType;
import java.util.Arrays;

/**
 * Computes canonical SNES ROM checksums.
 *
 * <p>SNES cartridges expose ROMs in a mirrored power-of-two memory map. ROM dumps, however,
 * frequently omit duplicated regions and therefore do not always match the physical cartridge
 * layout byte-for-byte.
 *
 * <p>When a ROM size is not already a power of two, the checksum algorithm mirrors the trailing
 * remainder region until it fills the next power-of-two boundary, matching the behavior described
 * by the SNES ROM header specification.
 *
 * <p>Example, A 3 MiB ROM is treated as:
 *
 * <ul>
 *   <li>2 MiB base region
 *   <li>1 MiB remainder region
 *   <li>The 1 MiB remainder is mirrored once to fill the missing upper 1 MiB
 * </ul>
 *
 * <p>If the trailing remainder is not itself a power of two, it is conceptually padded with
 * {@code 0x00} bytes until it reaches the next power-of-two size before being mirrored. Clean retail
 * dumps usually do not need this padding; it mainly matters for unusual or space-saving homebrew
 * dumps.
 *
 * <p>Sources:
 *
 * <ul>
 *   <li><a href="https://snes.nesdev.org/wiki/ROM_header#Checksum">SNESDev: Rom header</a></li>
 *   <li><a href="https://github.com/Optiroc/SuperFamicheck">GitHub: SuperFamicheck</a> for SPC7110
 *       checksum behavior observations and implementation details</li>
 * </ul>
 */
public final class Checksum {
  private Checksum() {}

  /**
   * Computes the canonical SNES checksum for a ROM image using decoded header metadata.
   *
   * <p>For standard mappings, the ROM is expanded up to the size declared by the internal ROM
   * header. For SPC7110 cartridges, the checksum is currently calculated over the raw ROM image
   * without generic power-of-two mirroring, matching known Tengai Makyou Zero behavior.
   *
   * @param provider byte source containing the ROM image
   * @param romOffset offset of the ROM data within the provider (after any copier header)
   * @param romSize physical ROM data size in bytes
   * @param declaredRomSize ROM size decoded from the internal SNES header
   * @param romMapType decoded ROM mapping type
   * @return 16-bit SNES checksum value
   * @throws IOException if ROM bytes cannot be read
   */
  public static int snesChecksum(
      ByteProvider provider,
      long romOffset,
      long romSize,
      long declaredRomSize,
      RomMapType romMapType)
      throws IOException {
    if (romSize <= 0) {
      return 0;
    }

    if (romMapType == RomMapType.SPC7110) {
      return checksumRange(provider, romOffset, romSize);
    }

    long mappedSize = Math.max(romSize, declaredRomSize);
    return checksumMappedRom(provider, romOffset, romSize, mappedSize);
  }

  private static int checksumMappedRom(
      ByteProvider provider, long romOffset, long romSize, long mappedSize) throws IOException {
    byte[] image = provider.readBytes(romOffset, romSize);

    while (image.length < mappedSize) {
      int mirrorStart = Math.toIntExact(mappedSize >> 1);
      if (mirrorStart >= image.length) {
        break;
      }

      int mirrorLength = (int) Math.min(mappedSize - image.length, image.length - mirrorStart);
      int previousLength = image.length;
      image = Arrays.copyOf(image, image.length + mirrorLength);
      System.arraycopy(image, mirrorStart, image, previousLength, mirrorLength);
    }

    return checksumBytes(image);
  }

  private static int checksumBytes(byte[] bytes) {
    int checksum = 0;

    for (byte value : bytes) {
      checksum = (checksum + (value & 0xff)) & 0xffff;
    }

    return checksum;
  }

  /** Computes the additive 16-bit checksum of a contiguous ROM region. */
  private static int checksumRange(ByteProvider provider, long offset, long size) throws IOException {
    int checksum = 0;

    for (long index = 0; index < size; index++) {
      checksum = (checksum + (provider.readByte(offset + index) & 0xff)) & 0xffff;
    }

    return checksum;
  }
}
