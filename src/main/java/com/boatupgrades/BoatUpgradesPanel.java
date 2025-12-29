package com.boatupgrades;

import com.boatupgrades.utils.SchematicUtils;
import com.boatupgrades.utils.UpgradeVisibilityUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class BoatUpgradesPanel extends PluginPanel
{
    private static final String TAB_AVAILABLE = "AVAILABLE";
    private static final String TAB_MY_LIST = "MY_LIST";
    private String activeTab = TAB_AVAILABLE;

    private static final Border ACTIVE_TAB_BORDER =
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE);
    private static final Border INACTIVE_TAB_BORDER =
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR);

    private final CardLayout cardLayout = new CardLayout();

    public final JPanel cardContainer = new JPanel(cardLayout);
    public JPanel availableUpgradesContainer;

    private JLabel availableTab;
    private JLabel myListTab;

    private final Map<String, ImageIcon> imageCache = new HashMap<>();
    private final Map<UpgradeData.UpgradeOption, Boolean> schematicUnlockedMap = new HashMap<>();


    private ItemManager itemManager;
    private BoatUpgradesOverlay boatUpgradesOverlay;
    private final BoatUpgradesConfig config;
    private Client client;
    @Inject
    private SchematicUtils schematicUtils;
    @Inject
    private AvailableUpgradesService availableUpgradesService;
    @Inject
    private UpgradeVisibilityUtils upgradeVisibilityUtils;

    @Inject
    public BoatUpgradesPanel(Client client, ItemManager itemManager, BoatUpgradesConfig config)
    {
        super(false);
        this.itemManager = itemManager;
        this.config = config;
        this.client = client;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCards(), BorderLayout.CENTER);
    }

    public void onAvailableUpgradesChanged()
    {
        List<UpgradeData.UpgradeOption> options = availableUpgradesService.get();

        schematicUnlockedMap.clear();
        for (UpgradeData.UpgradeOption opt : options)
        {
            schematicUnlockedMap.put(opt, schematicUtils.hasSchematic(opt.displayName));
        }

        List<UpgradeData.UpgradeOption> filteredOptions = options.stream()
                .filter(upgradeVisibilityUtils::shouldShowUpgrade)
                .collect(Collectors.toList());

        SwingUtilities.invokeLater(() ->
        {
            updateAvailableUpgrades(filteredOptions);
        });
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel titleRow = new JPanel();
        titleRow.setLayout(new BoxLayout(titleRow, BoxLayout.X_AXIS));
        titleRow.setOpaque(false);
        titleRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel iconLabel = new JLabel(loadHeaderIcon());
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 6));

        JLabel title = new JLabel("Boat Upgrades");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);

        titleRow.add(iconLabel);
        titleRow.add(title);

        header.add(titleRow);
        header.add(Box.createVerticalStrut(8));
        header.add(buildTabBar());

        return header;
    }

    private ImageIcon loadHeaderIcon()
    {
        BufferedImage image = ImageUtil.loadImageResource(
                getClass(),
                "icon.png"
        );

        //Image scaled = image.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private ImageIcon getUpgradeIcon(UpgradeData.UpgradeOption opt)
    {
        String key = opt.displayName + ":" + opt.boatType;

        return imageCache.computeIfAbsent(key, k -> {
            BufferedImage img = loadUpgradeImage(opt);
            if (img != null) {
                return new ImageIcon(img);
            } else {
                return (ImageIcon) UIManager.getIcon("OptionPane.warningIcon");
            }
        });
    }

    private BufferedImage loadUpgradeImage(UpgradeData.UpgradeOption opt)
    {
        String basePath = "/com/boatupgrades/ui/";

        List<String> candidates = new ArrayList<>();

        if (opt.boatType >= 0)
        {
            candidates.add(opt.displayName + " " + opt.boatType + ".png");
        }
        candidates.add(opt.displayName + ".png");

        for (String name : candidates)
        {
            BufferedImage image = ImageUtil.loadImageResource(
                    getClass(),
                    basePath + name
            );

            if (image != null)
            {
                return image;
            }
        }

        return null;
    }

    private JPanel buildTabBar()
    {
        JPanel tabBar = new JPanel(new GridLayout(1, 2));
        tabBar.setBackground(ColorScheme.DARK_GRAY_COLOR);

        availableTab = createTabLabel("Available upgrades", TAB_AVAILABLE);
        myListTab = createTabLabel("My list", TAB_MY_LIST);

        tabBar.add(availableTab);
        tabBar.add(myListTab);

        updateTabStyles();

        return tabBar;
    }

    private JLabel createTabLabel(String text, String tabKey)
    {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(FontManager.getRunescapeFont());
        label.setForeground(Color.WHITE);
        label.setBorder(new EmptyBorder(6, 4, 6, 4));
        label.setOpaque(true);
        label.setBackground(ColorScheme.DARK_GRAY_COLOR);

        label.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                activeTab = tabKey;
                cardLayout.show(cardContainer, tabKey);
                updateTabStyles();
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                if (!activeTab.equals(tabKey))
                {
                    label.setForeground(ColorScheme.BRAND_ORANGE);
                }
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                updateTabStyles();
            }
        });

        return label;
    }

    private void updateTabStyles()
    {
        styleTab(availableTab, TAB_AVAILABLE);
        styleTab(myListTab, TAB_MY_LIST);
    }

    private void styleTab(JLabel tab, String tabKey)
    {
        boolean active = activeTab.equals(tabKey);

        tab.setForeground(Color.WHITE);
        tab.setBackground(ColorScheme.DARK_GRAY_COLOR);
        tab.setBorder(active ? ACTIVE_TAB_BORDER : INACTIVE_TAB_BORDER);
    }

    private JPanel buildCards()
    {
        cardContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        cardContainer.add(buildAvailableUpgradesCard(), TAB_AVAILABLE);
        cardContainer.add(buildPlaceholderPanel("Under development"), TAB_MY_LIST);

        cardLayout.show(cardContainer, TAB_AVAILABLE);

        return cardContainer;
    }

    private JPanel buildPlaceholderPanel(String text)
    {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(15, 10, 10, 10));

        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setFont(FontManager.getRunescapeFont());

        panel.add(label, BorderLayout.NORTH);
        return panel;
    }

    private JPanel buildAvailableUpgradesCard()
    {
        availableUpgradesContainer = new JPanel();
        availableUpgradesContainer.setLayout(new BoxLayout(availableUpgradesContainer, BoxLayout.Y_AXIS));
        availableUpgradesContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        availableUpgradesContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        availableUpgradesContainer.setAlignmentX(Component.LEFT_ALIGNMENT);


        JScrollPane scroll = new JScrollPane(availableUpgradesContainer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        wrapper.add(scroll, BorderLayout.CENTER);

        return wrapper;
    }

    public void updateAvailableUpgrades(List<UpgradeData.UpgradeOption> options)
    {
        availableUpgradesContainer.removeAll();

        if (options.isEmpty())
        {
            availableUpgradesContainer.add(buildEmptyState("No available upgrades"));
        }
        else
        {
            for (UpgradeData.UpgradeOption opt : options)
            {
                availableUpgradesContainer.add(buildUpgradeRow(opt));
                availableUpgradesContainer.add(Box.createVerticalStrut(8));
            }
        }

        availableUpgradesContainer.revalidate();
        availableUpgradesContainer.repaint();
    }

    private JPanel buildUpgradeRow(UpgradeData.UpgradeOption opt)
    {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(6, 6, 6, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel imageLabel = new JLabel(getUpgradeIcon(opt));
        imageLabel.setPreferredSize(new Dimension(50, 50));
        row.add(imageLabel, BorderLayout.WEST);

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JTextArea title = new JTextArea(opt.displayName);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);
        title.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setMargin(new Insets(0, 0, 0, 0));
        title.setLineWrap(true);
        title.setWrapStyleWord(true);
        title.setEditable(false);
        title.setFocusable(false);
        title.setOpaque(false);

        title.setSize(100, Short.MAX_VALUE);
        Dimension titlePreferredSize = title.getPreferredSize();
        title.setMaximumSize(new Dimension(100, titlePreferredSize.height));

        makeClickable(title, opt.displayName);
        textCol.add(title);

        textCol.add(Box.createVerticalStrut(2));

        for (UpgradeData.Material material : opt.materials)
        {
            JLabel matLabel = new JLabel(String.valueOf(material));
            matLabel.setFont(FontManager.getRunescapeSmallFont());
            matLabel.setForeground(Color.LIGHT_GRAY);
            matLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            makeClickable(matLabel, material.name);
            textCol.add(matLabel);
        }

        boolean hasSchematic = schematicUnlockedMap.getOrDefault(opt, true);

        if (!hasSchematic && !config.filterSchematicRequirement())
        {
            textCol.add(Box.createVerticalStrut(2));

            String schematicName = schematicUtils.getSchematicNameForUpgrade(opt.displayName);
            if (schematicName == null)
            {
                schematicName = "schematic";
            }

            JTextArea schematicLabel = new JTextArea("Requires " + schematicName);
            schematicLabel.setFont(FontManager.getRunescapeSmallFont());
            schematicLabel.setForeground(Color.YELLOW);
            schematicLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            schematicLabel.setMargin(new Insets(0, 0, 0, 0));
            schematicLabel.setLineWrap(true);
            schematicLabel.setWrapStyleWord(true);
            schematicLabel.setEditable(false);
            schematicLabel.setFocusable(false);
            schematicLabel.setOpaque(false);

            schematicLabel.setSize(160, Short.MAX_VALUE);
            Dimension schematicLabelPreferredSize = schematicLabel.getPreferredSize();
            schematicLabel.setMaximumSize(new Dimension(160, schematicLabelPreferredSize.height));

            makeClickable(schematicLabel, schematicName);

            textCol.add(schematicLabel);
        }

        boolean meetsConstruction = upgradeVisibilityUtils.meetsConstructionRequirement(opt);

        if (!meetsConstruction && !config.filterConstructionRequirement())
        {
            textCol.add(Box.createVerticalStrut(2));

            JLabel constructionLabel = new JLabel("Requires " + opt.requiredConstructionLevel + " Construction");
            constructionLabel.setFont(FontManager.getRunescapeSmallFont());
            constructionLabel.setForeground(Color.YELLOW);
            constructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            //makeClickable(constructionLabel, "Construction");

            textCol.add(constructionLabel);
        }

        row.add(textCol, BorderLayout.CENTER);

        Dimension rowPreferredSize = row.getPreferredSize();
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, rowPreferredSize.height));

        return row;
    }

    private void makeClickable(JComponent component, String name)
    {
        Color originalColor = component.getForeground();
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        component.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                try
                {
                    String wikiName = name;

                    if (wikiName.equals("Teleport focus") ||
                            wikiName.equals("Greater teleport focus") ||
                            wikiName.equals("Anchor") ||
                            wikiName.equals("Range") ||
                            wikiName.equals("Keg"))
                    {
                        wikiName += " (facility)";
                    }

                    if (!wikiName.equals("Rosewood & cotton sails schematic"))
                    {
                        wikiName = wikiName.replace("&", "and");
                    }

                    wikiName = URLEncoder.encode(wikiName.replace(" ", "_"), StandardCharsets.UTF_8);

                    LinkBrowser.browse("https://oldschool.runescape.wiki/w/" + wikiName);


                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                component.setForeground(ColorScheme.BRAND_ORANGE);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                component.setForeground(originalColor);
            }
        });
    }

    private JPanel buildEmptyState(String text)
    {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        panel.setBorder(new EmptyBorder(20, 10, 10, 10));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(text);
        label.setFont(FontManager.getRunescapeFont());
        label.setForeground(Color.GRAY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(label);

        return panel;
    }
}