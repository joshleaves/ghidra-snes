/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.options;

import ghidra.framework.options.Options;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.ProgramUserData;
import ghidra_snes.SnesCartridge;

public final class SnesOptions {
  public static final String OPTIONS_NAME = "SNES ROM";

  public static final String OPT_CART_MAPPER = "cart.mapper";
  public static final String OPT_CART_TITLE = "cart.title";
  public static final String OPT_CART_SRAM_SIZE = "cart.sram_size";

  public static final String OPT_ROM_HAS_SMC_HEADER = "rom.hasSmcHeader";

  private static final String DEFAULT_CART_MAPPER = "Raw";
  private static final String DEFAULT_CART_TITLE = "";
  private static final int DEFAULT_CART_SRAM_SIZE = 0;
  private static final boolean DEFAULT_ROM_HAS_SMC_HEADER = false;

  private SnesOptions() {}

  /**
   * Initializes SNES metadata options from a parsed cartridge.
   *
   * <p>This stores persistent ROM metadata into the Ghidra ProgramUserData
   * option store so it can later be reused by plugins, UI components,
   * analyzers, or mapping services.
   *
   * @param program target Ghidra program
   * @param cartridge parsed SNES cartridge metadata
   */
  public static void initializeFromCartridge(Program program, SnesCartridge cartridge) {
    String title = cartridge.getRomLabel().orElse("");
    int sramSize =
        cartridge.getRomHeader().map(SnesCartridge.SnesRomHeader::getSramSizeBytes).orElse(0);

    setOptions(
        program,
        options -> {
          options.setString(OPT_CART_MAPPER, cartridge.getRomType().toString());
          options.setString(OPT_CART_TITLE, title);
          options.setInt(OPT_CART_SRAM_SIZE, sramSize);
          options.setBoolean(OPT_ROM_HAS_SMC_HEADER, cartridge.hasCopierHeader());
        });
  }

  /**
   * Opens a ProgramUserData transaction and exposes the SNES option store.
   *
   * <p>This helper should be used whenever multiple options need to be
   * modified atomically.
   *
   * @param program target Ghidra program
   * @param action option mutation callback
   */
  public static void setOptions(Program program, java.util.function.Consumer<Options> action) {
    ProgramUserData userData = program.getProgramUserData();
    int transactionId = userData.startTransaction();
    try {
      Options options = userData.getOptions(OPTIONS_NAME);
      action.accept(options);
    } finally {
      userData.endTransaction(transactionId);
    }
  }

  /**
   * Reads a string value from the SNES option store.
   */
  private static String getString(Program program, String optionName, String defaultValue) {
    return program
        .getProgramUserData()
        .getOptions(OPTIONS_NAME)
        .getString(optionName, defaultValue);
  }

  /**
   * Reads an integer value from the SNES option store.
   */
  private static int getInt(Program program, String optionName, int defaultValue) {
    return program.getProgramUserData().getOptions(OPTIONS_NAME).getInt(optionName, defaultValue);
  }

  /**
   * Reads a boolean value from the SNES option store.
   */
  private static boolean getBoolean(Program program, String optionName, boolean defaultValue) {
    return program
        .getProgramUserData()
        .getOptions(OPTIONS_NAME)
        .getBoolean(optionName, defaultValue);
  }

  /**
   * Returns the detected SNES ROM mapping type.
   *
   * @param program target Ghidra program
   * @return mapper type (LoROM, HiROM, ExHiROM, Raw)
   */
  public static String getCartMapper(Program program) {
    return getString(program, OPT_CART_MAPPER, DEFAULT_CART_MAPPER);
  }

  /**
   * Returns the internal ROM title extracted from the cartridge header.
   *
   * @param program target Ghidra program
   * @return cartridge title or empty string if unavailable
   */
  public static String getCartTitle(Program program) {
    return getString(program, OPT_CART_TITLE, DEFAULT_CART_TITLE);
  }

  /**
   * Returns the detected SRAM size in bytes.
   *
   * @param program target Ghidra program
   * @return SRAM size in bytes
   */
  public static int getCartSramSize(Program program) {
    return getInt(program, OPT_CART_SRAM_SIZE, DEFAULT_CART_SRAM_SIZE);
  }

  /**
   * Returns whether the loaded ROM originally contained a copier/SMC header.
   *
   * @param program target Ghidra program
   * @return true if a copier header was detected
   */
  public static boolean hasSmcHeader(Program program) {
    return getBoolean(program, OPT_ROM_HAS_SMC_HEADER, DEFAULT_ROM_HAS_SMC_HEADER);
  }
}
