/* (C) Arnaud 'red' Rouyer 2026 */
// package ghidra_snes;

// import docking.action.builder.ActionBuilder;
// import generic.theme.GIcon;
// import ghidra.app.plugin.PluginCategoryNames;
// import ghidra.framework.plugintool.PluginInfo;
// import ghidra.framework.plugintool.PluginTool;
// import ghidra.framework.plugintool.util.PluginStatus;
// import ghidra.program.model.listing.Program;
// import ghidra.app.plugin.ProgramPlugin;
// import ghidra_snes.ui.SnesCartridgeProvider;

// @PluginInfo(
// 	status = PluginStatus.UNSTABLE,
// 	packageName = "SNES",
// 	category = PluginCategoryNames.ANALYSIS,
// 	shortDescription = "SNES cartridge tools",
// 	description = "SNES cartridge mapping, board detection, mirrors, and register symbols."
// )
// public final class SnesPlugin extends ProgramPlugin {

// 	private SnesCartridgeProvider provider;

// 	public SnesPlugin(PluginTool tool) {
// 		super(tool);
// 	}

// 	@Override
// 	protected void init() {
// 		super.init();

// 		provider = new SnesCartridgeProvider(tool, getCurrentProgram());
// 		tool.addComponentProvider(provider, false);

// 		new ActionBuilder("Show SNES Cartridge", getName())
// 			.menuPath("Tools", "SNES Cartridge...")
// 			.menuIcon(new GIcon("icon.plugin"))
// 			.description("Show SNES cartridge mapping tools.")
// 			.onAction(ctx -> provider.setVisible(true))
// 			.buildAndInstall(tool);
// 	}

// 	@Override
// 	protected void programActivated(Program program) {
// 		super.programActivated(program);
// 		if (provider != null) {
// 			provider.setProgram(program);
// 		}
// 	}

// 	@Override
// 	protected void programDeactivated(Program program) {
// 		super.programDeactivated(program);
// 		if (provider != null && provider.getProgram() == program) {
// 			provider.setProgram(null);
// 		}
// 	}

// 	@Override
// 	protected void dispose() {
// 		if (provider != null) {
// 			tool.removeComponentProvider(provider);
// 			provider = null;
// 		}
// 		super.dispose();
// 	}
// }
