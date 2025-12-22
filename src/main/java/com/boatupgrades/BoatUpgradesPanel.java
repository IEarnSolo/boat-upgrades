package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Slf4j
public class BoatUpgradesPanel extends PluginPanel
{
    private static final String TAB_AVAILABLE = "AVAILABLE";
    private static final String TAB_MY_LIST = "MY_LIST";
    private static final Border ACTIVE_TAB_BORDER =
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.BRAND_ORANGE);
    private static final Border INACTIVE_TAB_BORDER =
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARK_GRAY_COLOR);
    private final CardLayout cardLayout = new CardLayout();
    public final JPanel cardContainer = new JPanel(cardLayout);

    private JLabel availableTab;
    private JLabel myListTab;
    public JPanel availableUpgradesContainer;

    private String activeTab = TAB_AVAILABLE;

    private ItemManager itemManager;
    private BoatUpgradesOverlay boatUpgradesOverlay;

    @Inject
    private AvailableUpgradesService availableUpgradesService;

    @Inject
    public BoatUpgradesPanel(ItemManager itemManager)
    {
        super(false);
        this.itemManager = itemManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCards(), BorderLayout.CENTER);
    }

    public void onAvailableUpgradesChanged()
    {
        SwingUtilities.invokeLater(() ->
        {
            log.info("[Panel] Received available upgrades update");
            updateAvailableUpgrades(availableUpgradesService.get());
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

        Image scaled = image.getScaledInstance(18, 18, Image.SCALE_SMOOTH);
        return new ImageIcon(image);
    }

    private Icon loadUpgradeImage(UpgradeData.UpgradeOption opt, int maxSize)
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
                return new ImageIcon(resizePreserveAspect(image, maxSize));
            }
        }

        return UIManager.getIcon("OptionPane.warningIcon");
    }

    private BufferedImage resizePreserveAspect(BufferedImage image, int maxSize)
    {
        int width = image.getWidth();
        int height = image.getHeight();

        float scale = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return ImageUtil.resizeImage(image, newWidth, newHeight);
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
        cardContainer.add(buildPlaceholderPanel("My list"), TAB_MY_LIST);

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

        cardContainer.revalidate();
        cardContainer.repaint();
    }

    private JPanel buildUpgradeRow(UpgradeData.UpgradeOption opt)
    {
        log.info(
                "[Panel] Building row for '{}' (boatType={})",
                opt.displayName,
                opt.boatType
        );

        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(6, 6, 6, 6));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel imageLabel = new JLabel(loadUpgradeImage(opt, 50));
        imageLabel.setPreferredSize(new Dimension(50, 50));
        row.add(imageLabel, BorderLayout.WEST);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        JPanel textCol = new JPanel();
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        textCol.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        JLabel title = new JLabel(opt.displayName);
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(Color.WHITE);

        textCol.add(title);

        if (!opt.materials.isEmpty())
        {
            JLabel mats = new JLabel(formatMaterials(opt));
            mats.setFont(FontManager.getRunescapeFont());
            mats.setForeground(Color.LIGHT_GRAY);
            textCol.add(mats);
        }

        row.add(textCol, BorderLayout.CENTER);
        return row;
    }

    private String formatMaterials(UpgradeData.UpgradeOption opt)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < opt.materials.size(); i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(opt.materials.get(i));
        }

        return sb.toString();
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