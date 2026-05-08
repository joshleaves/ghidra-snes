/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common.registers;

import ghidra.program.model.listing.Program;
import ghidra_snes.ghidra.MemoryMapUtils;
import java.util.List;

public final class Mmio {
  private Mmio() {}

  public record MmioRegister(String name, long address) {}

  /**
   * MMIO Registers
   */
  public static List<MmioRegister> MMIO_REGISTERS = List.of(
      new MmioRegister("INIDISP", 0x2100),
      new MmioRegister("OBSEL", 0x2101),
      new MmioRegister("OAMADDL", 0x2102),
      new MmioRegister("OAMADDH", 0x2103),
      new MmioRegister("OAMDATA", 0x2104),
      new MmioRegister("BGMODE", 0x2105),
      new MmioRegister("MOSAIC", 0x2106),
      new MmioRegister("BG1SC", 0x2107),
      new MmioRegister("BG2SC", 0x2108),
      new MmioRegister("BG3SC", 0x2109),
      new MmioRegister("BG4SC", 0x210a),
      new MmioRegister("BG12NBA", 0x210b),
      new MmioRegister("BG34NBA", 0x210c),
      new MmioRegister("BG1HOFS_M7HOFS", 0x210d),
      new MmioRegister("BG1VOFS_M7VOFS", 0x210e),
      new MmioRegister("VMAIN", 0x2115),
      new MmioRegister("VMADDL", 0x2116),
      new MmioRegister("VMADDH", 0x2117),
      new MmioRegister("VMDATAL", 0x2118),
      new MmioRegister("VMDATAH", 0x2119),
      new MmioRegister("M7SEL", 0x211a),
      new MmioRegister("CGADD", 0x2121),
      new MmioRegister("CGDATA", 0x2122),
      new MmioRegister("W12SEL", 0x2123),
      new MmioRegister("W34SEL", 0x2124),
      new MmioRegister("WOBJSEL", 0x2125),
      new MmioRegister("TM", 0x212c),
      new MmioRegister("TS", 0x212d),
      new MmioRegister("TMW", 0x212e),
      new MmioRegister("TSW", 0x212f),
      new MmioRegister("CGWSEL", 0x2130),
      new MmioRegister("CGADSUB", 0x2131),
      new MmioRegister("COLDATA", 0x2132),
      new MmioRegister("SETINI", 0x2133),
      new MmioRegister("APUIO0", 0x2140),
      new MmioRegister("APUIO1", 0x2141),
      new MmioRegister("APUIO2", 0x2142),
      new MmioRegister("APUIO3", 0x2143),
      new MmioRegister("WMDATA", 0x2180),
      new MmioRegister("WMADDL", 0x2181),
      new MmioRegister("WMADDM", 0x2182),
      new MmioRegister("WMADDH", 0x2183),
      new MmioRegister("NMITIMEN", 0x4200),
      new MmioRegister("WRIO", 0x4201),
      new MmioRegister("WRMPYA", 0x4202),
      new MmioRegister("WRMPYB", 0x4203),
      new MmioRegister("WRDIVL", 0x4204),
      new MmioRegister("WRDIVH", 0x4205),
      new MmioRegister("WRDIVB", 0x4206),
      new MmioRegister("HTIMEL", 0x4207),
      new MmioRegister("HTIMEH", 0x4208),
      new MmioRegister("VTIMEL", 0x4209),
      new MmioRegister("VTIMEH", 0x420a),
      new MmioRegister("MDMAEN", 0x420b),
      new MmioRegister("HDMAEN", 0x420c),
      new MmioRegister("MEMSEL", 0x420d),
      new MmioRegister("RDNMI", 0x4210),
      new MmioRegister("TIMEUP", 0x4211),
      new MmioRegister("HVBJOY", 0x4212),
      new MmioRegister("RDIO", 0x4213),
      new MmioRegister("RDDIVL", 0x4214),
      new MmioRegister("RDDIVH", 0x4215),
      new MmioRegister("RDMPYL", 0x4216),
      new MmioRegister("RDMPYH", 0x4217),
      new MmioRegister("JOY1L", 0x4218),
      new MmioRegister("JOY1H", 0x4219),
      new MmioRegister("JOY2L", 0x421a),
      new MmioRegister("JOY2H", 0x421b),
      new MmioRegister("JOY3L", 0x421c),
      new MmioRegister("JOY3H", 0x421d),
      new MmioRegister("JOY4L", 0x421e),
      new MmioRegister("JOY4H", 0x421f),
      new MmioRegister("DMAP0", 0x4300),
      new MmioRegister("BBAD0", 0x4301),
      new MmioRegister("A1T0L", 0x4302),
      new MmioRegister("A1T0H", 0x4303),
      new MmioRegister("A1B0", 0x4304),
      new MmioRegister("DAS0L", 0x4305),
      new MmioRegister("DAS0H", 0x4306),
      new MmioRegister("DASB0", 0x4307),
      new MmioRegister("A2A0L", 0x4308),
      new MmioRegister("A2A0H", 0x4309),
      new MmioRegister("NLTR0", 0x430a),
      new MmioRegister("DMAP1", 0x4310),
      new MmioRegister("BBAD1", 0x4311),
      new MmioRegister("A1T1L", 0x4312),
      new MmioRegister("A1T1H", 0x4313),
      new MmioRegister("A1B1", 0x4314),
      new MmioRegister("DAS1L", 0x4315),
      new MmioRegister("DAS1H", 0x4316),
      new MmioRegister("DASB1", 0x4317),
      new MmioRegister("A2A1L", 0x4318),
      new MmioRegister("A2A1H", 0x4319),
      new MmioRegister("NLTR1", 0x431a),
      new MmioRegister("DMAP2", 0x4320),
      new MmioRegister("BBAD2", 0x4321),
      new MmioRegister("A1T2L", 0x4322),
      new MmioRegister("A1T2H", 0x4323),
      new MmioRegister("A1B2", 0x4324),
      new MmioRegister("DAS2L", 0x4325),
      new MmioRegister("DAS2H", 0x4326),
      new MmioRegister("DASB2", 0x4327),
      new MmioRegister("A2A2L", 0x4328),
      new MmioRegister("A2A2H", 0x4329),
      new MmioRegister("NLTR2", 0x432a),
      new MmioRegister("DMAP3", 0x4330),
      new MmioRegister("BBAD3", 0x4331),
      new MmioRegister("A1T3L", 0x4332),
      new MmioRegister("A1T3H", 0x4333),
      new MmioRegister("A1B3", 0x4334),
      new MmioRegister("DAS3L", 0x4335),
      new MmioRegister("DAS3H", 0x4336),
      new MmioRegister("DASB3", 0x4337),
      new MmioRegister("A2A3L", 0x4338),
      new MmioRegister("A2A3H", 0x4339),
      new MmioRegister("NLTR3", 0x433a),
      new MmioRegister("DMAP4", 0x4340),
      new MmioRegister("BBAD4", 0x4341),
      new MmioRegister("A1T4L", 0x4342),
      new MmioRegister("A1T4H", 0x4343),
      new MmioRegister("A1B4", 0x4344),
      new MmioRegister("DAS4L", 0x4345),
      new MmioRegister("DAS4H", 0x4346),
      new MmioRegister("DASB4", 0x4347),
      new MmioRegister("A2A4L", 0x4348),
      new MmioRegister("A2A4H", 0x4349),
      new MmioRegister("NLTR4", 0x434a),
      new MmioRegister("DMAP5", 0x4350),
      new MmioRegister("BBAD5", 0x4351),
      new MmioRegister("A1T5L", 0x4352),
      new MmioRegister("A1T5H", 0x4353),
      new MmioRegister("A1B5", 0x4354),
      new MmioRegister("DAS5L", 0x4355),
      new MmioRegister("DAS5H", 0x4356),
      new MmioRegister("DASB5", 0x4357),
      new MmioRegister("A2A5L", 0x4358),
      new MmioRegister("A2A5H", 0x4359),
      new MmioRegister("NLTR5", 0x435a),
      new MmioRegister("DMAP6", 0x4360),
      new MmioRegister("BBAD6", 0x4361),
      new MmioRegister("A1T6L", 0x4362),
      new MmioRegister("A1T6H", 0x4363),
      new MmioRegister("A1B6", 0x4364),
      new MmioRegister("DAS6L", 0x4365),
      new MmioRegister("DAS6H", 0x4366),
      new MmioRegister("DASB6", 0x4367),
      new MmioRegister("A2A6L", 0x4368),
      new MmioRegister("A2A6H", 0x4369),
      new MmioRegister("NLTR6", 0x436a),
      new MmioRegister("DMAP7", 0x4370),
      new MmioRegister("BBAD7", 0x4371),
      new MmioRegister("A1T7L", 0x4372),
      new MmioRegister("A1T7H", 0x4373),
      new MmioRegister("A1B7", 0x4374),
      new MmioRegister("DAS7L", 0x4375),
      new MmioRegister("DAS7H", 0x4376),
      new MmioRegister("DASB7", 0x4377),
      new MmioRegister("A2A7L", 0x4378),
      new MmioRegister("A2A7H", 0x4379),
      new MmioRegister("NLTR7", 0x437a)
    );

  /**
   * Creates labels for SNES MMIO registers.
   *
   * Labels are only created if they do not already exist. The register
   * definitions are sourced from {@link ghidra_snes.common.registers.Mmio#MMIO_REGISTERS}.
   *
   * @param program the current program
   * @param bank system bank where labels should be created
   * @return number of labels created
   */
  public static int createMmioLabels(Program program, int bank) throws Exception {
    if (!MemoryMapUtils.isSystemBank(bank)) {
      return 0;
    }
    int created = 0;
    for (MmioRegister register : Mmio.MMIO_REGISTERS) {
      String labelName =
          bank == 0x00 ? register.name() : "%s_%02X".formatted(register.name(), bank);
      long bankedAddress = ((long) bank << 16) | (register.address() & 0xffff);
      created += MemoryMapUtils.ensureLabel(program, labelName, bankedAddress);
    }

    return created;
  }
}
