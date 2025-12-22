package com.boatupgrades;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;

import static net.runelite.api.gameval.VarbitID.SAILING_BOARDED_BOAT;
import static net.runelite.api.gameval.VarbitID.SAILING_BOARDED_BOAT_TYPE;
import static net.runelite.api.gameval.VarbitID.SAILING_PREVIOUS_BOAT_TYPE_ID;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_FACILITY_SAIL;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_FACILITY_HELM;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_FACILITY_KEEL;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_FACILITY_HULL;
import static net.runelite.api.gameval.VarClientID.SAILING_SIDEPANEL_CAPTAIN_NAME;
import static net.runelite.api.gameval.VarbitID.SAILING_SIDEPANEL_SHIPYARD_MODE;

@Slf4j
public class BoatUpgradesOverlay extends Overlay
{
    private final Client client;
    private final BoatUpgradesConfig config;
    private final FacilityService facilityService;
    private BoatUpgradesPanel panel;
    private final AvailableUpgradesService availableUpgradesService;
    private final PanelComponent panelComponent = new PanelComponent();

    private List<UpgradeData.UpgradeOption> cachedAvailable = new ArrayList<>();
    private long cacheExpiryMillis = 0L;
    private boolean prevActive = false;
    private String lastCaptainName = "";
    private int lastBoatTypeRaw = -1;
    int constructionLevel;

    @Inject
    public BoatUpgradesOverlay(Client client, BoatUpgradesConfig config, FacilityService facilityService, BoatUpgradesPanel panel, AvailableUpgradesService availableUpgradesService)
    {
        this.client = client;
        this.config = config;
        this.facilityService = facilityService;
        this.panel = panel;
        this.availableUpgradesService = availableUpgradesService;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.displayOverlay())
        {
            return null;
        }
        panelComponent.getChildren().clear();

        final long now = System.currentTimeMillis();

        int boarded = client.getVarbitValue(SAILING_BOARDED_BOAT);
        int shipyardMode = client.getVarbitValue(SAILING_SIDEPANEL_SHIPYARD_MODE);

        boolean isBoarded = boarded != 0;
        boolean isInShipyard = shipyardMode != 0;

        boolean isActive = isBoarded || isInShipyard;

        String captain = client.getVarcStrValue(SAILING_SIDEPANEL_CAPTAIN_NAME);
        if (captain == null) captain = "";
        captain = captain.replace('\u00A0', ' ').trim();

        String localName = "";
        if (client.getLocalPlayer() != null)
        {
            localName = client.getLocalPlayer().getName();
            if (localName == null) localName = "";
        }

        boolean userIsCaptain = !localName.isEmpty() && !captain.isEmpty() && localName.equalsIgnoreCase(captain);
        if (isActive && !userIsCaptain)
        {
            prevActive = true;
            return panelComponent.render(graphics);
        }

        if (isActive)
        {
            int boatTypeRaw;
            if (isBoarded)
            {
                boatTypeRaw = client.getVarbitValue(SAILING_BOARDED_BOAT_TYPE);
            }
            else
            {
                boatTypeRaw = client.getVarbitValue(SAILING_PREVIOUS_BOAT_TYPE_ID);
            }

            int sailingLevel = client.getBoostedSkillLevel(Skill.SAILING);
            constructionLevel = client.getBoostedSkillLevel(Skill.CONSTRUCTION);

            int sailTier = client.getVarbitValue(SAILING_SIDEPANEL_FACILITY_SAIL);
            int helmTier = client.getVarbitValue(SAILING_SIDEPANEL_FACILITY_HELM);
            int keelTier = client.getVarbitValue(SAILING_SIDEPANEL_FACILITY_KEEL);
            int hullTier = client.getVarbitValue(SAILING_SIDEPANEL_FACILITY_HULL);

            java.util.Map<String, Integer> currentTiers = new java.util.HashMap<>();

            currentTiers.put("Sails", sailTier);
            currentTiers.put("Helm", helmTier);
            currentTiers.put("Keel", keelTier);
            currentTiers.put("Hull", hullTier);
            currentTiers.put("Base", hullTier);

            currentTiers.putAll(facilityService.getAllHighestTiers());

            List<UpgradeData.UpgradeOption> liveAvailable =
                    UpgradeData.getAvailableOptions(
                            boatTypeRaw,
                            currentTiers,
                            sailingLevel,
                            constructionLevel,
                            config
                    );

            List<UpgradeData.UpgradeOption> toDisplayLive = filterOptions(liveAvailable);

            boolean changed = availableUpgradesService.updateIfChanged(toDisplayLive);

            if (changed && panel != null)
            {
                log.info("[Overlay] Notifying panel of upgrade change");
                panel.onAvailableUpgradesChanged();
            }
            else if (panel == null)
            {
                log.warn("[Overlay] Panel is NULL — cannot notify");
            }

            if (toDisplayLive.isEmpty())
            {
                return null;
            }

            if (!liveAvailable.isEmpty())
            {
                cachedAvailable = new ArrayList<>(liveAvailable);
                cacheExpiryMillis = Long.MAX_VALUE;
                lastCaptainName = captain;
                lastBoatTypeRaw = boatTypeRaw;
            }
            else
            {
                cachedAvailable.clear();
                cacheExpiryMillis = 0L;
                lastCaptainName = "";
                lastBoatTypeRaw = -1;
            }

            prevActive = true;

            if (liveAvailable.isEmpty())
            {
                return panelComponent.render(graphics);
            }

            renderHeaderAndOptions(graphics, toDisplayLive);

            return panelComponent.render(graphics);
        }
        else
        {
            if (prevActive)
            {
                if (!cachedAvailable.isEmpty())
                {
                    int minutes = Math.max(0, config.persistMinutes());
                    cacheExpiryMillis = now + (minutes * 60L * 1000L);
                    facilityService.resetAllDetectedTiers();
                }
                else
                {
                    cacheExpiryMillis = 0L;
                }
            }

            prevActive = false;

            if (!cachedAvailable.isEmpty() && now <= cacheExpiryMillis)
            {
                String showCaptain = lastCaptainName == null ? "" : lastCaptainName;
                int boatTypeToShow = lastBoatTypeRaw;

                if (showCaptain.isEmpty() || localName.isEmpty() || !localName.equalsIgnoreCase(showCaptain))
                {
                    return panelComponent.render(graphics);
                }

                List<UpgradeData.UpgradeOption> toDisplayCached = filterOptions(cachedAvailable);
                renderHeaderAndOptions(graphics, toDisplayCached);
                return panelComponent.render(graphics);
            }

            return panelComponent.render(graphics);
        }
    }

    private void renderHeaderAndOptions(Graphics2D graphics, List<UpgradeData.UpgradeOption> options)
    {
        panelComponent.getChildren().add(
                TitleComponent.builder().text("Boat Upgrades").build()
        );

        panelComponent.getChildren().add(
                LineComponent.builder().left("Current available upgrades:").build()
        );

        for (UpgradeData.UpgradeOption opt : options)
        {
            boolean hasSchematic = hasRequiredSchematic(opt);

            boolean meetsConstruction =
                    opt.requiredConstructionLevel <= 0
                            || constructionLevel >= opt.requiredConstructionLevel;

            if (config.filterSchematicRequirement() && !hasSchematic)
            {
                continue;
            }

            if (config.filterConstructionRequirement() && !meetsConstruction)
            {
                continue;
            }

            panelComponent.getChildren().add(
                    LineComponent.builder().left(" ").build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(opt.displayName + ":")
                            .build()
            );

            StringBuilder mats = new StringBuilder();
            for (int i = 0; i < opt.materials.size(); i++)
            {
                if (i > 0) mats.append(", ");
                mats.append(opt.materials.get(i));
            }

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(mats.toString())
                            .build()
            );

            if (!meetsConstruction)
            {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("(Requires " + opt.requiredConstructionLevel + " Con)")
                                .leftColor(Color.YELLOW)
                                .build()
                );
            }

            if (!hasSchematic)
            {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("(Requires schematic)")
                                .leftColor(Color.YELLOW)
                                .build()
                );
            }
        }
    }

    public void refreshCacheExpiry()
    {
        if (cachedAvailable == null || cachedAvailable.isEmpty())
        {
            return;
        }

        if (cacheExpiryMillis == Long.MAX_VALUE)
        {
            return;
        }

        long now = System.currentTimeMillis();
        int minutes = Math.max(0, config.persistMinutes());
        cacheExpiryMillis = now + (minutes * 60L * 1000L);
    }

    private List<UpgradeData.UpgradeOption> filterOptions(List<UpgradeData.UpgradeOption> options)
    {
        if (options == null || options.isEmpty())
        {
            return options;
        }

        List<UpgradeData.UpgradeOption> visible = new ArrayList<>();
        for (UpgradeData.UpgradeOption opt : options)
        {
            if (isConfigEnabled(opt.partName))
            {
                visible.add(opt);
            }
        }

        if (!config.hideLowerTiers())
        {
            return visible;
        }

        Map<String, UpgradeData.UpgradeOption> best = new LinkedHashMap<>();
        for (UpgradeData.UpgradeOption opt : visible)
        {
            String key = opt.partName;
            UpgradeData.UpgradeOption existing = best.get(key);
            if (existing == null || opt.targetTier > existing.targetTier)
            {
                best.put(key, opt);
            }
        }

        List<UpgradeData.UpgradeOption> filtered = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (UpgradeData.UpgradeOption opt : visible)
        {
            String key = opt.partName;
            if (added.contains(key))
            {
                continue;
            }

            if (best.get(key) == opt)
            {
                filtered.add(opt);
                added.add(key);
            }
        }

        return filtered;
    }

    private boolean isConfigEnabled(String partName)
    {
        switch (partName)
        {
            case "Base":
            case "Hull": return config.showBaseHull();
            case "Helm": return config.showHelm();
            case "Sails": return config.showSails();
            case "Keel": return config.showKeel();
            case "Salvaging Hook": return config.showSalvagingHook();
            case "Cargo Hold": return config.showCargoHold();
            case "Cannon": return config.showCannon();
            case "Teleport Focus": return config.showTeleportFocus();
            case "Wind Device": return config.showWindDevice();
            case "Trawling Net": return config.showTrawlingNet();
            case "Chum Station": return config.showChumStation();
            case "Fathom Device": return config.showFathomDevice();
            case "Range": return config.showRange();
            case "Keg": return config.showKeg();
            case "Anchor": return config.showAnchor();
            case "Inoculation Station": return config.showInoculationStation();
            case "Salvaging Station": return config.showSalvagingStation();
            case "Crystal Extractor": return config.showCrystalExtractor();
            case "Eternal Brazier": return config.showEternalBrazier();
            default: return true;
        }
    }

    private static final Map<String, Integer> SCHEMATIC_VARBITS = Map.ofEntries(
            Map.entry("Salvaging station", VarbitID.LOST_SCHEMATIC_SALVAGING_STATION),
            Map.entry("Gale catcher", VarbitID.LOST_SCHEMATIC_GALE_CATCHER),
            Map.entry("Eternal brazier", VarbitID.LOST_SCHEMATIC_ETERNAL_BRAZIER),
            Map.entry("Dragon salvaging hook", VarbitID.LOST_SCHEMATIC_DRAGON_SALVAGING_HOOK),
            Map.entry("Rosewood cargo hold", VarbitID.LOST_SCHEMATIC_ROSEWOOD_CARGO_HOLD),
            Map.entry("Dragon cannon", VarbitID.LOST_SCHEMATIC_DRAGON_CANNON),
            Map.entry("Rosewood base", VarbitID.LOST_SCHEMATIC_ROSEWOOD_HULL),
            Map.entry("Rosewood hull", VarbitID.LOST_SCHEMATIC_ROSEWOOD_HULL),
            Map.entry("Rosewood mast & cotton sails", VarbitID.LOST_SCHEMATIC_ROSEWOOD_SAIL),
            Map.entry("Dragon helm", VarbitID.LOST_SCHEMATIC_DRAGON_TILLER),
            Map.entry("Dragon keel", VarbitID.LOST_SCHEMATIC_DRAGON_KEEL)
    );

    public boolean hasRequiredSchematic(UpgradeData.UpgradeOption opt)
    {
        Integer varbit = SCHEMATIC_VARBITS.get(opt.displayName);
        if (varbit == null)
        {
            return true;
        }

        return client.getVarbitValue(varbit) == 1;
    }
}