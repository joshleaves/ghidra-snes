/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ghidra;

import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra_snes.common.RomType;
import ghidra_snes.common.registers.Mmio;
import java.util.List;

public class MemoryMap {
  /**
   * Canonical SNES system region definition.
   *
   * <p>These regions represent the CPU-visible internal SNES areas mirrored
   * across banks 00-3F and 80-BF.
   */
  public record SystemRegion(String bankName, String name, long start, long size) {}

  /**
   * Canonical SNES system regions.
   *
   * <p>The canonical mapping always lives in bank 00. Mirror blocks are later
   * generated into the visible CPU banks.
   */
  public static final List<SystemRegion> SYSTEM_REGIONS = List.of(
      new SystemRegion("snes_low_ram", "SNES Low RAM", 0x0, 0x2000),
      new SystemRegion("snes_io", "SNES I/O", 0x2000, 0x4000)
    );

  /**
   * Creates the canonical SNES system regions in bank $00.
   *
   * <p>This creates the base low RAM and I/O blocks used by all mirrored system
   * views.
   *
   * @param program the current program
   * @return number of blocks created
   */
  public static int createBlockSystemRegion(Program program) throws Exception {
    int created = 0;

    for (SystemRegion memoryRegion : SYSTEM_REGIONS) {
      created +=
          MemoryMapUtils.ensureUninitializedBlock(
              program,
              memoryRegion.bankName(),
              memoryRegion.start(),
              memoryRegion.size(),
              "SNES:Canonical",
              String.format("%s at 0x%06x", memoryRegion.name(), memoryRegion.start()));
    }
    Mmio.createMmioLabels(program, 0x0);

    return created;
  }

  /**
   * Creates mirrored SNES system region blocks across visible CPU banks.
   *
   * <p>The canonical regions remain mapped in bank 00 while all visible banks
   * expose mapped mirrors pointing back to the canonical blocks.
   *
   * @param program the current program
   * @param maxBank highest bank to expose
   * @return number of blocks created
   */
  public static int createBlockSystemRegionMirrors(Program program, int maxBank) throws Exception {
    int created = 0;
    int lastBank = Math.max(0x00, Math.min(maxBank, 0xbf));

    for (int bank = 0x01; bank <= lastBank; bank++) {
      if (!MemoryMapUtils.isSystemBank(bank)) {
        continue;
      }

      for (SystemRegion memoryRegion : SYSTEM_REGIONS) {
        long canonicalStart = memoryRegion.start();
        long bankedStart = ((long) bank << 16) | (canonicalStart & 0xffff);
        Address mappedAddress =
            program.getAddressFactory().getDefaultAddressSpace().getAddress(canonicalStart);

        created +=
            MemoryMapUtils.ensureMappedBlock(
                program,
                "bank_%02x_%s_mirror".formatted(bank, memoryRegion.bankName()),
                bankedStart,
                mappedAddress,
                memoryRegion.size(),
                "SNES:Mirror",
                "Mirror of %s at 0x%06x".formatted(memoryRegion.name(), bankedStart));
      }
      Mmio.createMmioLabels(program, bank);
    }

    return created;
  }

  /**
   * Creates the main SNES WRAM memory block ($7E–$7F) if missing.
   *
   * The block is created as uninitialized, read/write, non-executable memory.
   *
   * @param program the current program
   * @return 1 if created, 0 if already present
   */
  public static int createBlockWram(Program program) throws Exception {
    return MemoryMapUtils.ensureUninitializedBlock(
        program, "snes_wram", 0x7e0000, 0x20000, "SNES:Canonical", "SNES WRAM at 0x7e0000");
  }

  /**
   * Creates high-half ROM mirror blocks for the detected ROM mapping type.
   *
   * <p>The loader maps ROM into a canonical ROM view. This method exposes
   * additional CPU-visible {@code 8000-FFFF} aliases as mapped blocks, skipping
   * banks that are protected or not part of the high-half ROM mirror for the
   * given mapping type.
   *
   * <p>For LoROM, this maps {@code $00-$7D:8000-FFFF} to
   * {@code $80-$FD:8000-FFFF}. For HiROM, this maps {@code $00-$3F:8000-FFFF} to
   * {@code $C0-$FF:8000-FFFF}. ExHiROM uses the same high-half mirror rule as
   * HiROM for now.
   *
   * @param program the current program
   * @param romType detected ROM mapping type
   * @param maxBank highest mirror bank to expose
   * @return number of blocks created
   */
  public static int createBlockHighHalfRomMirrors(Program program, RomType romType, int maxBank)
      throws Exception {
    int created = 0;
    int lastBank = Math.max(0x00, Math.min(maxBank, 0xff));

    for (int bank = 0x00; bank <= lastBank; bank++) {
      Long canonicalStart = canonicalRomMirrorStart(romType, bank);
      if (canonicalStart == null) {
        continue;
      }

      long mirrorStart = ((long) bank << 16) | 0x8000L;
      Address mappedAddress =
          program.getAddressFactory().getDefaultAddressSpace().getAddress(canonicalStart);

      created +=
          MemoryMapUtils.ensureMappedBlock(
              program,
              "bank_%02x_rom_mirror".formatted(bank),
              mirrorStart,
              mappedAddress,
              0x8000,
              "Mirror",
              "Mirror of ROM at 0x%06x".formatted(canonicalStart));
    }

    return created;
  }

  /**
   * Resolves the canonical ROM source address for a high-half mirror bank.
   *
   * <p>This converts a CPU-visible {@code 8000-FFFF} ROM mirror bank into the
   * corresponding canonical high-half ROM address.
   *
   * @param romType detected ROM mapping type
   * @param bank mirror bank index
   * @return canonical ROM address, or null if the bank is not mirrored
   */
  private static Long canonicalRomMirrorStart(RomType romType, int bank) {
    return switch (romType) {
      case LoROM -> {
        if (bank >= 0x00 && bank <= 0x7d) {
          yield 0x800000L + ((long) bank << 16) + 0x8000L;
        }
        yield null;
      }
      case HiROM, ExHiROM -> {
        if (bank >= 0x00 && bank <= 0x3f) {
          yield 0xc00000L + ((long) bank << 16) + 0x8000L;
        }
        yield null;
      }
      case Raw -> null;
    };
  }
}
