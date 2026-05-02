/* (C) Arnaud 'red' Rouyer 2026 */
package snesplugin;

import docking.ActionContext;
import docking.action.MenuData;
import docking.action.ToggleDockingAction;
import ghidra.framework.options.Options;
import ghidra.framework.plugintool.PluginTool;
import ghidra.program.model.listing.Program;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class SnesMemoryToggleAction extends ToggleDockingAction {

  private final PluginTool tool;
  private final Supplier<Program> programSupplier;
  private final String optionsName;
  private final String optionName;
  private final BiConsumer<String, Boolean> onToggle;

  public SnesMemoryToggleAction(
      PluginTool tool,
      String owner,
      String name,
      String description,
      String optionsName,
      String optionName,
      boolean defaultValue,
      Supplier<Program> programSupplier,
      BiConsumer<String, Boolean> onToggle,
      MenuData menuData) {
    super(name, owner);

    this.tool = tool;
    this.programSupplier = programSupplier;
    this.optionsName = optionsName;
    this.optionName = optionName;
    this.onToggle = onToggle;

    setDescription(description);
    setSelected(defaultValue);
    setMenuBarData(menuData);
  }

  @Override
  public void actionPerformed(ActionContext context) {
    Program program = programSupplier.get();
    if (program == null) {
      tool.setStatusInfo("No active program");
      return;
    }

    boolean enabled = isSelected();
    setBooleanOption(program, enabled);
    onToggle.accept(optionName, enabled);
  }

  protected String getOptionName() {
    return optionName;
  }

  protected Program getCurrentProgramOrNull() {
    return programSupplier.get();
  }

  protected Options getOptions(Program program) {
    return program.getProgramUserData().getOptions(optionsName);
  }

  private void setBooleanOption(Program program, boolean value) {
    int tx = program.getProgramUserData().startTransaction();

    try {
      Options opts = getOptions(program);
      opts.setBoolean(optionName, value);
    } finally {
      program.getProgramUserData().endTransaction(tx);
    }
  }
}
