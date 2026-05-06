/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

public enum RomType {
  LoROM,
  HiROM,
  ExHiROM,
  Raw;

  public Iterable<MappingChunk> mappingChunks(long romSize) {
    return switch (this) {
      case LoROM ->
          () -> new RomMappingIterator(romSize, 0x8000, 0x8000, new BankRange(0x80, 0xff));
      case HiROM ->
          () -> new RomMappingIterator(romSize, 0x10000, 0x0000, new BankRange(0xc0, 0xff));
      case ExHiROM ->
          () ->
              new RomMappingIterator(
                  romSize, 0x10000, 0x0000, new BankRange(0xc0, 0xff), new BankRange(0x40, 0x7f));
      case Raw -> throw new IllegalStateException("Cannot map Raw ROM type");
    };
  }

  public record MappingChunk(int bank, long fileOffset, long cpuAddress, long requestedSize) {}

  private record BankRange(int start, int end) {}

  private static final class RomMappingIterator implements Iterator<MappingChunk> {
    private final long romSize;
    private final int chunkSize;
    private final int addressStart;
    private final BankRange[] bankRanges;

    private int rangeIndex;
    private int bank;
    private long fileOffset;

    private RomMappingIterator(
        long romSize, int chunkSize, int addressStart, BankRange... bankRanges) {
      this.romSize = Math.max(0, romSize);
      this.chunkSize = chunkSize;
      this.addressStart = addressStart;
      this.bankRanges = bankRanges;
      this.rangeIndex = 0;
      this.bank = bankRanges.length == 0 ? 0x100 : bankRanges[0].start();
      this.fileOffset = 0;
    }

    @Override
    public boolean hasNext() {
      skipEmptyRanges();
      return fileOffset < romSize && rangeIndex < bankRanges.length;
    }

    @Override
    public MappingChunk next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      int size = (int) Math.min(chunkSize, romSize - fileOffset);
      long memoryOffset = ((long) bank << 16) | (addressStart & 0xFFFF);
      MappingChunk chunk = new MappingChunk(bank, fileOffset, memoryOffset, size);

      fileOffset += size;
      bank++;
      advanceRangeIfNeeded();

      return chunk;
    }

    private void advanceRangeIfNeeded() {
      while (rangeIndex < bankRanges.length && bank > bankRanges[rangeIndex].end()) {
        rangeIndex++;
        if (rangeIndex < bankRanges.length) {
          bank = bankRanges[rangeIndex].start();
        }
      }
    }

    private void skipEmptyRanges() {
      advanceRangeIfNeeded();
    }
  }
}
