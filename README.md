# ghidra-snes

SNES tooling for Ghidra.

This repository now includes a Ghidra **Loader extension** that maps SNES ROMs as LoROM/HiROM at import time.

## SNES Loader extension

The loader class is:

- `src/main/java/snesloader/SnesRomLoader.java`

It maps:

- LoROM: `00–7D:8000–FFFF`
- HiROM: `40–7F:0000–FFFF`
- WRAM: `7E–7F:0000–FFFF`

Detection is score-based (LoROM header vs HiROM header) and includes optional 0x200-byte SMC header adjustment.

### Build

Set `GHIDRA_INSTALL_DIR` to your local Ghidra install and run:

```shell
GHIDRA_INSTALL_DIR=/path/to/ghidra ./gradlew clean buildExtension
```

The built extension zip will be created in `dist/`.

### Install

1. Open Ghidra.
2. Go to **File → Install Extensions...**
3. Click **+** and pick the zip from `dist/`.
4. Restart Ghidra.

When importing a ROM, select the **SNES ROM** format.

## Legacy script

`MapSnesBanks.java` is kept as a standalone script reference.

> [!CAUTION]
> The script clears all memory blocks before remapping, which destroys existing analysis.


# Third-party code notice

This project includes code originally sourced from:
[ghidra-65816](https://github.com/achan1989/ghidra-65816)

All credit for the original implementation goes to its respective authors.

Parts of the codebase (notably the 65816 language specification) are derived from that repository, with modifications for compatibility, fixes, and integration.

The register list has been taken from [undisbeliever's Register Cheat Sheet](https://undisbeliever.net/snesdev/registers/cheatsheet.html).