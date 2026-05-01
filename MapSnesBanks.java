//Map SNES LoROM/HiROM banks into Ghidra memory
//@category SNES

import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.*;
import ghidra.program.model.mem.*;
import ghidra.program.database.mem.FileBytes;

public class MapSnesBanks extends GhidraScript {
  private Memory mem;
  private AddressSpace space;
  private long romSize;
  private FileBytes romFileBytes;

  public void run() throws Exception {
    mem = currentProgram.getMemory();
    space = currentProgram.getAddressFactory().getDefaultAddressSpace();

    loadRomFileBytesFromCurrentProgram();

    String mode = detectMapping();

    println("Mapping as " + mode + ", ROM size = 0x" + Long.toHexString(romSize));

    clearMemoryMap();

    if (mode.equals("LoROM")) mapLoROM();
    else if (mode.equals("HiROM")) mapHiROM();
    else {
      printerr("Could not detect mapping; mapping raw ROM at 0x000000 instead");
      mapRawRom();
    }
    mapWram();
  }

  private String detectMapping() throws Exception {
    int lo = scoreHeader(0x7fc0, true);
    int hi = scoreHeader(0xffc0, false);

    println("LoROM score: " + lo);
    println("HiROM score: " + hi);

    if (lo > hi) return "LoROM";
    if (hi > lo) return "HiROM";
    return "Unknown";
  }

  private int scoreHeader(long off, boolean lorom) throws Exception {
    if (off + 0x40 > romSize) return -999;

    int score = 0;

    // Title: 21 mostly printable ASCII bytes.
    for (int i = 0; i < 21; i++) {
      int b = u8(off + i);
      if (b >= 0x20 && b <= 0x7e) score++;
      else score -= 2;
    }

    int mapMode = u8(off + 0x15);
    int low = mapMode & 0x0f;

    if (lorom && (low == 0x0 || low == 0x3)) score += 8; // common LoROM-ish
    if (!lorom && (low == 0x1 || low == 0x5)) score += 8; // common HiROM/ExHiROM-ish

    int comp = u16(off + 0x1c);
    int checksum = u16(off + 0x1e);
    if (((comp ^ checksum) & 0xffff) == 0xffff) score += 8;

    int reset = u16(off + 0x3c);
    if (reset >= 0x8000 && reset <= 0xffff) score += 8;
    if (reset == 0xffff || reset == 0x0000) score -= 8;

    return score;
  }

  private void mapRawRom() throws Exception {
    map("rom", 0x000000, 0, romSize);
  }

  private void mapLoROM() throws Exception {
    long banks = (romSize + 0x7fff) / 0x8000;

    for (long bank = 0; bank < banks && bank <= 0x7d; bank++) {
      long off = bank * 0x8000;
      map("lorom_%02X".formatted(bank), (bank << 16) | 0x8000, off, 0x8000);
    }
  }

  private void mapHiROM() throws Exception {
    long banks = (romSize + 0xffff) / 0x10000;

    for (long i = 0; i < banks; i++) {
      long off = i * 0x10000;

      long bank = 0x40 + i;
      if (bank <= 0x7f) {
        map("hirom_%02X".formatted(bank), bank << 16, off, 0x10000);
      }
    }
  }

  private void loadRomFileBytesFromCurrentProgram() throws Exception {
    // FileBytes are the imported bytes for the currently open Ghidra program.
    // They remain available even after we remove all MemoryBlocks below.
    java.util.List<FileBytes> allFileBytes = mem.getAllFileBytes();
    if (allFileBytes.isEmpty()) {
      throw new RuntimeException("No FileBytes found for imported ROM");
    }

    romFileBytes = allFileBytes.get(0);
    for (FileBytes fileBytes : allFileBytes) {
      if (fileBytes.getSize() > romFileBytes.getSize()) {
        romFileBytes = fileBytes;
      }
    }

    romSize = romFileBytes.getSize();
    println("Using FileBytes: " + romFileBytes.getFilename() + ", size = 0x" + Long.toHexString(romSize));
  }

  private void clearMemoryMap() throws Exception {
    MemoryBlock[] blocks = mem.getBlocks();

    for (MemoryBlock block : blocks) {
      println("remove block: " + block.getName());
      mem.removeBlock(block, monitor);
    }
  }

  private void mapWram() throws Exception {
    uninit("snes_wram", 0x7e0000, 0x20000, true, true, false);
  }

  private void uninit(String name, long cpu, long size, boolean r, boolean w, boolean x) throws Exception {
    Address dst = space.getAddress(cpu);

    try {
      MemoryBlock block = mem.createUninitializedBlock(name, dst, size, false);
      block.setRead(r);
      block.setWrite(w);
      block.setExecute(x);

      println("%-14s CPU 0x%06X size 0x%X"
        .formatted(name, cpu, size));
    } catch (MemoryConflictException e) {
      println("skip conflict: " + name + " at 0x" + Long.toHexString(cpu));
    }
  }

  private void map(String name, long cpu, long off, long size) throws Exception {
    if (off >= romSize) return;
    if (off + size > romSize) size = romSize - off;
    if (size <= 0) return;

    Address dst = space.getAddress(cpu);

    try {
      MemoryBlock block = mem.createInitializedBlock(
        name,
        dst,
        romFileBytes,
        off,
        size,
        false
      );
      block.setRead(true);
      block.setWrite(false);
      block.setExecute(true);

      println("%-14s file[%06X, +0x%X] -> CPU 0x%06X"
        .formatted(name, off, size, cpu));
    } catch (MemoryConflictException e) {
      println("skip conflict: " + name + " at 0x" + Long.toHexString(cpu));
    }
  }

  private int u8(long off) throws Exception {
    byte[] b = new byte[1];
    romFileBytes.getOriginalBytes(off, b, 0, 1);
    return b[0] & 0xff;
  }

  private int u16(long off) throws Exception {
    return u8(off) | (u8(off + 1) << 8);
  }
}