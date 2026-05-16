/* (C) Arnaud 'red' Rouyer 2026 */
package ghidra_snes.ui.options;

import docking.DialogComponentProvider;
import docking.widgets.tree.GTree;
import docking.widgets.tree.GTreeNode;
import generic.theme.GIcon;
import ghidra.program.model.listing.Program;
import ghidra_snes.options.SnesOptions;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import resources.Icons;

public final class SnesOptionsDialog extends DialogComponentProvider {
  private static final String ROOT_PAGE = "/";
  private static final String MEMORY_PAGE = "/memory/";
  private static final String SYSTEM_BANKS_PAGE = "/memory/system_banks";
  private static final String ROM_MIRRORS_PAGE = "/memory/rom_mirrors";

  private static final Icon OPEN_FOLDER_ICON = Icons.OPEN_FOLDER_ICON;
  private static final Icon CLOSED_FOLDER_ICON = Icons.CLOSED_FOLDER_ICON;
  private static final Icon PROPERTIES_ICON = new GIcon("icon.properties");

  private final Program currentProgram;
  private final CardLayout pageLayout = new CardLayout();
  private final JPanel pagePanel = new JPanel(pageLayout);
  private final Map<GTreeNode, String> nodeToPage = new HashMap<>();

  public SnesOptionsDialog(Program currentProgram) {
    super("SNES Options", true);
    this.currentProgram = currentProgram;

    addWorkPanel(buildMainPanel());
    addDismissButton();
    setPreferredSize(980, 680);
  }

  private JComponent buildMainPanel() {
    GTree tree = buildTree();
    pagePanel.add(buildRootPage(), ROOT_PAGE);
    pagePanel.add(buildEmptyPage("Memory Map"), MEMORY_PAGE);
    pagePanel.add(buildEmptyPage("System Banks (scaffold only)"), SYSTEM_BANKS_PAGE);
    pagePanel.add(buildEmptyPage("ROM Mirrors (scaffold only)"), ROM_MIRRORS_PAGE);

    JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tree, pagePanel);
    splitPane.setResizeWeight(0.0);
    splitPane.setDividerLocation(280);
    splitPane.setDividerSize(6);
    splitPane.setOneTouchExpandable(true);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(splitPane, BorderLayout.CENTER);
    return panel;
  }

  private GTree buildTree() {
    SnesOptionsTreeNode rootNode = SnesOptionsTreeNode.folder("SNES Options", ROOT_PAGE);
    SnesOptionsTreeNode romDetailsNode = SnesOptionsTreeNode.leaf("ROM Details", ROOT_PAGE);
    SnesOptionsTreeNode memoryMapNode = SnesOptionsTreeNode.folder("Memory Map", MEMORY_PAGE);
    SnesOptionsTreeNode systemBanksNode =
      SnesOptionsTreeNode.leaf("System Banks", SYSTEM_BANKS_PAGE);
    SnesOptionsTreeNode romMirrorsNode =
      SnesOptionsTreeNode.leaf("ROM Mirrors", ROM_MIRRORS_PAGE);

    rootNode.addNode(romDetailsNode);
    rootNode.addNode(memoryMapNode);
    memoryMapNode.addNode(systemBanksNode);
    memoryMapNode.addNode(romMirrorsNode);

    registerPage(romDetailsNode);
    registerPage(memoryMapNode);
    registerPage(systemBanksNode);
    registerPage(romMirrorsNode);

    GTree tree = new GTree(rootNode);
    tree.setRootVisible(false);
    tree.setShowsRootHandles(true);
    tree.setRootNodeAllowedToCollapse(false);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.expandPath(rootNode);
    tree.expandPath(memoryMapNode);
    tree.addGTreeSelectionListener(event -> showSelectedPage(tree));
    tree.setSelectedNode(romDetailsNode);
    return tree;
  }

  private void registerPage(SnesOptionsTreeNode node) {
    if (node.getPageKey() != null) {
      nodeToPage.put(node, node.getPageKey());
    }
  }

  private void showSelectedPage(GTree tree) {
    TreePath selectionPath = tree.getSelectionPath();
    if (selectionPath == null) {
      pageLayout.show(pagePanel, ROOT_PAGE);
      return;
    }

    Object selectedNode = selectionPath.getLastPathComponent();
    if (!(selectedNode instanceof GTreeNode)) {
      pageLayout.show(pagePanel, ROOT_PAGE);
      return;
    }

    String page = nodeToPage.getOrDefault((GTreeNode) selectedNode, ROOT_PAGE);
    pageLayout.show(pagePanel, page);
  }

  private JComponent buildEmptyPage(String title) {
    JPanel page = new JPanel(new BorderLayout());
    page.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    JLabel label = new JLabel(title, SwingConstants.CENTER);
    label.setPreferredSize(new Dimension(300, 60));
    page.add(label, BorderLayout.CENTER);
    return page;
  }

  private JComponent buildRootPage() {
    JPanel page = new JPanel(new BorderLayout());
    page.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

    if (currentProgram == null) {
      page.add(new JLabel("No open program.", SwingConstants.CENTER), BorderLayout.CENTER);
      return page;
    }

    JPanel content = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.gridwidth = 2;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = new Insets(0, 0, 16, 0);

    JLabel titleLabel = new JLabel("ROM Details");
    titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
    content.add(titleLabel, constraints);

    constraints.gridy = 1;
    content.add(new JSeparator(), constraints);

    int row = 2;
    addInfoRow(content, row++, "Program", currentProgram.getName());
    addInfoRow(content, row++, "Executable path", normalized(currentProgram.getExecutablePath()));
    addInfoRow(content, row++, "Mapper", SnesOptions.getCartMapper(currentProgram));
    addInfoRow(content, row++, "ROM title", normalized(SnesOptions.getCartTitle(currentProgram)));
    addInfoRow(content, row++, "ROM size", formatBytes(SnesOptions.getRomSizeBytes(currentProgram)));
    addInfoRow(content, row++, "SRAM size", formatBytes(SnesOptions.getCartSramSize(currentProgram)));
    addInfoRow(content, row++, "SMC header", String.valueOf(SnesOptions.hasSmcHeader(currentProgram)));
    addInfoRow(content, row++, "ROM offset", formatHex(SnesOptions.getFileRomOffset(currentProgram)));
    addInfoRow(content, row++, "Header location", formatHex(SnesOptions.getRomHeader(currentProgram).location()));
    addInfoRow(content, row, "Metadata source", SnesOptions.getMetadataSource(currentProgram));

    JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(content, BorderLayout.NORTH);
    page.add(wrapper, BorderLayout.CENTER);
    return page;
  }

  private void addInfoRow(JPanel content, int row, String key, String value) {
    GridBagConstraints keyConstraints = new GridBagConstraints();
    keyConstraints.gridx = 0;
    keyConstraints.gridy = row;
    keyConstraints.anchor = GridBagConstraints.NORTHWEST;
    keyConstraints.insets = new Insets(6, 0, 6, 18);

    JLabel keyLabel = new JLabel(key);
    keyLabel.setFont(keyLabel.getFont().deriveFont(Font.BOLD));
    content.add(keyLabel, keyConstraints);

    GridBagConstraints valueConstraints = new GridBagConstraints();
    valueConstraints.gridx = 1;
    valueConstraints.gridy = row;
    valueConstraints.weightx = 1.0;
    valueConstraints.fill = GridBagConstraints.HORIZONTAL;
    valueConstraints.anchor = GridBagConstraints.NORTHWEST;
    valueConstraints.insets = new Insets(6, 0, 6, 0);

    content.add(new JLabel(value), valueConstraints);
  }

  private static String normalized(String value) {
    if (value == null || value.isBlank()) {
      return "(not set)";
    }
    return value;
  }

  private static String formatHex(long value) {
    return String.format("0x%X", value);
  }

  private static String formatBytes(long value) {
    return String.format("%d bytes (%s)", value, formatHex(value));
  }

  private static final class SnesOptionsTreeNode extends GTreeNode {
    private final String name;
    private final String pageKey;
    private final boolean leaf;

    private SnesOptionsTreeNode(String name, String pageKey, boolean leaf) {
      this.name = name;
      this.pageKey = pageKey;
      this.leaf = leaf;
    }

    static SnesOptionsTreeNode folder(String name, String pageKey) {
      return new SnesOptionsTreeNode(name, pageKey, false);
    }

    static SnesOptionsTreeNode leaf(String name, String pageKey) {
      return new SnesOptionsTreeNode(name, pageKey, true);
    }

    String getPageKey() {
      return pageKey;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Icon getIcon(boolean isExpanded) {
      if (leaf) {
        return PROPERTIES_ICON;
      }
      return isExpanded ? OPEN_FOLDER_ICON : CLOSED_FOLDER_ICON;
    }

    @Override
    public String getToolTip() {
      return pageKey;
    }

    @Override
    public boolean isLeaf() {
      return leaf;
    }

    @Override
    public int compareTo(GTreeNode other) {
      return name.compareTo(other.getName());
    }
  }
}
