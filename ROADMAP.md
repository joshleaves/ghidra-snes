# V1 scope

The goal for V1 is to provide a working SNES ROM loader/plugin for recent Ghidra builds, focused on clean static analysis.

## Done / mostly done

- Load SNES ROM files through a dedicated Ghidra loader.
- Detect LoROM and HiROM using header-based heuristics.
- Support optional `0x200` byte SMC headers.
- Map canonical ROM banks:
  - LoROM: `80–FF:8000–FFFF`
  - HiROM: `C0–FF:0000–FFFF`
- Use `FileBytes`-backed memory blocks so the Memory Map shows ROM file offsets.
- Map SNES WRAM.
- Map SNES MMIO regions.
- Add labels for SNES MMIO registers.
- Add labels for SNES interrupt/vector entries.
- Add UI toggles for managed memory blocks:
  - MMIO
  - WRAM
  - SRAM, when declared by the ROM header
  - ROM mirrors
- Store plugin metadata/state in hidden `ProgramUserData`, not editable Program Options.
- Provide a toolbar/menu entry for SNES memory helpers.
- Document third-party code/assets and credits.

## V1 cleanup before release

- [ ] Re-check LoROM mirror mapping after switching canonical LoROM banks to `80–FF`.
- [ ] Re-check HiROM mirror mapping, especially `00–3F` / `80–BF` half-bank mirrors.
- [ ] Verify SRAM size decoding against known ROMs.
- [ ] Verify SRAM mapping for LoROM and HiROM separately.
- [ ] Ensure `README.md` matches the current canonical mapping behavior.
- [ ] Test import on several ROM types:
  - [ ] Small LoROM
  - [ ] Large LoROM
  - [ ] HiROM
  - [ ] ROM with SRAM
  - [ ] ROM with SMC header

## API cleanup: Introduce Mapping strategies

Introduce mapping strategy objects instead of spreading mapper-specific logic through the loader/plugin:
- LoROM
- HiROM
- ExHiROM
- possible board-specific mappings later

Each strategy should own:
- Canonical ROM mapping
- Mirror mappings (SRAM-less ROMs often got additional mirror mappings in $40:OOOO and $C0:0000)
- Header location
- SRAM mapping
- Vector/header assumptions

# Near-term ideas

## Special chips
Investigate best-effort support for common special chips:

- SuperFX
- SA-1
- DSP variants
- Cx4
- S-DD1
- SPC7110

Likely first step:
- Detect chip when possible from header/metadata
- Expose manual “Add chip mapping” actions
- Map static register ranges and labels only

## Analyzer support
Add a Ghidra analyzer to apply higher-level SNES structure after import:

- [ ] Apply an SNES header struct
- [ ] Label/check interrupt vectors
- [ ] Optionally create/reset entry points
- [ ] Add bookmarks for header, vectors, MMIO, SRAM
- [ ] Add comments for decoded header fields

## Memory-map UI
Replace or supplement simple toggles with a dedicated SNES Memory panel:

- [ ] Checkbox-based options (immediate apply on click)
- [ ] Confirmation before removing blocks with user annotations
- [ ] Display a map image with dynamic overlays for active/inactive ranges

# Backlog / advanced support

## ExHiROM
Add ExHiROM support once LoROM/HiROM behavior is stable.
Open questions:

- Canonical mapping ranges
- Mirror behavior
- Header location
- SRAM behavior
- How much should be heuristic vs. explicit mapper strategy

## Board/database mapping
Investigate `Super Famicom.bml` and integrate known board databases later.

Potential use:
- Identify board type from checksum/metadata
- Apply more accurate mapping for known clean dumps

Caveat:
- ROM hacks, translations, randomizers, and modified dumps can invalidate checksum/database assumptions, so the loader must remain useful without board DB support.

## 65816 language spec issues
Investigate feedback from SNESDev users about the 65816 language spec:
- References on immediate vs absolute operands.
- Incorrect or missing reference generation.

# Guiding principles
- Keep canonical ROM banks stable; treat mirrors as optional analysis aids.
- Avoid destructive behavior: never remove user-created blocks accidentally.
- Keep all plugin-managed memory blocks under clear `snes_*` names.
- Keep ROM hacks usable, even when headers/checksums/databases are wrong.
