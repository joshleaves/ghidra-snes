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
