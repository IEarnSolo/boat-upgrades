package com.boatupgrades;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
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


public class BoatUpgradesOverlay extends Overlay
{
    private final Client client;
    private final BoatUpgradesConfig config;
    private final PanelComponent panelComponent = new PanelComponent();

    private List<UpgradeData.UpgradeOption> cachedAvailable = new ArrayList<>();
    private long cacheExpiryMillis = 0L;
    private boolean prevActive = false;
    private String lastCaptainName = "";
    private int lastBoatTypeRaw = -1;

    @Inject
    public BoatUpgradesOverlay(Client client, BoatUpgradesConfig config)
    {
        this.client = client;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
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

            int sailingLevel = client.getRealSkillLevel(Skill.SAILING);

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

            java.util.List<UpgradeData.UpgradeOption> liveAvailable =
                    UpgradeData.getAvailableOptions(boatTypeRaw, currentTiers, sailingLevel);

            List<UpgradeData.UpgradeOption> toDisplayLive = filterHighestTiers(liveAvailable);

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

            if (toDisplayLive.isEmpty())
            {
                return panelComponent.render(graphics);
            }

            renderHeaderAndOptions(graphics, boatTypeRaw, localName, captain, sailingLevel, toDisplayLive);

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

                List<UpgradeData.UpgradeOption> toDisplayCached = filterHighestTiers(cachedAvailable);
                renderHeaderAndOptions(graphics, boatTypeToShow, localName, showCaptain, client.getRealSkillLevel(Skill.SAILING), toDisplayCached);
                return panelComponent.render(graphics);
            }

            return panelComponent.render(graphics);
        }
    }

    private void renderHeaderAndOptions(Graphics2D graphics, int boatTypeRaw, String localName, String captain, int sailingLevel, List<UpgradeData.UpgradeOption> options)
    {
        String boatTypeName;
        switch (boatTypeRaw)
        {
            case 0: boatTypeName = "Raft"; break;
            case 1: boatTypeName = "Skiff"; break;
            case 2: boatTypeName = "Sloop"; break;
            default: boatTypeName = "Unknown(" + boatTypeRaw + ")"; break;
        }

        panelComponent.getChildren().add(TitleComponent.builder().text("Boat Upgrades").build());
        panelComponent.getChildren().add(LineComponent.builder().left("Current available upgrades:").build());
        panelComponent.getChildren().add(LineComponent.builder().left(" ").build());

        for (UpgradeData.UpgradeOption opt : options)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(opt.displayName + ":")
                    .build());

            if (opt.materials.isEmpty())
            {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("  (no materials listed)")
                        .build());
            }
            else
            {
                StringBuilder mats = new StringBuilder();
                for (int i = 0; i < opt.materials.size(); i++)
                {
                    if (i > 0) mats.append(", ");
                    mats.append(opt.materials.get(i).toString());
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left(mats.toString())
                        .build());
                panelComponent.getChildren().add(LineComponent.builder().left(" ").build());
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

    private List<UpgradeData.UpgradeOption> filterHighestTiers(List<UpgradeData.UpgradeOption> options)
    {
        if (options == null || options.isEmpty() || !config.hideLowerTiers())
        {
            return options;
        }

        java.util.Map<String, UpgradeData.UpgradeOption> best = new java.util.LinkedHashMap<>();
        for (UpgradeData.UpgradeOption opt : options)
        {
            String key = opt.partName;
            UpgradeData.UpgradeOption existing = best.get(key);
            if (existing == null || opt.targetTier > existing.targetTier)
            {
                best.put(key, opt);
            }
        }

        java.util.List<UpgradeData.UpgradeOption> filtered = new ArrayList<>();
        java.util.Set<String> added = new java.util.HashSet<>();
        for (UpgradeData.UpgradeOption opt : options)
        {
            String key = opt.partName;
            if (added.contains(key))
            {
                continue;
            }
            UpgradeData.UpgradeOption bestOpt = best.get(key);
            if (bestOpt == opt)
            {
                filtered.add(opt);
                added.add(key);
            }
        }

        return filtered;
    }
}