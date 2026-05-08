/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes;

import ghidra.MiscellaneousPluginPackage;
import ghidra.app.plugin.PluginCategoryNames;
import ghidra.app.plugin.ProgramPlugin;
import ghidra.framework.plugintool.PluginInfo;
import ghidra.framework.plugintool.PluginTool;
import ghidra.framework.plugintool.util.PluginStatus;
import ghidra_snes.ui.about.AboutAction;

@PluginInfo(
  status = PluginStatus.RELEASED,
  packageName = MiscellaneousPluginPackage.NAME,
  category = PluginCategoryNames.COMMON,
  shortDescription = "SNES ROM loader helpers",
  description = "SNES ROM loader helpers and UI actions.")
public class SnesPlugin extends ProgramPlugin {
  public SnesPlugin(PluginTool tool) {
    super(tool);

    new AboutAction(tool, getName());
  }
}
