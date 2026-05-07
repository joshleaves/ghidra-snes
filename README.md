# ghidra-snes

SNES tooling for Ghidra.

This project provides a Ghidra extension for loading and working with SNES ROMs. It currently includes:

- a SNES ROM loader with LoROM / HiROM support
- a 65816 language specification tailored for SNES analysis
- SNES memory helpers (MMIO, WRAM, mirrors) and register/vector labels

## Features
### SNES ROM loader

The loader maps SNES ROMs into the SNES CPU address space at import time instead of treating them as raw binary blobs.

Supported mappings (canonical):

- LoROM: `80–FF:8000–FFFF`
- HiROM: `C0–FF:0000–FFFF`
- WRAM: `7E–7F:0000–FFFF`
- SNES MMIO register ranges

ROM mapping detection is score-based and compares LoROM and HiROM header candidates. It also supports optional `0x200`-byte SMC header adjustment.

The loader creates primary (canonical) ROM banks; mirror banks are optional and managed by the plugin UI.

Loader implementation:

- `src/main/java/snesloader/SnesRomLoader.java`

### 65816 language support

The extension includes a SNES-oriented 65816 language definition:

- language id: `65816:LE:24:snes`
- compiler spec: `default`

This allows imported ROMs to use native 24-bit 65816 addressing directly in Ghidra.

## Build

Set `GHIDRA_INSTALL_DIR` to your local Ghidra install and run:

```shell
GHIDRA_INSTALL_DIR=/path/to/ghidra ./gradlew clean buildExtension
```

The built extension zip will be created in `dist/`.

## Install

1. Open Ghidra.
2. Go to **File → Install Extensions...**
3. Click **+** and select the zip from `dist/`.
4. Restart Ghidra.

When importing a ROM, select the **SNES ROM Loader** format.

## Third-party code notice

This project includes code originally sourced from [ghidra-65816](https://github.com/achan1989/ghidra-65816).

All credit for the original implementation goes to its respective authors.

Parts of the codebase, notably the 65816 language specification, are derived from that repository, with modifications for compatibility, fixes, and integration.

Additional resources:

- The SNES register list is based on [undisbeliever's Register Cheat Sheet](https://undisbeliever.net/snesdev/registers/cheatsheet.html).
- The Super Famicom logo icon is sourced from [Wikimedia Commons](https://en.wikipedia.org/wiki/File:Super_Famicom_logo.svg). It is considered to be in the public domain due to lack of originality, but may still be subject to trademark laws.

## Special thanks

Special thanks to Near (formerly known as byuu), whose contributions to SNES documentation, emulation, and preservation have had a lasting impact on the community.
