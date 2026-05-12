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
import java.util.List;
import ghidra.util.task.TaskMonitor;

/** Factories for loading SNES ROM bytes into temporary Ghidra {@link Program}s in tests. */
public final class ProgramFactory {
  private ProgramFactory() {}

  /**
   * Loads SNES ROM bytes with the project's loader and returns the loaded {@link Program}s.
   *
   * <p>The caller is responsible for closing the returned {@link LoadResults}.
   */
  @SuppressWarnings("unchecked")
  public static LoadResults<Program> loadSnesRom(String name, byte[] romBytes) throws Exception {
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

  private static synchronized void initializeGhidraApplication() throws IOException {
    if (Application.isInitialized()) {
      return;
    }

    Application.initializeApplication(
      new GhidraApplicationLayout(resolveInstallDirectory()),
      new HeadlessGhidraApplicationConfiguration()
    );
  }

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
