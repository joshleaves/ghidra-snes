/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.options;

import ghidra.framework.options.Options;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.ProgramUserData;
import ghidra_snes.SnesCartridge;
import ghidra_snes.common.SnesRomHeader;

public final class SnesOptions {
  public static final String OPTIONS_NAME = "SNES ROM";

  public static final String OPT_CART_MAPPER         = "cart.mapper";
  public static final String OPT_CART_TITLE          = "cart.title";
  public static final String OPT_CART_SRAM_SIZE      = "cart.sram_size";
  public static final String OPT_HEADER_LOCATION     = "header.location";
  public static final String OPT_HEADER_RAW_BYTES    = "header.rawBytes";
  public static final String OPT_ROM_SIZE            = "rom.size";
  public static final String OPT_FILE_HAS_SMC_HEADER = "file.hasSmcHeader";
  public static final String OPT_FILE_ROM_OFFSET     = "file.romOffset";
  public static final String OPT_METADATA_SOURCE     = "metadata.source";

  private static final String  DEFAULT_CART_MAPPER         = "UNKNOWN";
  private static final String  DEFAULT_CART_TITLE          = "";
  private static final int     DEFAULT_CART_SRAM_SIZE      = 0;
  private static final long    DEFAULT_HEADER_LOCATION     = SnesRomHeader.LOROM_HEADER_OFFSET - 1;
  private static final byte[]  DEFAULT_HEADER_RAW_BYTES    = new byte[Math.toIntExact(SnesRomHeader.HEADER_SIZE)];
  private static final long    DEFAULT_ROM_SIZE            = 0;
  private static final boolean DEFAULT_FILE_HAS_SMC_HEADER = false;
  private static final long    DEFAULT_FILE_ROM_OFFSET     = 0;
  private static final String  DEFAULT_METADATA_SOURCE     = SnesCartridge.MetadataSource.UNKNOWN.name();

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
    setOptions(
        program,
        options -> {
          options.setString(   OPT_CART_MAPPER,         cartridge.getRomMapType().toString());
          options.setString(   OPT_CART_TITLE,          cartridge.getRomHeader().titleString());
          options.setLong(     OPT_CART_SRAM_SIZE,      cartridge.getRomHeader().ramSize());
          options.setLong(     OPT_HEADER_LOCATION,     cartridge.getRomHeader().location());
          options.setByteArray(OPT_HEADER_RAW_BYTES,    cartridge.getRomHeader().romHeaderBytes());
          options.setLong(     OPT_ROM_SIZE,            cartridge.getRomSizeBytes());
          options.setBoolean(  OPT_FILE_HAS_SMC_HEADER, cartridge.hasCopierHeader());
          options.setLong(     OPT_FILE_ROM_OFFSET,     cartridge.getRomOffset());
          options.setString(   OPT_METADATA_SOURCE,     cartridge.getMetadataSource().name());
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
   * Reads a long value from the SNES option store.
   */
  private static long getLong(Program program, String optionName, long defaultValue) {
    return program.getProgramUserData().getOptions(OPTIONS_NAME).getLong(optionName, defaultValue);
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
   * Returns the raw detected SNES ROM header stored at import time.
   *
   * @param program target Ghidra program
   * @return parsed header record backed by the stored raw header bytes
   */
  public static SnesRomHeader getRomHeader(Program program) {
    Options optionObject = program.getProgramUserData().getOptions(OPTIONS_NAME);
    long headerLocation = optionObject.getLong(OPT_HEADER_LOCATION, DEFAULT_HEADER_LOCATION);
    byte[] rawHeaderBytes = optionObject.getByteArray(OPT_HEADER_RAW_BYTES, DEFAULT_HEADER_RAW_BYTES);
    return new SnesRomHeader(headerLocation, rawHeaderBytes);
  }

  /**
   * Returns the detected SNES ROM mapping type.
   *
   * @param program target Ghidra program
   * @return mapper type (LoROM, HiROM, ExHiROM, UNKNOWN)
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
  public static long getCartSramSize(Program program) {
    return getLong(program, OPT_CART_SRAM_SIZE, DEFAULT_CART_SRAM_SIZE);
  }

  /**
   * Returns the ROM payload size, excluding any copier/SMC header.
   *
   * @param program target Ghidra program
   * @return ROM size in bytes
   */
  public static long getRomSizeBytes(Program program) {
    return getLong(program, OPT_ROM_SIZE, DEFAULT_ROM_SIZE);
  }

  /**
   * Returns whether the loaded ROM originally contained a copier/SMC header.
   *
   * @param program target Ghidra program
   * @return true if a copier header was detected
   */
  public static boolean hasSmcHeader(Program program) {
    return getBoolean(program, OPT_FILE_HAS_SMC_HEADER, DEFAULT_FILE_HAS_SMC_HEADER);
  }

  /**
   * Returns the ROM payload offset inside the loaded file.
   *
   * @param program target Ghidra program
   * @return byte offset of the ROM payload
   */
  public static long getFileRomOffset(Program program) {
    return getLong(program, OPT_FILE_ROM_OFFSET, DEFAULT_FILE_ROM_OFFSET);
  }

  /**
   * Returns the source used for detected cartridge metadata.
   *
   * @param program target Ghidra program
   * @return metadata source name
   */
  public static String getMetadataSource(Program program) {
    return getString(program, OPT_METADATA_SOURCE, DEFAULT_METADATA_SOURCE);
  }
}
