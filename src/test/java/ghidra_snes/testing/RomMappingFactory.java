/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.testing;

import ghidra_snes.common.RomMapType;
import java.util.List;
import java.util.stream.StreamSupport;

/** Convenience factories for asserting complete ROM mapping sequences in unit tests. */
public final class RomMappingFactory {
  private RomMappingFactory() {}

  /**
   * Materializes mapping chunks into a list.
   *
   * <p>Prefer direct iterator assertions when a test cares about iterator state. Use this helper
   * when the expected behavior is the full sequence of generated chunks.
   */
  public static List<RomMapType.MappingChunk> chunksFor(RomMapType romMapType, long romSize) {
    return StreamSupport.stream(romMapType.mappingChunks(romSize).spliterator(), false).toList();
  }
}
