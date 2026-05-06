/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ghidra;

import ghidra.app.util.opinion.AbstractProgramLoader;
import ghidra.app.util.opinion.LoadSpec;
import ghidra.program.model.lang.Endian;
import ghidra.program.model.lang.LanguageCompilerSpecPair;
import ghidra.program.model.lang.LanguageCompilerSpecQuery;
import ghidra.program.model.lang.LanguageNotFoundException;
import ghidra.program.model.lang.LanguageService;
import ghidra.program.model.lang.Processor;
import ghidra.program.model.lang.ProcessorNotFoundException;
import java.util.ArrayList;
import java.util.List;

public final class LanguageHelper {
  private static final String SNES_65816_LANGUAGE_ID = "65816:LE:24:snes";
  private static final String SNES_65816_COMPILER_SPEC_ID = "default";
  private static String SNES_65816_PROC_ID = "65816";
  private static String SNES_65816_VARIANT_ID = "snes";

  /**
   * Finds the preferred SNES 65816 language/compiler spec pair.
   *
   * The loader first asks for the exact bundled language id. If it cannot be found, it falls back
   * to a query for any compatible 65816 little-endian 24-bit SNES language. If no language module
   * is installed, the caller will provide an incomplete LoadSpec so Ghidra can still present the
   * loader as a candidate.
   *
   * @return matching load specs, possibly empty
   */
  public static List<LoadSpec> find65816LoadSpecs(
      AbstractProgramLoader loader, LanguageService languageService) {
    var specs = new ArrayList<LoadSpec>();

    try {
      var preferred =
          new LanguageCompilerSpecPair(SNES_65816_LANGUAGE_ID, SNES_65816_COMPILER_SPEC_ID);

      if (languageService.getLanguage(preferred.languageID) != null) {
        specs.add(new LoadSpec(loader, 0, preferred, true));
        return specs;
      }

      Processor processor = Processor.toProcessor(SNES_65816_PROC_ID);
      String variant = SNES_65816_VARIANT_ID;
      var query = new LanguageCompilerSpecQuery(processor, Endian.LITTLE, 24, variant, null);

      for (var pair : languageService.getLanguageCompilerSpecPairs(query)) {
        specs.add(new LoadSpec(loader, 0, pair, true));
      }

    } catch (LanguageNotFoundException | ProcessorNotFoundException ignored) {
      // 65816 language module isn't installed. We'll use incomplete LoadSpec fallback.
    }

    return specs;
  }
}
