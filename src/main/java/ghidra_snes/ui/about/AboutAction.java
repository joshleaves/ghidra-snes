/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ui.about;

import docking.action.builder.ActionBuilder;
import ghidra.framework.plugintool.PluginTool;
import ghidra.util.Msg;
import ghidra.util.Swing;
import ghidra_snes.BuildInfo;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import resources.ResourceManager;

public final class AboutAction {
  private static final String PLUGIN_NAME = "Ghidra-SNES";
  private static final String PLUGIN_VERSION = BuildInfo.PLUGIN_VERSION;
  private static final String PLUGIN_GHIDRA_COMPATIBILITY = BuildInfo.PLUGIN_GHIDRA_COMPATIBILITY;
  private static final String PLUGIN_BUILD_DATE = BuildInfo.PLUGIN_BUILD_DATE;
  private static final String PLUGIN_BUILD_COMMIT = BuildInfo.PLUGIN_BUILD_COMMIT;

  private static final Icon SNES_ICON = ResourceManager.loadImage("images/SFC_logo_16.png");
  private static final ImageIcon GHIDRA_SNES_LOGO =
    new ImageIcon(
        ResourceManager.loadImage("images/ghidra-snes.png")
            .getImage()
            .getScaledInstance(280, -1, Image.SCALE_SMOOTH));

  public AboutAction(PluginTool tool, String owner) {
    new ActionBuilder("About Ghidra-SNES", owner)
        .menuPath("Help", "About Ghidra-SNES")
        .menuIcon(SNES_ICON)
        .description("Show Ghidra-SNES version and compatibility information.")
        .onAction(context -> showAboutDialog(tool))
        .buildAndInstall(tool);
  }

  private static void showAboutDialog(PluginTool tool) {
    String dedication =
        "This extension is dedicated to Near (formerly known as byuu), whose "
            + "contributions to SNES documentation, emulation, and preservation "
            + "have had a lasting impact on the community.";

    String license =
        "Released under the MIT License. Permission is hereby granted, "
            + "free of charge, to any person obtaining a copy of this "
            + "software and associated documentation files to deal in "
            + "the Software without restriction.";

    Swing.runLater(
        () -> {
          try {
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
            panel.setPreferredSize(new Dimension(320, 420));


            JLabel logoLabel = new JLabel(GHIDRA_SNES_LOGO);
            logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(logoLabel);

            panel.add(Box.createVerticalStrut(12));

            JLabel versionLabel =
                new JLabel("Plugin Version: " + PLUGIN_VERSION, SwingConstants.CENTER);
            versionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            versionLabel.setFont(versionLabel.getFont().deriveFont(java.awt.Font.BOLD));
            panel.add(versionLabel);

            panel.add(Box.createVerticalStrut(4));

            JLabel compatibilityLabel =
                new JLabel(
                    "Ghidra Compatibility: " + PLUGIN_GHIDRA_COMPATIBILITY, SwingConstants.CENTER);
            compatibilityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            compatibilityLabel.setFont(compatibilityLabel.getFont().deriveFont(java.awt.Font.BOLD));
            panel.add(compatibilityLabel);

            panel.add(Box.createVerticalStrut(4));

            JLabel buildDateLabel =
                new JLabel("Build Date: " + PLUGIN_BUILD_DATE, SwingConstants.CENTER);
            buildDateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            buildDateLabel.setFont(buildDateLabel.getFont().deriveFont(java.awt.Font.BOLD));
            panel.add(buildDateLabel);

            JLabel buildCommitLabel =
                new JLabel("Build commit: " + PLUGIN_BUILD_COMMIT, SwingConstants.CENTER);
            buildCommitLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            buildCommitLabel.setFont(buildCommitLabel.getFont().deriveFont(java.awt.Font.BOLD));
            panel.add(buildCommitLabel);

            panel.add(Box.createVerticalStrut(16));
            panel.add(new JSeparator());
            panel.add(Box.createVerticalStrut(16));

            JTextArea dedicationArea = new JTextArea(dedication);
            dedicationArea.setLineWrap(true);
            dedicationArea.setWrapStyleWord(true);
            dedicationArea.setEditable(false);
            dedicationArea.setOpaque(false);
            dedicationArea.setAlignmentX(Component.CENTER_ALIGNMENT);
            dedicationArea.setMaximumSize(new Dimension(280, 120));
            panel.add(dedicationArea);

            panel.add(Box.createVerticalStrut(16));
            panel.add(new JSeparator());
            panel.add(Box.createVerticalStrut(16));

            JTextArea licenseArea = new JTextArea(license);
            licenseArea.setLineWrap(true);
            licenseArea.setWrapStyleWord(true);
            licenseArea.setEditable(false);
            licenseArea.setOpaque(false);
            licenseArea.setAlignmentX(Component.CENTER_ALIGNMENT);
            licenseArea.setMaximumSize(new Dimension(280, 120));
            panel.add(licenseArea);

            JOptionPane.showMessageDialog(
                tool.getToolFrame(), panel, "About " + PLUGIN_NAME, JOptionPane.PLAIN_MESSAGE);
          } catch (Exception e) {
            Msg.showInfo(AboutAction.class, null, "About " + PLUGIN_NAME, dedication);
          }
        });
  }
}
