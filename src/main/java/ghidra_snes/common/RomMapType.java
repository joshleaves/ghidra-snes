/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.common;

import java.util.Iterator;
import java.util.NoSuchElementException;

public enum RomMapType {
  LoROM,
  HiROM,
  ExHiROM,
  SA_1,
  SPC7110,
  S_DD1,
  UNKNOWN;

  public Iterable<MappingChunk> mappingChunks(long romSize) {
    return switch (this) {
      case LoROM ->
          () -> new RomMappingIterator(romSize,
                  new BankRange(0x80, 0xff, 0x8000, 0x8000, 0));
      case HiROM, SA_1 ->
          () -> new RomMappingIterator(romSize,
                  new BankRange(0xc0, 0xff, 0x10000, 0x0000, 0));
      case ExHiROM, SPC7110, S_DD1 ->
          () -> new RomMappingIterator(romSize,
                  new BankRange(0xc0, 0xff, 0x10000, 0x0000, 0),
                  new BankRange(0x40, 0x7f, 0x10000, 0x0000, 0),
                  new BankRange(0x3e, 0x3f, 0x8000, 0x8000, 0x8000));
      case UNKNOWN -> throw new IllegalStateException("Cannot map unknown ROM type");
    };
  }

  public record MappingChunk(int bank, long fileOffset, long cpuAddress, long requestedSize) {}

  private record BankRange(int start, int end, int chunkSize, int addressStart, int fileSkip) {}

  private static final class RomMappingIterator implements Iterator<MappingChunk> {
    private final long romSize;
    private final BankRange[] bankRanges;

    private int rangeIndex;
    private int bank;
    private long fileOffset;

    private RomMappingIterator(long romSize, BankRange... bankRanges) {
      this.romSize = Math.max(0, romSize);
      this.bankRanges = bankRanges;
      this.rangeIndex = 0;
      this.bank = bankRanges.length == 0 ? 0x100 : bankRanges[0].start();
      this.fileOffset = 0;
    }

    @Override
    public boolean hasNext() {
      skipEmptyRanges();
      if (rangeIndex >= bankRanges.length) {
        return false;
      }

      BankRange range = bankRanges[rangeIndex];
      return fileOffset + range.fileSkip() < romSize;
    }

    @Override
    public MappingChunk next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }

      BankRange range = bankRanges[rangeIndex];
      long chunkFileOffset = fileOffset + range.fileSkip();
      int size = (int) Math.min(range.chunkSize(), romSize - chunkFileOffset);
      long memoryOffset = ((long) bank << 16) | (range.addressStart() & 0xffff);
      MappingChunk chunk = new MappingChunk(bank, chunkFileOffset, memoryOffset, size);

      fileOffset += range.fileSkip() + size;
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
