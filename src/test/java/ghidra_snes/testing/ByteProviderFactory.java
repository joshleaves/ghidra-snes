/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.testing;

import ghidra.app.util.bin.ByteArrayProvider;

/** Factories for small in-memory {@link ByteArrayProvider} instances used by unit tests. */
public final class ByteProviderFactory {
  private ByteProviderFactory() {}

  /**
   * Builds a provider from unsigned byte literals.
   *
   * <p>Use this for compact synthetic data where writing {@code (byte) 0xff} repeatedly would
   * obscure the test case. Values are masked to 8 bits.
   */
  public static ByteArrayProvider fromUnsignedBytes(int... values) {
    byte[] bytes = new byte[values.length];
    for (int index = 0; index < values.length; index++) {
      bytes[index] = (byte) values[index];
    }
    return new ByteArrayProvider(bytes);
  }

  /**
   * Builds a named ROM provider from an existing byte array.
   *
   * <p>Use this for synthetic ROM images that need a stable filename in loader tests. Real-data
   * functional tests should keep constructing providers from their fixture files directly.
   */
  public static ByteArrayProvider namedRom(String name, byte[] bytes) {
    return new ByteArrayProvider(name, bytes);
  }
}
