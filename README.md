![](src/main/resources/images/ghidra-snes.png)

SNES tooling for Ghidra.

This project provides a Ghidra extension for loading and working with SNES ROMs. It currently includes:

- a SNES ROM loader
- a 65816 language specification tailored for SNES analysis
- SNES memory helpers (MMIO, WRAM, mirrors) and register/vector labels

# Features
## SNES ROM loader

The loader maps SNES ROMs into the SNES CPU address space at import time instead of treating them as raw binary blobs.

Supported mappings (canonical):

- LoROM: `80–FF:8000–FFFF`
- HiROM: `C0–FF:0000–FFFF`
- WRAM: `7E–7F:0000–FFFF`
- SNES MMIO register ranges

ROM mapping detection is score-based and compares LoROM and HiROM header candidates. It also supports optional `0x200`-byte SMC header adjustment.

The loader creates primary (canonical) ROM banks and required mirror banks (`00:0000-5FFF` for SNES system RAM & I/O, and `00:8000-FFFF` for ROM). Other mirrors can be managed by the (coming next) plugin UI.

Loader implementation:

- `src/main/java/ghidra_snes/loader/SnesRomLoader.java`

### Carts support

- ✅ [LoROM](https://problemkaputt.de/fullsnes.htm#snescartlorommappingromdividedinto32kbanksaround1500games): Supported.
- ✅ [HiROM](https://problemkaputt.de/fullsnes.htm#snescarthirommappingromdividedinto64kbanksaround500games): Supported.
- ✅ [ExHiROM](https://problemkaputt.de/fullsnes.htm#snescartridgeromimageinterleave): Supported.
- ✅ [SA-1](https://problemkaputt.de/fullsnes.htm#snescartsa1programmable65c816cpuakasuperaccelerator35games): Supported.
- ✅ [GSU-n](https://problemkaputt.de/fullsnes.htm#snescartgsunprogrammablerisccpuakasuperfxmariochip10games): Supported.
- ✅ [Capcom CX-4](https://problemkaputt.de/fullsnes.htm#snescartcapcomcx4programmablerisccpumegamanx232games): Supported.
- ✅ [DSP-n](https://problemkaputt.de/fullsnes.htm#snescartdspnst010st011preprogrammednecupd77c25cpu23games): Supported.
- ✅ [OBC1](https://problemkaputt.de/fullsnes.htm#snescartobc1objcontroller1game): Supported.
- ⚠️ [S-DD1](https://problemkaputt.de/fullsnes.htm#snescartsdd1datadecompressor2games): Partial support. Canonical ROM views are available, but some mappings currently differ from emulator behavior.
- ⚠️ [SPC7110](https://problemkaputt.de/fullsnes.htm#snescartspc7110datadecompressor3games): Partial support. Canonical ROM views are available, but some mappings currently differ from emulator behavior.

More details: [Functional tests](FUNCTIONAL.md)

## 65816 language support

The extension includes a SNES-oriented 65816 language definition:

- language id: `65816:LE:24:snes`
- compiler spec: `default`

This allows imported ROMs to use native 24-bit 65816 addressing directly in Ghidra.

# Build

Set `GHIDRA_INSTALL_DIR` to your local Ghidra install and run:

```shell
GHIDRA_INSTALL_DIR=/path/to/ghidra ./gradlew clean buildExtension
```

The built extension zip will be created in `dist/`.

# Install

1. Open Ghidra.
2. Go to **File → Install Extensions...**
3. Click **+** and select the zip from `dist/`.
4. Restart Ghidra.

When importing a ROM, select the **SNES ROM Loader** format.

# Third-party code notice

- This project includes code originally sourced from [ghidra-65816](https://github.com/achan1989/ghidra-65816), licensed under the MIT License. Parts of the codebase, notably the 65816 language specification, are derived from that repository, with modifications for compatibility, fixes, and integration.

- Parts of the SPC7110 checksum behavior were implemented based on observations from [SuperFamiCheck](https://github.com/Optiroc/SuperFamicheck), licensed under the MIT License.

## Additional resources

- A lot of informations about the SNES Memory Map was taken from the [SNESDev Wiki](https://snes.nesdev.org/wiki/Memory_map).
- The SNES register list is based on [undisbeliever's Register Cheat Sheet](https://undisbeliever.net/snesdev/registers/cheatsheet.html).
- The Super Famicom logo icon is sourced from [Wikimedia Commons](https://en.wikipedia.org/wiki/File:Super_Famicom_logo.svg). It is considered to be in the public domain due to lack of originality, but may still be subject to trademark laws.
- One test ROM embeds the complete text of Aleph One's famous [Smashing The Stack For Fun And Profit](https://phrack.org/issues/49/14), originally published in Phrack 49, for testing and debugging purposes.

# Special thanks

Special thanks to Near (formerly known as byuu), whose contributions to SNES documentation, emulation, and preservation have had a lasting impact on the community.
