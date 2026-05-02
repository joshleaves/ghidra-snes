/* (C) Arnaud 'red' Rouyer 0000 */
package snesplugin;

import docking.action.MenuData;
import docking.action.ToggleDockingAction;
import docking.action.ToolBarData;
import docking.menu.MultiActionDockingAction;
import docking.tool.ToolConstants;
import ghidra.MiscellaneousPluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.options.Options;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.util.task.TaskMonitor;
import java.util.List;
import javax.swing.Icon;
import resources.ResourceManager;
import snescommon.SnesCommon;

@PluginInfo(
    status = PluginStatus.RELEASED,
    packageName = MiscellaneousPluginPackage.NAME,
    category = PluginCategoryNames.COMMON,
    shortDescription = "SNES memory visibility controls",
    description = "Toolbar and menu controls for SNES ROM memory map visibility")
public class SnesPlugin extends ProgramPlugin {
  private static final Icon SNES_ICON = ResourceManager.loadImage("images/Super_Famicom_logo.png");

  private ToggleDockingAction displayMmioAction;
  private ToggleDockingAction displayMirrorsAction;
  private ToggleDockingAction displaySramAction;
  private ToggleDockingAction displayWramAction;

  private MultiActionDockingAction toolbarMemoryAction;

  public SnesPlugin(PluginTool tool) {
    super(tool);
    createActions();
  }

  /**
   * Creates the SNES memory actions exposed through the Tools menu and toolbar dropdown.
   *
   * Each action persists its state in hidden {@link Program#getProgramUserData()} options, then
   * asks the plugin to add or remove the corresponding memory blocks.
   */
  private void createActions() {
    displayMmioAction =
        new SnesMemoryToggleAction(
            tool,
            getName(),
            "Enable MMIO Mappings",
            "Add/remove SNES MMIO memory blocks",
            SnesCommon.OPTIONS_NAME,
            SnesCommon.OPT_MEMORY_DISPLAY_MMIO,
            true,
            this::getCurrentProgram,
            this::handleMemoryToggle,
            new MenuData(
                new String[] {ToolConstants.MENU_TOOLS, "SNES Memory", "Enable MMIO Mappings"},
                SNES_ICON));
    tool.addAction(displayMmioAction);

    displayMirrorsAction =
        new SnesMemoryToggleAction(
            tool,
            getName(),
            "Enable ROM Mirrors",
            "Add/remove SNES mirrored memory blocks",
            SnesCommon.OPTIONS_NAME,
            SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS,
            false,
            this::getCurrentProgram,
            this::handleMemoryToggle,
            new MenuData(
                new String[] {ToolConstants.MENU_TOOLS, "SNES Memory", "Enable ROM Mirrors"},
                SNES_ICON));
    tool.addAction(displayMirrorsAction);

    displaySramAction =
        new SnesMemoryToggleAction(
            tool,
            getName(),
            "Enable SRAM Bank",
            "Add/remove SNES SRAM block",
            SnesCommon.OPTIONS_NAME,
            SnesCommon.OPT_MEMORY_DISPLAY_SRAM,
            false,
            this::getCurrentProgram,
            this::handleMemoryToggle,
            new MenuData(
                new String[] {ToolConstants.MENU_TOOLS, "SNES Memory", "Enable SRAM Bank"},
                SNES_ICON)) {
          @Override
          public boolean isEnabled() {
            Program program = getCurrentProgramOrNull();
            if (program == null) {
              return false;
            }

            Options opts = getOptions(program);
            int sramSize = opts.getInt(SnesCommon.OPT_CART_SRAM_SIZE, 0);
            return sramSize > 0;
          }
        };

    tool.addAction(displaySramAction);

    displayWramAction =
        new SnesMemoryToggleAction(
            tool,
            getName(),
            "Enable WRAM Banks",
            "Add/remove SNES WRAM memory blocks",
            SnesCommon.OPTIONS_NAME,
            SnesCommon.OPT_MEMORY_DISPLAY_WRAM,
            true,
            this::getCurrentProgram,
            this::handleMemoryToggle,
            new MenuData(
                new String[] {ToolConstants.MENU_TOOLS, "SNES Memory", "Enable WRAM Banks"},
                SNES_ICON));
    tool.addAction(displayWramAction);

    toolbarMemoryAction = new MultiActionDockingAction("SNES Memory", getName());
    toolbarMemoryAction.setDescription("SNES memory visibility controls");
    // toolbarMemoryAction.setMenuBarData(
    //   new MenuData(new String[] { ToolConstants.MENU_TOOLS, "SNES Memory"},
    //   SNES_ICON
    // ));
    toolbarMemoryAction.setToolBarData(new ToolBarData(SNES_ICON, "SNES Memory"));
    toolbarMemoryAction.setActions(
        List.of(displayMmioAction, displayMirrorsAction, displaySramAction, displayWramAction));

    tool.addAction(toolbarMemoryAction);
  }

  /**
   * Handles a memory toggle action after its option has already been persisted.
   *
   * @param optionName option key identifying the memory mapping family
   * @param enabled true to recreate the mapping, false to remove it
   */
  private void handleMemoryToggle(String optionName, Boolean enabled) {
    Program program = getCurrentProgram();
    if (program == null) {
      tool.setStatusInfo("No active program");
      return;
    }

    toggleMemoryMapVisibility(program, optionName, enabled);
  }

  /**
   * Syncs action checked states when Ghidra switches to a different active program.
   */
  @Override
  protected void programActivated(Program program) {
    super.programActivated(program);
    refreshActionsFromOptions(program);
  }

  /**
   * Reads hidden SNES memory options from the active program and updates toggle states.
   *
   * @param program active program
   */
  private void refreshActionsFromOptions(Program program) {
    if (program == null) {
      return;
    }

    Options opts = program.getProgramUserData().getOptions(SnesCommon.OPTIONS_NAME);
    displayMmioAction.setSelected(opts.getBoolean(SnesCommon.OPT_MEMORY_DISPLAY_MMIO, true));
    displayWramAction.setSelected(opts.getBoolean(SnesCommon.OPT_MEMORY_DISPLAY_WRAM, true));
    displaySramAction.setSelected(opts.getBoolean(SnesCommon.OPT_MEMORY_DISPLAY_SRAM, false));
    displayMirrorsAction.setSelected(opts.getBoolean(SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS, false));
  }

  /**
   * Applies a memory mapping toggle inside a program transaction.
   *
   * Ghidra memory blocks do not have a visibility flag, so toggling means removing blocks
   * or recreating them from their known definitions.
   *
   * @param program active program
   * @param optionName option key identifying the mapping family
   * @param enabled true to recreate the mapping, false to remove it
   */
  private void toggleMemoryMapVisibility(Program program, String optionName, boolean enabled) {
    int tx = program.startTransaction("Toggle SNES memory mapping");
    boolean commit = false;

    try {
      int updated = 0;
      if (enabled) {
        updated += recreateMemoryBlocks(program, optionName);
        if (optionName.equals(SnesCommon.OPT_MEMORY_DISPLAY_MMIO)) {
          SnesCommon.createMmioLabels(program);
        }
        if (optionName.equals(SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS)) {
          SnesCommon.createVectorLabels(program);
        }
      } else {
        updated += removeMemoryBlocks(program, optionName);
      }

      commit = true;
      tool.setStatusInfo("SNES memory mapping updated: " + updated + " block(s)");
    } catch (Exception e) {
      tool.setStatusInfo("Failed to update SNES memory mapping: " + e.getMessage());
    } finally {
      program.endTransaction(tx, commit);
    }
  }

  /**
   * Returns whether a memory block belongs to the mapping family controlled by an option.
   *
   * Matching is intentionally namespace-based so user-created blocks are not removed by accident.
   *
   * @param blockName memory block name
   * @param optionName option key identifying the mapping family
   * @return true if the block is managed by the given option
   */
  private boolean matchesVisibilityOption(String blockName, String optionName) {
    return switch (optionName) {
      case SnesCommon.OPT_MEMORY_DISPLAY_MMIO -> blockName.startsWith("snes_mmio_");
      case SnesCommon.OPT_MEMORY_DISPLAY_SRAM -> blockName.equals("snes_sram");
      case SnesCommon.OPT_MEMORY_DISPLAY_WRAM -> blockName.equals("snes_wram");
      case SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS -> blockName.startsWith("snes_rom_mirror_");
      default -> false;
    };
  }

  /**
   * Removes memory blocks managed by a specific SNES mapping toggle.
   *
   * Initialized non-mapped blocks are protected to avoid deleting real ROM banks. Byte-mapped
   * mirrors are allowed to be removed.
   *
   * @param program active program
   * @param optionName option key identifying the mapping family
   * @return number of blocks removed
   */
  private int removeMemoryBlocks(Program program, String optionName) throws Exception {
    Memory memory = program.getMemory();
    int removed = 0;

    for (MemoryBlock block : memory.getBlocks()) {
      // Protective guard:
      // - MMIO / WRAM / SRAM are uninitialized blocks.
      // - ROM mirrors are initialized byte-mapped blocks.
      // - Real ROM banks are initialized non-mapped blocks and must not be removed.
      if (block.isInitialized() && !block.isMapped()) {
        continue;
      }
      if (!matchesVisibilityOption(block.getName(), optionName)) {
        continue;
      }

      memory.removeBlock(block, TaskMonitor.DUMMY);
      removed++;
    }

    return removed;
  }

  /**
   * Recreates memory blocks for the mapping family controlled by an option.
   *
   * @param program active program
   * @param optionName option key identifying the mapping family
   * @return number of blocks created
   */
  private int recreateMemoryBlocks(Program program, String optionName) throws Exception {
    return switch (optionName) {
      case SnesCommon.OPT_MEMORY_DISPLAY_MMIO -> recreateMmioBlocks(program);
      case SnesCommon.OPT_MEMORY_DISPLAY_WRAM -> recreateWramBlocks(program);
      case SnesCommon.OPT_MEMORY_DISPLAY_MIRRORS -> recreateMirrorBlocks(program);
      case SnesCommon.OPT_MEMORY_DISPLAY_SRAM -> recreateSramBlocks(program);
      default -> 0;
    };
  }

  /**
   * Recreates SNES MMIO blocks and ensures MMIO register labels exist.
   *
   * @param program active program
   * @return number of MMIO memory blocks created
   */
  private int recreateMmioBlocks(Program program) throws Exception {
    int created = SnesCommon.createMemoryBlocksMmio(program);
    SnesCommon.createMmioLabels(program);
    return created;
  }

  /**
   * Recreates the main SNES WRAM block.
   *
   * @param program active program
   * @return 1 if the WRAM block was created, 0 if it already existed
   */
  private int recreateWramBlocks(Program program) throws Exception {
    return SnesCommon.createMemoryBlockWram(program);
  }

  /**
   * Recreates the cartridge SRAM block when the loaded ROM declares SRAM.
   *
   * The current implementation maps SRAM using the basic LoROM address range. Mapper-specific
   * SRAM placement can be refined later.
   *
   * @param program active program
   * @return 1 if the SRAM block was created, 0 otherwise
   */
  private int recreateSramBlocks(Program program) throws Exception {
    Options opts = program.getProgramUserData().getOptions(SnesCommon.OPTIONS_NAME);
    int sramSize = opts.getInt(SnesCommon.OPT_CART_SRAM_SIZE, 0);

    if (sramSize <= 0) {
      return 0;
    }

    // Basic mapping: assume LoROM-style SRAM at 0x700000
    // This can be refined later depending on mapper
    return SnesCommon.ensureUninitializedBlock(program, "snes_sram", 0x700000L, sramSize);
  }

  /**
   * Recreates ROM mirror blocks for whichever ROM mapping is present.
   *
   * LoROM and HiROM mirror different source ranges, so the implementation delegates to dedicated
   * mapping-specific helpers.
   *
   * @param program active program
   * @return number of mirror blocks created
   */
  private int recreateMirrorBlocks(Program program) throws Exception {
    return recreateLoRomMirrorBlocks(program) + recreateHiRomMirrorBlocks(program);
  }

  /**
   * Recreates LoROM mirrors in banks {@code 00-7D:8000-FFFF}.
   *
   * The canonical LoROM ROM banks are mapped at {@code 80-FF:8000-FFFF}. Each lower mirror bank
   * maps directly to the corresponding canonical bank.
   *
   * @param program active program
   * @return number of mirror blocks created
   */
  private int recreateLoRomMirrorBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    int created = 0;

    for (int mirrorBank = 0x00; mirrorBank <= 0x7d; mirrorBank++) {
      int sourceBank = mirrorBank + 0x80;
      MemoryBlock sourceBlock = memory.getBlock(String.format("bank_%02x_lorom", sourceBank));
      if (sourceBlock == null) {
        continue;
      }

      created +=
          SnesCommon.ensureMappedBlock(
              program,
              String.format("snes_rom_mirror_%02x_lorom", mirrorBank),
              ((long) mirrorBank << 16) | 0x8000L,
              sourceBlock.getStart(),
              sourceBlock.getSize(),
              "LoROM mirror of " + sourceBlock.getName());
    }

    return created;
  }

  /**
   * Recreates HiROM mirrors in banks {@code 00-3F:8000-FFFF} and {@code 80-BF:8000-FFFF}.
   *
   * The canonical HiROM ROM banks are mapped at {@code C0-FF:0000-FFFF}. The mirror windows expose
   * only the upper half of each canonical bank, so each mirror block maps to source offset
   * {@code +0x8000} and has length {@code 0x8000}.
   *
   * @param program active program
   * @return number of mirror blocks created
   */
  private int recreateHiRomMirrorBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    int created = 0;

    for (int sourceBank = 0xc0; sourceBank <= 0xff; sourceBank++) {
      MemoryBlock sourceBlock = memory.getBlock(String.format("bank_%02x_hirom", sourceBank));
      if (sourceBlock == null) {
        continue;
      }

      int lowMirrorBank = sourceBank - 0xc0;
      int highMirrorBank = sourceBank - 0x40;

      created += createHiRomHalfMirrorBlock(program, sourceBlock, lowMirrorBank);
      created += createHiRomHalfMirrorBlock(program, sourceBlock, highMirrorBank);
    }

    return created;
  }

  /**
   * Recreates one HiROM half-bank mirror.
   *
   * @param program active program
   * @param sourceBlock canonical {@code C0-FF} HiROM source block
   * @param mirrorBank destination mirror bank
   * @return 1 if the mirror block was created, 0 if skipped
   */
  private int createHiRomHalfMirrorBlock(Program program, MemoryBlock sourceBlock, int mirrorBank)
      throws Exception {
    return SnesCommon.ensureMappedBlock(
        program,
        String.format("snes_rom_mirror_%02x_hirom", mirrorBank),
        ((long) mirrorBank << 16) | 0x8000L,
        sourceBlock.getStart().add(0x8000L),
        0x8000L,
        "HiROM mirror of " + sourceBlock.getName() + "+0x8000");
  }
}
