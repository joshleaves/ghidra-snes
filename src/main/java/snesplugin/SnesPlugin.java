/* (C) Arnaud 'red' Rouyer 0000 */
package snesplugin;

import docking.ActionContext;
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
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSpace;
import ghidra.program.model.listing.Program;
import ghidra.program.model.listing.ProgramUserData;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.mem.MemoryBlock;
import ghidra.program.model.mem.MemoryConflictException;
import ghidra.util.task.TaskMonitor;
import java.util.List;
import javax.swing.Icon;
import resources.ResourceManager;

@PluginInfo(
    status = PluginStatus.RELEASED,
    packageName = MiscellaneousPluginPackage.NAME,
    category = PluginCategoryNames.COMMON,
    shortDescription = "SNES memory visibility controls",
    description = "Toolbar and menu controls for SNES ROM memory map visibility")
public class SnesPlugin extends ProgramPlugin {

  private static final String OPTIONS_NAME = "SNES ROM";

  private static final String OPT_DISPLAY_MMIO = "memory.display_mmio";
  private static final String OPT_DISPLAY_WRAM = "memory.display_wram";
  private static final String OPT_DISPLAY_MIRRORS = "memory.display_mirrors";

  private static final Icon SNES_ICON = ResourceManager.loadImage("images/Super_Famicom_logo.png");

  private ToggleDockingAction displayMmioAction;
  private ToggleDockingAction displayWramAction;
  private ToggleDockingAction displayMirrorsAction;
  private MultiActionDockingAction toolbarMemoryAction;

  public SnesPlugin(PluginTool tool) {
    super(tool);
    createActions();
  }

  private void createActions() {
    displayMmioAction =
        createToggleAction(
            "Enable MMIO Mappings", "Add/remove SNES MMIO memory blocks", OPT_DISPLAY_MMIO, true);
    displayMirrorsAction =
        createToggleAction(
            "Enable ROM Mirrors",
            "Add/remove SNES mirrored memory blocks",
            OPT_DISPLAY_MIRRORS,
            false);
    displayWramAction =
        createToggleAction(
            "Enable WRAM Banks", "Add/remove SNES WRAM memory blocks", OPT_DISPLAY_WRAM, true);

    toolbarMemoryAction = new MultiActionDockingAction("SNES Memory", getName());
    toolbarMemoryAction.setDescription("SNES memory visibility controls");
    // toolbarMemoryAction.setMenuBarData(
    //   new MenuData(new String[] { ToolConstants.MENU_TOOLS, "SNES Memory"},
    //   SNES_ICON
    // ));
    toolbarMemoryAction.setToolBarData(new ToolBarData(SNES_ICON, "SNES Memory"));
    toolbarMemoryAction.setActions(
        List.of(displayMmioAction, displayMirrorsAction, displayWramAction));

    tool.addAction(toolbarMemoryAction);
  }

  private ToggleDockingAction createToggleAction(
      String name, String description, String optionName, boolean defaultValue) {
    ToggleDockingAction action =
        new ToggleDockingAction(name, getName()) {
          @Override
          public void actionPerformed(ActionContext context) {
            Program program = getCurrentProgram();
            if (program == null) {
              tool.setStatusInfo("No active program");
              return;
            }

            boolean enabled = isSelected();
            setBooleanOption(program, optionName, enabled);
            toggleMemoryMapVisibility(program, optionName, enabled);
          }
        };

    action.setDescription(description);
    action.setEnabled(true);
    action.setSelected(defaultValue);
    action.setMenuBarData(
        new MenuData(new String[] {ToolConstants.MENU_TOOLS, "SNES Memory", name}, SNES_ICON));

    tool.addAction(action);
    return action;
  }

  @Override
  protected void programActivated(Program program) {
    super.programActivated(program);
    refreshActionsFromOptions(program);
  }

  private void refreshActionsFromOptions(Program program) {
    if (program == null) {
      return;
    }

    Options opts = program.getProgramUserData().getOptions(OPTIONS_NAME);
    displayMmioAction.setSelected(opts.getBoolean(OPT_DISPLAY_MMIO, true));
    displayWramAction.setSelected(opts.getBoolean(OPT_DISPLAY_WRAM, true));
    displayMirrorsAction.setSelected(opts.getBoolean(OPT_DISPLAY_MIRRORS, false));
  }

  private void setBooleanOption(Program program, String optionName, boolean value) {
    ProgramUserData userData = program.getProgramUserData();
    int tx = userData.startTransaction();

    try {
      Options opts = userData.getOptions(OPTIONS_NAME);
      opts.setBoolean(optionName, value);
    } finally {
      userData.endTransaction(tx);
    }
  }

  private void toggleMemoryMapVisibility(Program program, String optionName, boolean enabled) {
    int tx = program.startTransaction("Toggle SNES memory mapping");
    boolean commit = false;

    try {
      int updated =
          enabled
              ? recreateMemoryBlocks(program, optionName)
              : removeMemoryBlocks(program, optionName);

      commit = true;
      tool.setStatusInfo("SNES memory mapping updated: " + updated + " block(s)");
    } catch (Exception e) {
      tool.setStatusInfo("Failed to update SNES memory mapping: " + e.getMessage());
    } finally {
      program.endTransaction(tx, commit);
    }
  }

  private boolean matchesVisibilityOption(String blockName, String optionName) {
    return switch (optionName) {
      case OPT_DISPLAY_MMIO -> blockName.startsWith("snes_mmio_");
      case OPT_DISPLAY_WRAM -> blockName.startsWith("snes_wram");
      case OPT_DISPLAY_MIRRORS -> blockName.startsWith("snes_rom_mirror_");
      default -> false;
    };
  }

  private int removeMemoryBlocks(Program program, String optionName) throws Exception {
    Memory memory = program.getMemory();
    int removed = 0;

    for (MemoryBlock block : memory.getBlocks()) {
      /** Protective guard
       * - MMIO: Not initialized
       * - WRAM: Not initialized
       * - Mirrors: Initialized & Mapped
       * The block names are good indicators, but just in case, we don't want to remove
       * a user-named bank that looks like our name filters.
       */
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

  private int recreateMemoryBlocks(Program program, String optionName) throws Exception {
    return switch (optionName) {
      case OPT_DISPLAY_MMIO -> recreateMmioBlocks(program);
      case OPT_DISPLAY_WRAM -> recreateWramBlocks(program);
      case OPT_DISPLAY_MIRRORS -> recreateMirrorBlocks(program);
      default -> 0;
    };
  }

  private int recreateMirrorBlocks(Program program) throws Exception {
    int created = 0;
    created += recreateLoRomMirrorBlocks(program);
    created += recreateHiRomMirrorBlocks(program);
    return created;
  }

  private int recreateLoRomMirrorBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    int created = 0;

    for (int bank = 0x00; bank <= 0x3f; bank++) {
      MemoryBlock sourceBlock = memory.getBlock(String.format("bank_%02x_lorom", bank));
      if (sourceBlock == null) {
        continue;
      }

      int mirrorBank = bank + 0x80;
      String mirrorName = String.format("snes_rom_mirror_%02x_lorom", mirrorBank);
      long mirrorAddress = ((long) mirrorBank << 16) | 0x8000L;

      created +=
          createByteMappedBlockIfMissing(
              memory,
              space,
              mirrorName,
              mirrorAddress,
              sourceBlock.getStart(),
              sourceBlock.getSize(),
              "LoROM mirror of " + sourceBlock.getName());
    }

    return created;
  }

  private int recreateHiRomMirrorBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    int created = 0;

    for (int bank = 0x40; bank <= 0x7f; bank++) {
      MemoryBlock sourceBlock = memory.getBlock(String.format("bank_%02x_hirom", bank));
      if (sourceBlock == null) {
        continue;
      }

      int mirrorBank = bank + 0x80;
      String mirrorName = String.format("snes_rom_mirror_%02x_hirom", mirrorBank);
      long mirrorAddress = (long) mirrorBank << 16;

      created +=
          createByteMappedBlockIfMissing(
              memory,
              space,
              mirrorName,
              mirrorAddress,
              sourceBlock.getStart(),
              sourceBlock.getSize(),
              "HiROM mirror of " + sourceBlock.getName());
    }

    return created;
  }

  private int createByteMappedBlockIfMissing(
      Memory memory,
      AddressSpace space,
      String name,
      long start,
      Address mappedAddress,
      long size,
      String comment)
      throws Exception {
    if (memory.getBlock(name) != null) {
      return 0;
    }

    if (size <= 0 || size > Integer.MAX_VALUE) {
      return 0;
    }

    try {
      MemoryBlock block =
          memory.createByteMappedBlock(name, space.getAddress(start), mappedAddress, size, false);
      block.setComment(comment);
      block.setRead(true);
      block.setWrite(false);
      block.setExecute(true);

      return 1;
    } catch (MemoryConflictException e) {
      return 0;
    }
  }

  private int recreateMmioBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();
    int created = 0;

    created +=
        createUninitializedBlockIfMissing(
            memory, space, "snes_mmio_ppu", 0x002100, 0x40, true, true, false);
    created +=
        createUninitializedBlockIfMissing(
            memory, space, "snes_mmio_apu", 0x002140, 0x40, true, true, false);
    created +=
        createUninitializedBlockIfMissing(
            memory, space, "snes_mmio_wram", 0x002180, 0x04, true, true, false);
    created +=
        createUninitializedBlockIfMissing(
            memory, space, "snes_mmio_cpu", 0x004200, 0x20, true, true, false);
    created +=
        createUninitializedBlockIfMissing(
            memory, space, "snes_mmio_dma", 0x004300, 0x80, true, true, false);

    return created;
  }

  private int recreateWramBlocks(Program program) throws Exception {
    Memory memory = program.getMemory();
    AddressSpace space = program.getAddressFactory().getDefaultAddressSpace();

    return createUninitializedBlockIfMissing(
        memory, space, "snes_wram", 0x7e0000, 0x20000, true, true, false);
  }

  private int createUninitializedBlockIfMissing(
      Memory memory,
      AddressSpace space,
      String name,
      long start,
      long size,
      boolean read,
      boolean write,
      boolean execute)
      throws Exception {
    if (memory.getBlock(name) != null) {
      return 0;
    }

    MemoryBlock block = memory.createUninitializedBlock(name, space.getAddress(start), size, false);
    block.setRead(read);
    block.setWrite(write);
    block.setExecute(execute);
    return 1;
  }
}
