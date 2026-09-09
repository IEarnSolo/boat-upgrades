package com.boatupgrades;

import com.boatupgrades.utils.SchematicUtils;
import com.boatupgrades.utils.UpgradeVisibilityUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.IconTextField;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class BoatUpgradesPanel extends PluginPanel
{
    private static final String AVAILABLE = "AVAILABLE", LISTS = "LISTS";
    private static final String SELECTED = "SELECTED", CATALOG = "CATALOG";
    private static final String ANY = "Any", AVAILABLE_ONLY = "Available only", ANY_TYPE = "Any type", ANY_BOAT = "Any boat";
    private final CardLayout cards = new CardLayout(), listCards = new CardLayout();
    public final JPanel cardContainer = new JPanel(cards);
    public JPanel availableUpgradesContainer;
    private final JPanel listContainer = new JPanel(listCards), selectedContainer = vertical(), catalogContainer = vertical();
    private final Map<String, ImageIcon> imageCache = new HashMap<>();
    private final Map<UpgradeData.UpgradeOption, Boolean> schematicMap = new HashMap<>();
    private final Map<String, JButton> catalogDecreaseButtons = new HashMap<>();
    private final Map<String, JButton> catalogActionButtons = new HashMap<>();
    private final Map<String, JLabel> catalogQuantityLabels = new HashMap<>();
    private final BoatUpgradesConfig config;
    private final MyUpgradeListService lists;
    private final AvailableUpgradesService availableService;
    private final SchematicUtils schematicUtils;
    private final UpgradeVisibilityUtils visibilityUtils;
    private final MaterialOwnershipService materialOwnershipService;
    private final BankMaterialViewService bankMaterialViewService;
    private String activeTab = AVAILABLE, activeListMode = SELECTED;
    private JLabel availableTab, listsTab;
    private JButton selectedButton, catalogButton;
    private IconTextField search;
    private JComboBox<String> availability, boat, type;
    private JComboBox<MyUpgradeListService.ListSummary> listSelector;
    private JButton listMenuButton;
    private JPopupMenu listMenuPopup;
    private long listMenuClosedAt;
    private JPanel catalogListNotice;
    private boolean updatingListSelector;
    private JScrollPane catalogScrollPane;
    private JScrollPane selectedScrollPane;
    private boolean catalogLoaded;

    @Inject
    public BoatUpgradesPanel(Client client, ItemManager itemManager, BoatUpgradesConfig config,
        MyUpgradeListService lists, AvailableUpgradesService availableService,
        SchematicUtils schematicUtils, UpgradeVisibilityUtils visibilityUtils,
        MaterialOwnershipService materialOwnershipService, BankMaterialViewService bankMaterialViewService)
    {
        super(false);
        this.config = config;
        this.lists = lists;
        this.availableService = availableService;
        this.schematicUtils = schematicUtils;
        this.visibilityUtils = visibilityUtils;
        this.materialOwnershipService = materialOwnershipService;
        this.bankMaterialViewService = bankMaterialViewService;
        log.debug("[Panel] Building panel and Lists planner");
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCards(), BorderLayout.CENTER);
    }

    public void onAvailableUpgradesChanged()
    {
        List<UpgradeData.UpgradeOption> options = availableService.get();
        log.debug("[Panel] Refreshing {} published available upgrades", options.size());
        schematicMap.clear();
        options.forEach(o -> schematicMap.put(o, schematicUtils.hasCachedSchematic(o.displayName)));
        List<UpgradeData.UpgradeOption> visible = options.stream().filter(visibilityUtils::shouldShowUpgrade).collect(Collectors.toList());
        SwingUtilities.invokeLater(() ->
        {
            updateAvailableUpgrades(visible);
            if (catalogLoaded && AVAILABLE_ONLY.equals(availability.getSelectedItem()))
            {
                log.debug("[Lists] Available-only catalog data changed; refreshing catalog");
                refreshCatalog();
            }
        });
    }

    public void onListsReloaded()
    {
        SwingUtilities.invokeLater(() ->
        {
            log.debug("[Lists] Refreshing planner after saved selections were reloaded");
            updateListSelector();
            refreshSelected();
            updateCatalogActionButtons();
        });
    }

    private JPanel buildHeader()
    {
        JPanel header = vertical();
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel title = new JPanel();
        title.setLayout(new BoxLayout(title, BoxLayout.X_AXIS));
        title.setOpaque(false);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel icon = new JLabel(new ImageIcon(ImageUtil.loadImageResource(getClass(), "icon.png")));
        icon.setBorder(new EmptyBorder(0, 0, 0, 6));
        JLabel text = new JLabel("Boat Upgrades");
        text.setFont(FontManager.getRunescapeBoldFont());
        text.setForeground(Color.WHITE);
        title.add(icon); title.add(text);
        header.add(title); header.add(Box.createVerticalStrut(8)); header.add(buildTabs());
        return header;
    }

    private JPanel buildTabs()
    {
        JPanel bar = new JPanel(new GridLayout(1, 2));
        bar.setBackground(ColorScheme.DARK_GRAY_COLOR);
        availableTab = tab("Available upgrades", AVAILABLE);
        listsTab = tab("Lists", LISTS);
        bar.add(availableTab); bar.add(listsTab); styleTabs();
        return bar;
    }

    private JLabel tab(String text, String key)
    {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(FontManager.getRunescapeFont()); label.setForeground(Color.WHITE); label.setOpaque(true);
        label.setBackground(ColorScheme.DARK_GRAY_COLOR); label.setBorder(new EmptyBorder(6, 4, 6, 4));
        label.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e) { activeTab = key; cards.show(cardContainer, key); styleTabs(); log.debug("[Panel] Switched tab to {} without rebuilding its contents", key); }
            public void mouseEntered(MouseEvent e) { if (!activeTab.equals(key)) label.setForeground(ColorScheme.BRAND_ORANGE); }
            public void mouseExited(MouseEvent e) { styleTabs(); }
        });
        return label;
    }

    private void styleTabs()
    {
        if (availableTab == null) return;
        styleTab(availableTab, AVAILABLE); styleTab(listsTab, LISTS);
    }

    private void styleTab(JLabel label, String key)
    {
        label.setForeground(Color.WHITE);
        label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, activeTab.equals(key) ? ColorScheme.BRAND_ORANGE : ColorScheme.DARK_GRAY_COLOR));
    }

    private JPanel buildCards()
    {
        cardContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        availableUpgradesContainer = vertical(); availableUpgradesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        updateAvailableUpgrades(availableService.get());
        cardContainer.add(scroll(availableUpgradesContainer), AVAILABLE);
        cardContainer.add(buildLists(), LISTS);
        cards.show(cardContainer, AVAILABLE);
        return cardContainer;
    }

    private JPanel buildLists()
    {
        JPanel panel = new JPanel(new BorderLayout()); panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JPanel top = vertical(); top.setBackground(ColorScheme.DARKER_GRAY_COLOR); top.add(buildListManager());
        JPanel modes = new JPanel(new GridLayout(1, 2, 4, 0)); modes.setBackground(ColorScheme.DARKER_GRAY_COLOR); modes.setBorder(new EmptyBorder(8, 10, 6, 10));
        selectedButton = modeButton("My upgrades", SELECTED); catalogButton = modeButton("Add upgrades", CATALOG);
        modes.add(selectedButton); modes.add(catalogButton); top.add(modes); panel.add(top, BorderLayout.NORTH);
        selectedContainer.setBorder(new EmptyBorder(10, 10, 10, 10)); catalogContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        listContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        selectedScrollPane = createScrollPane(selectedContainer);
        listContainer.add(wrapScrollPane(selectedScrollPane), SELECTED);
        JPanel catalog = new JPanel(new BorderLayout()); catalog.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        catalog.add(buildFilters(), BorderLayout.NORTH);
        catalogScrollPane = createScrollPane(catalogContainer);
        catalog.add(wrapScrollPane(catalogScrollPane), BorderLayout.CENTER);
        listContainer.add(catalog, CATALOG); panel.add(listContainer, BorderLayout.CENTER);
        styleModes(); updateListSelector(); refreshSelected(); refreshCatalog(); return panel;
    }

    private JPanel buildListManager()
    {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(8, 10, 0, 10));
        listSelector = new JComboBox<>();
        listSelector.setToolTipText("Active upgrade list");
        listSelector.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        listSelector.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focused)
            {
                Component component = super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value == null) setText("No lists");
                else if (value instanceof MyUpgradeListService.ListSummary)
                {
                    MyUpgradeListService.ListSummary summary = (MyUpgradeListService.ListSummary) value;
                    setText(summary.getName() + " (" + summary.getUpgradeCount() + ")");
                }
                return component;
            }
        });
        listSelector.addActionListener(e ->
        {
            if (updatingListSelector) return;
            MyUpgradeListService.ListSummary selected = (MyUpgradeListService.ListSummary) listSelector.getSelectedItem();
            if (selected != null && lists.setActiveList(selected.getId()))
            {
                log.debug("[Lists] Switched active list from selector to {}", selected.getId());
                refreshSelected();
                updateCatalogActionButtons();
            }
        });
        listMenuButton = new JButton("⋮");
        listMenuButton.setToolTipText("Manage upgrade lists");
        listMenuButton.setPreferredSize(new Dimension(34, 26));
        listMenuButton.setFocusPainted(false);
        listMenuButton.setBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR));
        listMenuButton.addActionListener(e -> toggleListMenu());
        log.debug("[Lists] Applied visible borders to the active-list selector and management button");
        row.add(listSelector, BorderLayout.CENTER);
        row.add(listMenuButton, BorderLayout.EAST);
        return row;
    }

    private void toggleListMenu()
    {
        if (listMenuPopup != null && listMenuPopup.isVisible())
        {
            log.debug("[Lists] Closing list management menu from toggle button");
            listMenuPopup.setVisible(false);
            return;
        }

        if (System.currentTimeMillis() - listMenuClosedAt < 250)
        {
            log.debug("[Lists] Suppressed list management menu reopen after popup dismissal click");
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        listMenuPopup = menu;
        menu.addPopupMenuListener(new javax.swing.event.PopupMenuListener()
        {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent event) {}

            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent event)
            {
                listMenuClosedAt = System.currentTimeMillis();
                log.debug("[Lists] List management menu became hidden");
            }

            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent event)
            {
                listMenuClosedAt = System.currentTimeMillis();
                log.debug("[Lists] List management menu was cancelled");
            }
        });
        JMenuItem create = new JMenuItem("New list"), rename = new JMenuItem("Rename list"), delete = new JMenuItem("Delete list");
        JMenuItem export = new JMenuItem("Export list"), importList = new JMenuItem("Import list");
        create.addActionListener(e -> promptCreateList());
        rename.addActionListener(e -> promptRenameList());
        delete.addActionListener(e -> confirmDeleteList());
        export.addActionListener(e -> exportActiveList());
        importList.addActionListener(e -> importListFromClipboard());
        boolean hasList = lists.hasActiveList();
        rename.setEnabled(hasList); delete.setEnabled(hasList); export.setEnabled(hasList);
        menu.add(create); menu.add(rename); menu.addSeparator();
        menu.add(importList); menu.add(export); menu.addSeparator(); menu.add(delete);
        log.debug("[Lists] Opened list management menu; active list present={}", hasList);
        menu.show(listMenuButton, 0, listMenuButton.getHeight());
    }

    private void exportActiveList()
    {
        try
        {
            MyUpgradeListService.ListSummary active = lists.getActiveList();
            if (active == null) return;
            String exported = lists.exportActiveList();
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(exported), null);
            log.debug("[Lists] Copied exported list {} to the system clipboard", active.getId());
            JOptionPane.showMessageDialog(this,
                "'" + active.getName() + "' was copied to your clipboard.",
                "Upgrade list exported", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (Exception ex)
        {
            log.warn("[Lists] Failed to export the active list to the clipboard", ex);
            JOptionPane.showMessageDialog(this,
                "The upgrade list could not be copied to your clipboard.",
                "Export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importListFromClipboard()
    {
        try
        {
            Object clipboardValue = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
            String clipboardText = clipboardValue == null ? null : clipboardValue.toString();
            MyUpgradeListService.ListSummary imported = lists.importList(clipboardText);
            refreshAfterListChange();
            log.debug("[Lists] Imported list {} from the system clipboard", imported.getId());
            JOptionPane.showMessageDialog(this,
                "Imported '" + imported.getName() + "' with " + imported.getUpgradeCount()
                    + (imported.getUpgradeCount() == 1 ? " upgrade." : " upgrades."),
                "Upgrade list imported", JOptionPane.INFORMATION_MESSAGE);
        }
        catch (IllegalArgumentException ex)
        {
            log.debug("[Lists] Rejected clipboard list import: {}", ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid upgrade list", JOptionPane.ERROR_MESSAGE);
        }
        catch (Exception ex)
        {
            log.warn("[Lists] Failed to read an upgrade list from the clipboard", ex);
            JOptionPane.showMessageDialog(this,
                "The clipboard could not be read or does not contain text.",
                "Import failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void promptCreateList()
    {
        String name = promptForListName("Create upgrade list", "Enter a name for your new list:", "", false);
        if (name == null) return;
        lists.createList(name);
        log.debug("[Lists] Created list from panel dialog");
        refreshAfterListChange();
    }

    private void promptRenameList()
    {
        MyUpgradeListService.ListSummary active = lists.getActiveList();
        if (active == null) return;
        String name = promptForListName("Rename upgrade list", "Enter a new name for this list:", active.getName(), true);
        if (name == null) return;
        lists.renameActiveList(name);
        log.debug("[Lists] Renamed active list from panel dialog");
        refreshAfterListChange();
    }

    private String promptForListName(String title, String message, String initialValue, boolean renaming)
    {
        String value = initialValue;
        while (true)
        {
            Object result = JOptionPane.showInputDialog(this, message, title, JOptionPane.PLAIN_MESSAGE, null, null, value);
            if (result == null) { log.debug("[Lists] User cancelled '{}' dialog", title); return null; }
            try
            {
                String name = result.toString().trim();
                validateListNameForDialog(name, renaming);
                return name;
            }
            catch (IllegalArgumentException ex)
            {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Invalid list name", JOptionPane.ERROR_MESSAGE);
                value = result.toString();
                log.debug("[Lists] Rejected list name: {}", ex.getMessage());
            }
        }
    }

    private void validateListNameForDialog(String name, boolean renaming)
    {
        if (name.isEmpty()) throw new IllegalArgumentException("List name cannot be empty.");
        if (name.length() > 40) throw new IllegalArgumentException("List name cannot exceed 40 characters.");
        MyUpgradeListService.ListSummary active = lists.getActiveList();
        for (MyUpgradeListService.ListSummary list : lists.getLists())
        {
            if (renaming && active != null && list.getId().equals(active.getId())) continue;
            if (list.getName().equalsIgnoreCase(name)) throw new IllegalArgumentException("A list with that name already exists.");
        }
    }

    private void confirmDeleteList()
    {
        MyUpgradeListService.ListSummary active = lists.getActiveList();
        if (active == null) return;
        String contents = active.getUpgradeCount() + (active.getUpgradeCount() == 1 ? " upgrade" : " upgrades");
        if (active.getUpgradeCount() != active.getDistinctUpgradeCount())
            contents += " across " + active.getDistinctUpgradeCount() + (active.getDistinctUpgradeCount() == 1 ? " type" : " types");
        String message = "Delete list '" + active.getName() + "' and its " + contents + "?\n\nThis cannot be undone.";
        int result = JOptionPane.showConfirmDialog(this, message, "Delete upgrade list", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) { log.debug("[Lists] User cancelled deletion of list {}", active.getId()); return; }
        lists.deleteActiveList();
        log.debug("[Lists] User confirmed deletion of list {}", active.getId());
        refreshAfterListChange();
    }

    private void refreshAfterListChange()
    {
        updateListSelector(); refreshSelected(); updateCatalogActionButtons();
    }

    private void updateListSelector()
    {
        if (listSelector == null) return;
        updatingListSelector = true;
        listSelector.removeAllItems();
        String activeId = lists.getActiveListId();
        MyUpgradeListService.ListSummary active = null;
        for (MyUpgradeListService.ListSummary list : lists.getLists())
        {
            listSelector.addItem(list);
            if (list.getId().equals(activeId)) active = list;
        }
        listSelector.setSelectedItem(active);
        listSelector.setEnabled(active != null);
        listSelector.setToolTipText(active == null ? "No lists - use the menu to create one" : "Active upgrade list");
        if (catalogListNotice != null) catalogListNotice.setVisible(active == null);
        updatingListSelector = false;
        log.debug("[Lists] Updated list selector with {} lists; active={}", listSelector.getItemCount(), activeId);
    }

    private JButton modeButton(String text, String mode)
    {
        JButton button = new JButton(text); button.setFont(FontManager.getRunescapeSmallFont()); button.setFocusPainted(false);
        button.addActionListener(e -> showMode(mode)); return button;
    }

    private void showMode(String mode)
    {
        activeListMode = mode; listCards.show(listContainer, mode); styleModes();
        log.debug("[Lists] Switched planner mode to {} without rebuilding the catalog", mode);
    }

    private void styleModes()
    {
        if (selectedButton == null) return;
        styleMode(selectedButton, SELECTED); styleMode(catalogButton, CATALOG);
    }

    private void styleMode(JButton button, String mode)
    {
        boolean active = activeListMode.equals(mode);
        button.setBackground(active ? ColorScheme.MEDIUM_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
        button.setForeground(active ? ColorScheme.TEXT_COLOR : Color.LIGHT_GRAY);
        button.setBorder(BorderFactory.createLineBorder(active ? ColorScheme.TEXT_COLOR : ColorScheme.MEDIUM_GRAY_COLOR));
    }

    private JPanel buildFilters()
    {
        JPanel filters = vertical(); filters.setBorder(new EmptyBorder(4, 10, 6, 10));
        search = new IconTextField();
        search.setIcon(IconTextField.Icon.SEARCH);
        search.setToolTipText("Search upgrades, types, or materials");
        search.setPreferredSize(new Dimension(100, 30));
        search.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        search.setHoverBackgroundColor(ColorScheme.DARK_GRAY_HOVER_COLOR);
        search.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        search.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { refreshCatalog(); } public void removeUpdate(DocumentEvent e) { refreshCatalog(); } public void changedUpdate(DocumentEvent e) { refreshCatalog(); } });
        search.addClearListener(() -> log.debug("[Lists] Cleared catalog search using search-field clear button"));
        log.debug("[Lists] Configured catalog search with RuneLite search and clear icons");
        filters.add(control("Search", search)); filters.add(Box.createVerticalStrut(5));
        availability = new JComboBox<>(new String[]{ANY, AVAILABLE_ONLY});
        boat = new JComboBox<>(new String[]{ANY_BOAT, "Raft", "Skiff", "Sloop"});
        TreeSet<String> types = UpgradeData.getAllOptions().stream().map(o -> o.partName).collect(Collectors.toCollection(() -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)));
        List<String> choices = new ArrayList<>(); choices.add(ANY_TYPE); choices.addAll(types); type = new JComboBox<>(choices.toArray(new String[0]));
        availability.addActionListener(e -> refreshCatalog()); boat.addActionListener(e -> refreshCatalog()); type.addActionListener(e -> refreshCatalog());
        filters.add(control("Show", availability)); filters.add(Box.createVerticalStrut(4)); filters.add(control("Boat", boat)); filters.add(Box.createVerticalStrut(4)); filters.add(control("Type", type));
        catalogListNotice = new JPanel(new BorderLayout());
        catalogListNotice.setOpaque(false);
        catalogListNotice.setBorder(new EmptyBorder(6, 0, 0, 0));
        catalogListNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
        catalogListNotice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel catalogListNoticeText = new JLabel("Create a list before adding upgrades.");
        catalogListNoticeText.setFont(FontManager.getRunescapeSmallFont());
        catalogListNoticeText.setForeground(Color.YELLOW);
        catalogListNoticeText.setHorizontalAlignment(SwingConstants.LEFT);
        catalogListNotice.add(catalogListNoticeText, BorderLayout.WEST);
        catalogListNotice.setVisible(!lists.hasActiveList());
        log.debug("[Lists] Configured the no-list catalog notice in a full-width left-anchored row");
        filters.add(catalogListNotice);
        return filters;
    }

    private JPanel control(String text, JComponent component)
    {
        JPanel row = new JPanel(new BorderLayout(6, 0)); row.setOpaque(false); row.setAlignmentX(Component.LEFT_ALIGNMENT); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Math.max(28, component.getPreferredSize().height)));
        JLabel label = small(text); label.setPreferredSize(new Dimension(42, 24)); row.add(label, BorderLayout.WEST); row.add(component, BorderLayout.CENTER); return row;
    }

    public void updateAvailableUpgrades(List<UpgradeData.UpgradeOption> options)
    {
        availableUpgradesContainer.removeAll();
        if (options.isEmpty())
        {
            String message = availableService.hasPublishedData()
                ? "No available upgrades"
                : "Board your boat or enter a shipyard to update available upgrades.";
            availableUpgradesContainer.add(empty(message, false));
            log.debug("[Panel] Displaying Available upgrades empty state; confirmed scan={}", availableService.hasPublishedData());
        }
        else for (UpgradeData.UpgradeOption option : options) { availableUpgradesContainer.add(upgradeRow(option, null, true, 1)); availableUpgradesContainer.add(Box.createVerticalStrut(8)); }
        redraw(availableUpgradesContainer);
    }

    private void refreshSelected()
    {
        refreshSelected(false);
    }

    private void refreshSelectedPreservingScroll()
    {
        refreshSelected(true);
    }

    private void refreshSelected(boolean preserveScroll)
    {
        int scrollPosition = selectedScrollPane == null ? 0 : selectedScrollPane.getVerticalScrollBar().getValue();
        List<MyUpgradeListService.SelectedUpgrade> selections = lists.getSelectedUpgrades(); selectedContainer.removeAll();
        log.debug("[Lists] Rendering {} selected upgrade types", selections.size());
        if (!lists.hasActiveList()) selectedContainer.add(empty("You don't have any upgrade lists yet.", true));
        else if (selections.isEmpty()) selectedContainer.add(empty("This upgrade list is empty.", false));
        else
        {
            MyUpgradeListService.ListSummary active = lists.getActiveList();
            int total = active.getUpgradeCount();
            String headingText = total + (total == 1 ? " upgrade" : " upgrades");
            selectedContainer.add(heading(headingText)); selectedContainer.add(Box.createVerticalStrut(8));
            for (MyUpgradeListService.SelectedUpgrade selected : selections)
            {
                selectedContainer.add(upgradeRow(selected.getOption(), "Quantity", false, selected.getQuantity()));
                selectedContainer.add(Box.createVerticalStrut(8));
            }
            selectedContainer.add(totals());
        }
        redraw(selectedContainer);
        if (preserveScroll && selectedScrollPane != null)
        {
            SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() ->
                selectedScrollPane.getVerticalScrollBar().setValue(Math.min(scrollPosition,
                    selectedScrollPane.getVerticalScrollBar().getMaximum()))));
            log.debug("[Lists] Preserved selected-list scroll position {} during quantity refresh", scrollPosition);
        }
    }

    private JPanel totals()
    {/**/
        JPanel panel = vertical(); panel.setBackground(ColorScheme.DARK_GRAY_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(8, 8, 8, 8)));
        panel.add(heading("Total materials")); panel.add(Box.createVerticalStrut(5)); NumberFormat nf = NumberFormat.getIntegerInstance(Locale.US);
        Map<String, Integer> totals = lists.getMaterialTotals();
        for (Map.Entry<String, Integer> entry : totals.entrySet()) { JLabel label = materialLabel(nf.format(entry.getValue()) + " x " + entry.getKey(), entry.getKey(), entry.getValue()); clickable(label, entry.getKey()); panel.add(label); }
        MyUpgradeListService.ListSummary activeList = lists.getActiveList();
        String viewName = (activeList == null ? "Upgrade list" : activeList.getName()) + " total materials";
        panel.add(Box.createVerticalStrut(5)); panel.add(viewInBankButton(viewName, totals));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height)); return panel;
    }

    private void refreshCatalog()
    {
        if (search == null || availability == null || boat == null || type == null) return;
        String query = search.getText().trim().toLowerCase(Locale.ROOT), avail = (String) availability.getSelectedItem(), boatValue = (String) boat.getSelectedItem(), typeValue = (String) type.getSelectedItem();
        log.debug("[Lists] Filtering catalog search='{}', availability='{}', boat='{}', type='{}'", query, avail, boatValue, typeValue);
        catalogContainer.removeAll(); catalogDecreaseButtons.clear(); catalogActionButtons.clear(); catalogQuantityLabels.clear();
        if (AVAILABLE_ONLY.equals(avail) && !availableService.hasPublishedData()) { catalogContainer.add(empty("Availability is known after boarding a boat or entering a shipyard.", false)); catalogLoaded = true; redraw(catalogContainer); resetCatalogScroll(); return; }
        Set<String> availableIds = availableService.get().stream().map(UpgradeData.UpgradeOption::getStableId).collect(Collectors.toSet());
        List<UpgradeData.UpgradeOption> results = UpgradeData.getAllOptions().stream()
            .filter(o -> !AVAILABLE_ONLY.equals(avail) || availableIds.contains(o.getStableId())).filter(o -> matchesBoat(o, boatValue))
            .filter(o -> ANY_TYPE.equals(typeValue) || o.partName.equals(typeValue)).filter(o -> matchesSearch(o, query))
            .sorted(Comparator.comparingInt((UpgradeData.UpgradeOption o) -> o.requiredSailingLevel).thenComparing(o -> o.partName, String.CASE_INSENSITIVE_ORDER).thenComparingInt(o -> o.targetTier).thenComparingInt(o -> o.boatType)).collect(Collectors.toList());
        log.debug("[Lists] Catalog filter returned {} upgrades", results.size());
        if (results.isEmpty()) catalogContainer.add(empty("No upgrades match these filters.", false));
        else for (UpgradeData.UpgradeOption option : results) { catalogContainer.add(upgradeRow(option, "Add", false, 1)); catalogContainer.add(Box.createVerticalStrut(8)); }
        catalogLoaded = true;
        redraw(catalogContainer);
        resetCatalogScroll();
    }

    private boolean matchesBoat(UpgradeData.UpgradeOption o, String value)
    {
        if (ANY_BOAT.equals(value)) return true;
        int boatType = "Raft".equals(value) ? 0 : "Skiff".equals(value) ? 1 : 2;
        return UpgradeData.isSupportedOnBoat(o, boatType);
    }

    private boolean matchesSearch(UpgradeData.UpgradeOption o, String q)
    {
        if (q.isEmpty()
                || o.displayName.toLowerCase(Locale.ROOT).contains(q)
                || o.partName.toLowerCase(Locale.ROOT).contains(q)
                || o.materials.stream().anyMatch(material -> material.name.toLowerCase(Locale.ROOT).contains(q)))
        {
            return true;
        }

        String schematicName = schematicUtils.getSchematicNameForUpgrade(o.displayName);
        boolean schematicMatch = schematicName != null && schematicName.toLowerCase(Locale.ROOT).contains(q);
        if (schematicMatch)
        {
            log.debug("[Lists] Catalog search '{}' matched schematic '{}' for {}", q, schematicName, o.getStableId());
        }
        return schematicMatch;
    }

    private JPanel upgradeRow(UpgradeData.UpgradeOption o, String action, boolean availableView, int quantity)
    {
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setBackground(ColorScheme.DARK_GRAY_COLOR); row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorScheme.MEDIUM_GRAY_COLOR), new EmptyBorder(6, 6, 6, 6)));
        JLabel image = new JLabel(icon(o)); image.setPreferredSize(new Dimension(50, 50)); image.setVerticalAlignment(SwingConstants.CENTER); row.add(image, BorderLayout.WEST);
        log.debug("[Panel] Vertically centered upgrade image for {}", o.getStableId());
        JPanel text = vertical(); text.setOpaque(false); JTextArea title = wrapped(o.displayName, FontManager.getRunescapeBoldFont(), Color.WHITE, 120); clickable(title, o.displayName); text.add(title);
        if (!availableView) { JLabel boatLabel = small(boatName(o)); boatLabel.setForeground(ColorScheme.BRAND_ORANGE); text.add(boatLabel); text.add(small("Sailing " + o.requiredSailingLevel + "  •  Construction " + o.requiredConstructionLevel)); }
        int requirementMultiplier = "Quantity".equals(action) ? quantity : 1;
        text.add(Box.createVerticalStrut(3));
        for (UpgradeData.Material m : o.materials)
        {
            int required = m.qty * requirementMultiplier;
            JLabel label = materialLabel(required + " x " + m.name, m.name, required);
            clickable(label, m.name); text.add(label);
        }
        if (!availableView)
        {
            String schematicName = schematicUtils.getSchematicNameForUpgrade(o.displayName);
            if (schematicName != null)
            {
                JLabel schematic = schematicLabel(o.displayName, schematicName);
                clickable(schematic, schematicName);
                text.add(schematic);
                log.debug("[Schematics] Added {} requirement to planner card {}", schematicName, o.getStableId());
            }
        }
        if (availableView) addRequirementNotes(text, o);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)); actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(5, 0, 0, 0));
        Map<String, Integer> materialRequirements = new LinkedHashMap<>();
        o.materials.forEach(material -> materialRequirements.merge(material.name, material.qty * requirementMultiplier, Integer::sum));
        JButton viewInBankButton = viewInBankButton(o.displayName + " materials", materialRequirements);
        if ("Quantity".equals(action))
        {
            JButton minus = quantityButton("−"), plus = quantityButton("+");
            minus.setToolTipText(quantity == 1 ? "Remove from list" : "Decrease quantity");
            plus.setEnabled(quantity < MyUpgradeListService.MAX_UPGRADE_QUANTITY);
            plus.setToolTipText(plus.isEnabled() ? "Increase quantity" : "Maximum quantity is " + MyUpgradeListService.MAX_UPGRADE_QUANTITY);
            JLabel count = small(String.valueOf(quantity));
            count.setForeground(Color.WHITE); count.setHorizontalAlignment(SwingConstants.CENTER);
            count.setPreferredSize(new Dimension(18, 20));
            minus.addActionListener(e ->
            {
                if (lists.decrement(o))
                {
                    refreshSelectedPreservingScroll();
                    updateCatalogAction(o);
                    updateListSelector();
                }
            });
            plus.addActionListener(e ->
            {
                if (lists.add(o))
                {
                    refreshSelectedPreservingScroll();
                    updateCatalogAction(o);
                    updateListSelector();
                }
            });
            actions.add(minus); actions.add(Box.createHorizontalStrut(3)); actions.add(count);
            actions.add(Box.createHorizontalStrut(3)); actions.add(plus);
            actions.add(Box.createHorizontalStrut(5)); actions.add(viewInBankButton);
        }
        else if ("Add".equals(action) && !availableView)
        {
            JButton minus = quantityButton("−"), plus = quantityButton("+");
            JLabel count = small("0");
            count.setForeground(Color.WHITE); count.setHorizontalAlignment(SwingConstants.CENTER);
            count.setPreferredSize(new Dimension(18, 20));
            catalogDecreaseButtons.put(o.getStableId(), minus);
            catalogActionButtons.put(o.getStableId(), plus);
            catalogQuantityLabels.put(o.getStableId(), count);
            applyCatalogActionState(o, minus, plus, count);
            minus.addActionListener(e ->
            {
                if (lists.decrement(o))
                {
                    refreshSelected();
                    updateCatalogAction(o);
                    updateListSelector();
                }
            });
            plus.addActionListener(e ->
            {
                if (lists.add(o))
                {
                    refreshSelected();
                    updateCatalogAction(o);
                    updateListSelector();
                }
            });
            actions.add(minus); actions.add(Box.createHorizontalStrut(3)); actions.add(count);
            actions.add(Box.createHorizontalStrut(3)); actions.add(plus);
            actions.add(Box.createHorizontalStrut(5)); actions.add(viewInBankButton);
        }
        else if (action == null) actions.add(viewInBankButton);
        row.add(text, BorderLayout.CENTER); row.add(actions, BorderLayout.SOUTH); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height)); return row;
    }

    private JButton quantityButton(String text)
    {
        JButton button = new JButton(text);
        button.setFont(FontManager.getRunescapeSmallFont()); button.setFocusPainted(false); button.setFocusable(false);
        button.setMargin(new Insets(0, 5, 0, 5)); button.setPreferredSize(new Dimension(26, 20));
        return button;
    }

    private JButton viewInBankButton(String viewName, Map<String, Integer> materialRequirements)
    {
        Map<String, Integer> requirements = Collections.unmodifiableMap(new LinkedHashMap<>(materialRequirements));
        Collection<String> names = requirements.keySet();
        JButton button = new JButton("View in Bank");
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setFocusPainted(false);
        button.setFocusable(false);
        button.putClientProperty("boatupgrades.bankMaterialNames", names);
        applyViewInBankButtonState(button);
        button.addActionListener(e ->
        {
            log.debug("[Bank View] Side-panel action requested '{}' for {} material requirements", viewName, requirements.size());
            bankMaterialViewService.viewMaterials(viewName, requirements);
        });
        return button;
    }

    private void addRequirementNotes(JPanel text, UpgradeData.UpgradeOption o)
    {
        if (!schematicMap.getOrDefault(o, true) && !config.filterSchematicRequirement()) { String name = schematicUtils.getSchematicNameForUpgrade(o.displayName); if (name == null) name = "schematic"; JTextArea label = wrapped("Requires " + name, FontManager.getRunescapeSmallFont(), Color.YELLOW, 160); clickable(label, name); text.add(label); }
        if (!visibilityUtils.meetsConstructionRequirement(o) && !config.filterConstructionRequirement()) { JLabel label = small("Requires " + o.requiredConstructionLevel + " Construction"); label.setForeground(Color.YELLOW); text.add(label); }
    }

    private String boatName(UpgradeData.UpgradeOption o) { return o.boatType == 0 ? "Raft" : o.boatType == 1 ? "Skiff" : o.boatType == 2 ? "Sloop" : "All supported boats"; }

    private ImageIcon icon(UpgradeData.UpgradeOption o)
    {
        return imageCache.computeIfAbsent(o.displayName + ":" + o.boatType, key -> { List<String> names = new ArrayList<>(); if (o.boatType >= 0) names.add(o.displayName + " " + o.boatType + ".png"); names.add(o.displayName + ".png"); for (String name : names) { BufferedImage image = ImageUtil.loadImageResource(getClass(), "/com/boatupgrades/ui/" + name); if (image != null) return new ImageIcon(image); } log.debug("[Panel] No image found for {}", o.getStableId()); return (ImageIcon) UIManager.getIcon("OptionPane.warningIcon"); });
    }

    private void clickable(JComponent component, String name)
    {
        Color original = component.getForeground(); component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent e) { try { String wiki = name; if (wiki.equals("Teleport focus") || wiki.equals("Greater teleport focus") || wiki.equals("Anchor") || wiki.equals("Range") || wiki.equals("Keg")) wiki += " (facility)"; if (!wiki.equals("Rosewood & cotton sails schematic")) wiki = wiki.replace("&", "and"); log.debug("[Panel] Opening wiki link for {}", name); LinkBrowser.browse("https://oldschool.runescape.wiki/w/" + URLEncoder.encode(wiki.replace(" ", "_"), StandardCharsets.UTF_8)); } catch (Exception ex) { log.debug("[Panel] Failed to open wiki link for {}", name, ex); } }
            public void mouseEntered(MouseEvent e) { component.setForeground(ColorScheme.BRAND_ORANGE); }
            public void mouseExited(MouseEvent e)
            {
                if (component instanceof JLabel && ((JLabel) component).getClientProperty("boatupgrades.materialName") != null)
                {
                    applyMaterialColor((JLabel) component);
                }
                else if (component.getClientProperty("boatupgrades.schematicUpgradeName") != null)
                {
                    applySchematicColor(component);
                }
                else component.setForeground(original);
            }
        });
    }

    private JPanel empty(String message, boolean addAction)
    {
        JPanel panel = vertical(); panel.setBorder(new EmptyBorder(20, 8, 10, 8)); JLabel label = new JLabel("<html><div style='text-align:center;width:170px'>" + message + "</div></html>"); label.setFont(FontManager.getRunescapeFont()); label.setForeground(Color.GRAY); label.setAlignmentX(Component.CENTER_ALIGNMENT); panel.add(label);
        if (addAction) { panel.add(Box.createVerticalStrut(10)); JButton button = new JButton("Create a list"); button.setFont(FontManager.getRunescapeSmallFont()); button.setForeground(ColorScheme.BRAND_ORANGE); button.setAlignmentX(Component.CENTER_ALIGNMENT); button.addActionListener(e -> promptCreateList()); panel.add(button); } return panel;
    }

    private void updateCatalogActionButtons()
    {
        for (UpgradeData.UpgradeOption option : UpgradeData.getAllOptions())
        {
            updateCatalogAction(option);
        }
        log.debug("[Lists] Updated {} existing catalog action buttons without rebuilding rows", catalogActionButtons.size());
    }

    private void updateCatalogAction(UpgradeData.UpgradeOption option)
    {
        JButton minus = catalogDecreaseButtons.get(option.getStableId());
        JButton plus = catalogActionButtons.get(option.getStableId());
        JLabel count = catalogQuantityLabels.get(option.getStableId());
        if (minus != null && plus != null && count != null) applyCatalogActionState(option, minus, plus, count);
    }

    private void applyCatalogActionState(UpgradeData.UpgradeOption option, JButton minus, JButton plus, JLabel count)
    {
        int quantity = lists.getQuantity(option);
        boolean hasList = lists.hasActiveList();
        boolean belowMaximum = quantity < MyUpgradeListService.MAX_UPGRADE_QUANTITY;
        minus.setEnabled(hasList && quantity > 0);
        minus.setForeground(minus.isEnabled() ? ColorScheme.TEXT_COLOR : Color.GRAY);
        minus.setToolTipText(!hasList ? "Create an upgrade list before changing quantities"
            : quantity == 0 ? "This upgrade is not in the active list" : "Decrease quantity");
        plus.setEnabled(hasList && belowMaximum);
        plus.setForeground(plus.isEnabled() ? ColorScheme.TEXT_COLOR : Color.GRAY);
        plus.setToolTipText(!hasList ? "Create an upgrade list before adding upgrades"
            : !belowMaximum ? "Maximum quantity is " + MyUpgradeListService.MAX_UPGRADE_QUANTITY
            : "Increase quantity");
        count.setText(String.valueOf(quantity));
        count.setVisible(true);
        log.debug("[Lists] Updated catalog quantity action for {} to {}", option.getStableId(), quantity);
    }

    private void resetCatalogScroll()
    {
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() ->
        {
            if (catalogScrollPane != null)
            {
                catalogScrollPane.getViewport().setViewPosition(new Point(0, 0));
                catalogScrollPane.getVerticalScrollBar().setValue(0);
                log.debug("[Lists] Positioned freshly filtered catalog at the top after layout completed");
            }
        }));
    }

    private JLabel small(String text) { JLabel label = new JLabel(text); label.setFont(FontManager.getRunescapeSmallFont()); label.setForeground(Color.LIGHT_GRAY); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
    private JLabel materialLabel(String text, String materialName, int quantity)
    {
        JLabel label = small(text);
        label.putClientProperty("boatupgrades.materialName", materialName);
        label.putClientProperty("boatupgrades.materialQuantity", quantity);
        applyMaterialColor(label);
        return label;
    }

    private JLabel schematicLabel(String upgradeName, String schematicName)
    {
        JLabel label = small("");
        label.setText(wrappedSchematicText(label, schematicName, 140));
        label.putClientProperty("boatupgrades.schematicUpgradeName", upgradeName);
        applySchematicColor(label);
        log.debug("[Schematics] Configured font-measured wrapping requirement text for {}", schematicName);
        return label;
    }

    private String wrappedSchematicText(JLabel label, String schematicName, int maximumWidth)
    {
        FontMetrics metrics = label.getFontMetrics(label.getFont());
        StringBuilder html = new StringBuilder("<html>(");
        StringBuilder line = new StringBuilder();
        int lineCount = 1;
        for (String word : schematicName.split(" "))
        {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && metrics.stringWidth(candidate) > maximumWidth)
            {
                html.append(escapeHtml(line.toString())).append("<br>");
                line.setLength(0);
                lineCount++;
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        html.append(escapeHtml(line.toString())).append(")</html>");
        log.debug("[Schematics] Wrapped '{}' across {} line(s) at {} pixels", schematicName, lineCount, maximumWidth);
        return html.toString();
    }

    private static String escapeHtml(String value)
    {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public void refreshSchematicColors()
    {
        SwingUtilities.invokeLater(() ->
        {
            int updated = refreshSchematicColors(cardContainer);
            cardContainer.repaint();
            log.debug("[Schematics] Recolored {} existing side-panel schematic requirements in place", updated);
        });
    }

    public void refreshMaterialColors()
    {
        SwingUtilities.invokeLater(() ->
        {
            int updated = refreshMaterialColors(cardContainer);
            cardContainer.repaint();
            log.debug("[Materials] Recolored {} visible or cached side-panel requirement labels in place", updated);
        });
    }

    public void refreshViewInBankButtons()
    {
        SwingUtilities.invokeLater(() ->
        {
            int updated = refreshViewInBankButtons(cardContainer);
            cardContainer.repaint();
            log.debug("[Bank View] Refreshed {} existing View in Bank buttons in place", updated);
        });
    }

    private int refreshViewInBankButtons(Component component)
    {
        int updated = 0;
        if (component instanceof JButton)
        {
            JButton button = (JButton) component;
            if (button.getClientProperty("boatupgrades.bankMaterialNames") != null)
            {
                applyViewInBankButtonState(button); updated++;
            }
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) updated += refreshViewInBankButtons(child);
        }
        return updated;
    }

    @SuppressWarnings("unchecked")
    private void applyViewInBankButtonState(JButton button)
    {
        Collection<String> names = (Collection<String>) button.getClientProperty("boatupgrades.bankMaterialNames");
        BankMaterialViewService.Availability availability = bankMaterialViewService.getAvailability(names);
        button.setEnabled(availability == BankMaterialViewService.Availability.AVAILABLE);
        button.setForeground(button.isEnabled() ? ColorScheme.TEXT_COLOR : Color.GRAY);
        button.setToolTipText(availability.getReason());
    }

    private int refreshMaterialColors(Component component)
    {
        int updated = 0;
        if (component instanceof JLabel)
        {
            JLabel label = (JLabel) component;
            if (label.getClientProperty("boatupgrades.materialName") != null)
            {
                applyMaterialColor(label); updated++;
            }
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents()) updated += refreshMaterialColors(child);
        }
        return updated;
    }

    private int refreshSchematicColors(Component component)
    {
        int updated = 0;
        if (component instanceof JComponent)
        {
            JComponent requirement = (JComponent) component;
            if (requirement.getClientProperty("boatupgrades.schematicUpgradeName") != null)
            {
                applySchematicColor(requirement);
                updated++;
            }
        }
        if (component instanceof Container)
        {
            for (Component child : ((Container) component).getComponents())
            {
                updated += refreshSchematicColors(child);
            }
        }
        return updated;
    }

    private void applySchematicColor(JComponent label)
    {
        if (!schematicUtils.areCachedStatesKnown())
        {
            label.setForeground(MaterialOwnershipService.NEUTRAL_COLOR);
            label.setToolTipText("Log in to update schematic status");
            return;
        }

        String upgradeName = (String) label.getClientProperty("boatupgrades.schematicUpgradeName");
        boolean unlocked = schematicUtils.hasCachedSchematic(upgradeName);
        label.setForeground(unlocked ? ColorScheme.PROGRESS_COMPLETE_COLOR : Color.YELLOW);
        //log.debug("[Schematics] Applied {} requirement color to {}",
                //unlocked ? "inventory-material green" : "locked yellow", upgradeName);
        label.setToolTipText(unlocked ? "Schematic unlocked" : "Schematic not unlocked");
    }

    private void applyMaterialColor(JLabel label)
    {
        String name = (String) label.getClientProperty("boatupgrades.materialName");
        Integer quantity = (Integer) label.getClientProperty("boatupgrades.materialQuantity");
        label.setForeground(config.coloredItemRequirements().colorsSidePanel()
                ? materialOwnershipService.getColor(name, quantity)
                : MaterialOwnershipService.NEUTRAL_COLOR);
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.US);
        if (!materialOwnershipService.hasLoadedProfile())
        {
            label.setToolTipText("Log in to view owned amounts");
        }
        else if (materialOwnershipService.isBankKnown())
        {
            int owned = materialOwnershipService.getDisplayedOwnedQuantity(name);
            String sources = materialOwnershipService.isGroupStorageIncludedInDisplayedQuantity()
                    ? "inventory + bank + group storage"
                    : "inventory + bank";
            label.setToolTipText("Owned: " + numberFormat.format(owned) + " (" + sources + ")");
        }
        else if (materialOwnershipService.isGroupStorageIncludedInDisplayedQuantity())
        {
            int owned = materialOwnershipService.getDisplayedOwnedQuantity(name);
            label.setToolTipText("Owned: " + numberFormat.format(owned)
                    + " (inventory + group storage; bank not observed)");
        }
        else
        {
            int inventoryOwned = materialOwnershipService.getInventoryQuantity(name);
            label.setToolTipText("Owned: " + numberFormat.format(inventoryOwned) + " in inventory (bank not observed)");
        }
    }
    private JLabel heading(String text) { JLabel label = new JLabel(text); label.setFont(FontManager.getRunescapeBoldFont()); label.setForeground(Color.WHITE); label.setAlignmentX(Component.LEFT_ALIGNMENT); return label; }
    private JTextArea wrapped(String value, Font font, Color color, int width) { JTextArea text = new JTextArea(value); text.setFont(font); text.setForeground(color); text.setAlignmentX(Component.LEFT_ALIGNMENT); text.setMargin(new Insets(0, 0, 0, 0)); text.setLineWrap(true); text.setWrapStyleWord(true); text.setEditable(false); text.setFocusable(false); text.setOpaque(false); text.setSize(width, Short.MAX_VALUE); text.setMaximumSize(new Dimension(width, text.getPreferredSize().height)); return text; }
    private static JPanel vertical() { JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setBackground(ColorScheme.DARKER_GRAY_COLOR); panel.setAlignmentX(Component.LEFT_ALIGNMENT); return panel; }
    private JScrollPane createScrollPane(JPanel content)
    {
        JScrollPane pane = new JScrollPane(content);
        pane.setBorder(null);
        pane.getVerticalScrollBar().setUnitIncrement(16);
        pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pane.getViewport().addChangeListener(event ->
        {
            Point position = pane.getViewport().getViewPosition();
            if (position.x != 0)
            {
                pane.getViewport().setViewPosition(new Point(0, position.y));
            }
        });
        log.debug("[Panel] Locked vertical scroll pane to the left content edge");
        return pane;
    }
    private JPanel wrapScrollPane(JScrollPane pane) { JPanel wrapper = new JPanel(new BorderLayout()); wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR); wrapper.add(pane, BorderLayout.CENTER); return wrapper; }
    private JPanel scroll(JPanel content) { return wrapScrollPane(createScrollPane(content)); }
    private void redraw(JPanel panel) { panel.revalidate(); panel.repaint(); }
}
