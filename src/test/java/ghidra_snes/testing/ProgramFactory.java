/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.testing;

import ghidra.GhidraApplicationLayout;
import ghidra.app.util.Option;
import ghidra.app.util.bin.ByteArrayProvider;
import ghidra.app.util.importer.MessageLog;
import ghidra.app.util.opinion.LoadException;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.app.util.opinion.LoadResults;
import ghidra.app.util.opinion.Loader;
import ghidra.framework.Application;
import ghidra.framework.HeadlessGhidraApplicationConfiguration;
import ghidra.framework.TestApplicationUtils;
import ghidra.program.model.listing.Program;
import ghidra_snes.loader.SnesRomLoader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Assumptions;

import ghidra.util.task.TaskMonitor;

/**
 * Factory helpers for loading temporary SNES {@link Program}s in integration and harness tests.
 *
 * <p>This utility initializes a headless Ghidra test environment and loads ROM bytes through the
 * project's real {@link SnesRomLoader}, allowing tests to exercise the complete loader pipeline.
 */
public final class ProgramFactory {
  private static final Path REAL_DATA_DIR = Path.of("src/test/resources/data");
  private static final String SAMPLE_ROM = "smashing_the_stack.sfc";
  private ProgramFactory() {}

  /** Skips harness tests when no external Ghidra installation is configured. */
  public static void assumeGhidraInstallDirIsSet() {
    String ghidraInstallDir = System.getenv("GHIDRA_INSTALL_DIR");
    Assumptions.assumeTrue(
      ghidraInstallDir != null && !ghidraInstallDir.isBlank(),
      "GHIDRA_INSTALL_DIR is not defined"
    );
  }


  /**
   * Loads the default sample SNES ROM fixture through the project's real loader.
   *
   * <p>The returned {@link LoadResults} owns the created temporary {@link Program}s and must be
   * closed by the caller.
   */
  public static LoadResults<Program> loadSampleRom() throws Exception {
    Path data = REAL_DATA_DIR.resolve(SAMPLE_ROM);
    byte[] romData = Files.readAllBytes(data);
    return loadSnesRom(SAMPLE_ROM, romData);
  }

  /**
   * Loads arbitrary SNES ROM bytes through the project's real {@link SnesRomLoader}.
   *
   * <p>This helper performs the same loader discovery, option validation, and import workflow used
   * by Ghidra during a normal import operation.
   *
   * <p>The returned {@link LoadResults} owns the created temporary {@link Program}s and must be
   * closed by the caller.
   *
   * @param name logical ROM name used for the temporary import session
   * @param romBytes raw SNES ROM bytes
   * @return the loaded temporary {@link Program}s
   */
  @SuppressWarnings("unchecked")
  public static LoadResults<Program> loadSnesRom(String name, byte[] romBytes) throws Exception {
    assumeGhidraInstallDirIsSet();
    initializeGhidraApplication();

    ByteArrayProvider provider = new ByteArrayProvider(name + ".sfc", romBytes);
    SnesRomLoader loader = new SnesRomLoader();
    LoadSpec loadSpec = loader.findSupportedLoadSpecs(provider).stream()
      .findFirst()
      .orElseThrow(() -> new LoadException("No load spec found for " + name));

    List<Option> options = loader.getDefaultOptions(provider, loadSpec, null, false, false);
    String validationError = loader.validateOptions(provider, loadSpec, options, null);
    if (validationError != null) {
      throw new LoadException(validationError);
    }

    Loader.ImporterSettings settings = new Loader.ImporterSettings(
      provider,
      name,
      null,
      "/",
      false,
      loadSpec,
      options,
      ProgramFactory.class,
      new MessageLog(),
      TaskMonitor.DUMMY
    );

    return (LoadResults<Program>) (LoadResults<?>) loader.load(settings);
  }

  /** Initializes a reusable headless Ghidra application instance for tests. */
  private static synchronized void initializeGhidraApplication() throws IOException {
    if (Application.isInitialized()) {
      return;
    }

    Application.initializeApplication(
      new GhidraApplicationLayout(resolveInstallDirectory()),
      new HeadlessGhidraApplicationConfiguration()
    );
  }

  /** Resolves the active Ghidra installation directory used by the test harness. */
  private static File resolveInstallDirectory() {
    String configuredInstall = System.getenv("GHIDRA_INSTALL_DIR");
    File baseDir = configuredInstall != null
      ? new File(configuredInstall)
      : TestApplicationUtils.getInstallationDirectory();

    if (new File(baseDir, "Ghidra/application.properties").isFile()) {
      return baseDir;
    }

    if ("Ghidra".equals(baseDir.getName()) && new File(baseDir, "application.properties").isFile()) {
      File parent = baseDir.getParentFile();
      if (parent != null) {
        return parent;
      }
    }

    return baseDir;
  }
}
