/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ui.options;

import docking.DockingWindowManager;
import docking.action.builder.ActionBuilder;
import docking.tool.ToolConstants;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Program;
import java.util.function.Supplier;
import javax.swing.Icon;
import resources.ResourceManager;

public final class SnesOptionsAction {
  private static final String ACTION_NAME = "SNES Options";
  private static final Icon SNES_ICON = ResourceManager.loadImage("images/SFC_logo_16.png");

  private final PluginTool tool;
  private final Supplier<Program> currentProgramSupplier;

  public SnesOptionsAction(PluginTool tool, String owner, Supplier<Program> currentProgramSupplier) {
    this.tool = tool;
    this.currentProgramSupplier = currentProgramSupplier;

    new ActionBuilder(ACTION_NAME, owner)
      .menuPath(ToolConstants.MENU_TOOLS, ACTION_NAME)
      .menuIcon(SNES_ICON)
      .toolBarIcon(SNES_ICON)
      .toolBarGroup(owner)
      .description("Open the SNES options window.")
      .onAction(context -> showOptionsDialog())
      .buildAndInstall(tool);
  }

  private void showOptionsDialog() {
    DockingWindowManager.showDialog(tool.getToolFrame(), new SnesOptionsDialog(currentProgramSupplier.get()));
  }
}
