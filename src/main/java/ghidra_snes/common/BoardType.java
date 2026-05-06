/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

public enum BoardType {
  UNKNOWN,
  BANDAI_PT_923,
  BSC_1A5B9P_01,
  BSC_1A5M_02,
  BSC_1A7M_01,
  BSC_1A7M_10,
  BSC_1J3M_01,
  BSC_1J5M_01,
  BSC_1L3B_01,
  BSC_1L5B_01,
  EA_1A3M_30,
  EA_1J3M_20,
  MAXI_1A0N_30,
  MAXI_1J0N_20,
  MJSC_1A0N_30,
  MJSC_1J0N_20,
  SGB_R_10,
  SHVC_1A0N_01,
  SHVC_1A0N_02,
  SHVC_1A0N_10,
  SHVC_1A0N_20,
  SHVC_1A0N_30,
  SHVC_1A1B_04,
  SHVC_1A1B_05,
  SHVC_1A1B_06,
  SHVC_1A1M_01,
  SHVC_1A1M_10,
  SHVC_1A1M_11,
  SHVC_1A1M_20,
  SHVC_1A3B_11,
  SHVC_1A3B_12,
  SHVC_1A3B_13,
  SHVC_1A3B_20,
  SHVC_1A3M_10,
  SHVC_1A3M_20,
  SHVC_1A3M_21,
  SHVC_1A3M_30,
  SHVC_1A5B_02,
  SHVC_1A5B_04,
  SHVC_1A5M_01,
  SHVC_1A5M_11,
  SHVC_1A5M_20,
  SHVC_1B0N_02,
  SHVC_1B0N_03,
  SHVC_1B0N_10,
  SHVC_1B5B_02,
  SHVC_1C0N,
  SHVC_1C0N5S_01,
  SHVC_1CA0N5S_01,
  SHVC_1CA0N6S_01,
  SHVC_1CA6B_01,
  SHVC_1CB0N7S_01,
  SHVC_1CB5B_01,
  SHVC_1CB5B_20,
  SHVC_1CB7B_01,
  SHVC_1DC0N_01,
  SHVC_1DS0B_20,
  SHVC_1J0N_01,
  SHVC_1J0N_10,
  SHVC_1J0N_20,
  SHVC_1J1M_11,
  SHVC_1J1M_20,
  SHVC_1J3B_01,
  SHVC_1J3M_01,
  SHVC_1J3M_11,
  SHVC_1J3M_20,
  SHVC_1J5M_01,
  SHVC_1J5M_11,
  SHVC_1J5M_20,
  SHVC_1K0N_01,
  SHVC_1K1B_01,
  SHVC_1K1X_10,
  SHVC_1L3B_02,
  SHVC_1L3B_11,
  SHVC_1L5B_11,
  SHVC_1L5B_20,
  SHVC_1N0N_01,
  SHVC_1N0N_10,
  SHVC_2A0N_01,
  SHVC_2A0N_01_A,
  SHVC_2A0N_10,
  SHVC_2A0N_11,
  SHVC_2A0N_20,
  SHVC_2A1M_01,
  SHVC_2A3B_01,
  SHVC_2A3M_01,
  SHVC_2A3M_01_A,
  SHVC_2A3M_11,
  SHVC_2A3M_20,
  SHVC_2A5M_01,
  SHVC_2B3B_01,
  SHVC_2DC0N_01,
  SHVC_2E3M_01,
  SHVC_2J0N_01,
  SHVC_2J0N_10,
  SHVC_2J0N_11,
  SHVC_2J0N_20,
  SHVC_2J3M_01,
  SHVC_2J3M_11,
  SHVC_2J3M_20,
  SHVC_2J5M_01,
  SHVC_2P3B_01,
  SHVC_3J0N_01,
  SHVC_4PV5B_01,
  SHVC_BA0N_01,
  SHVC_BA0N_10,
  SHVC_BA1M_01,
  SHVC_BA3M_01,
  SHVC_BA3M_10,
  SHVC_BJ0N_01,
  SHVC_BJ0N_20,
  SHVC_BJ1M_10,
  SHVC_BJ1M_20,
  SHVC_BJ3M_10,
  SHVC_BJ3M_20,
  SHVC_LDH3C_01,
  SHVC_LJ3M_01,
  SHVC_LN3B_01,
  SHVC_SGB2_01,
  SHVC_YA0N_01,
  SHVC_YJ0N_01,
  SNSP_1C0N5S_01,
  SNSP_1L0N3S_01,
  SNSP_1N0N_01;

  public static BoardType fromString(String str) {
    if (str == null || str.isBlank()) {
      return UNKNOWN;
    }

    String normalized = str.toUpperCase().replace('-', '_').replace('#', '_');

    try {
      return BoardType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      return UNKNOWN;
    }
  }

  @Override
  public String toString() {
    String name = name();

    // Replace trailing _A with #A
    if (name.endsWith("_A")) {
      name = name.substring(0, name.length() - 2) + "#A";
    }

    return name.replace('_', '-');
  }
}
