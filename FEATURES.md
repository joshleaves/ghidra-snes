

# ghidra-snes Features

## ROM Loading

- Automatic SNES/SFC ROM header detection
- Supports rom mappings:
  - LoROM
  - HiROM
  - ExHiROM
  - SA-1
  - S-DD1
  - SPC7110
- Automatic copier (`.smc`) header detection
- Internal ROM metadata extraction:
  - Title
  - ROM size
  - RAM size
  - Mapping mode
  - Checksums
- ROM checksum verification
- Persistent cartridge metadata stored in Ghidra program options

## Memory Mapping

- Canonical SNES ROM memory mapping
- Canonical SNES RAM memory mapping (`$7E:0000-$7F-FFFF`)
- Canonical System Area mapping (`$00:0000-$5FFF`)
- First-bank ROM mirrors (`$00:8000-$FFFF`) correctly expose internal ROM headers
- MMIO register blocks and labels
- Separate ROM/System memory views

## Language Support

- 65C816-compatible loading workflow
- Automatic reset vector discovery
- Interrupt vector exposure
- Named MMIO labels
- Ghidra-compatible memory block layout

## Testing

- Unit-tested ROM mapping logic
- Unit-tested checksum logic
- Functional tests against real commercial ROM dumps
- Ghidra test harness integration tests

## Current Limitations

- Enhancement chips are currently treated as memory-layout variants, not full hardware implementations
- SA-1, S-DD1, and SPC7110 mappings are intended for static analysis, not emulation accuracy
- Some enhancement-chip mirror behavior may still differ from emulator implementations
- No automatic update mechanism yet

## Planned / Future Work

- Improved enhancement-chip mapping accuracy
- Loader UX polish
- Additional ROM metadata support
- Optional update checker
- Additional analyzers and auto-labeling