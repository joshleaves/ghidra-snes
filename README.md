# ghidra-snes

Small Ghidra scripts for working with SNES ROMs.

Depending on needs, some bits may be copied/ported from:
https://github.com/achan1989/ghidra-snes-loader

---

## MapSnesBanks.java

Maps a LoROM/HiROM into a CPU-like memory layout:

- LoROM: `00–7D:8000–FFFF`
- HiROM: `40–7F:0000–FFFF`
- WRAM: `7E–7F:0000–FFFF`

No mirrors, no IO, no bus modeling: just a clean view to follow code and pointers.

The script:
- auto-detects LoROM/HiROM
- uses Ghidra FileBytes (no dependency on the initial memory map)
- can be safely re-run (it rebuilds the mapping)

Tested on Ghidra 12.0.4.
