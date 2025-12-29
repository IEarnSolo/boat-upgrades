package com.boatupgrades;

import com.boatupgrades.utils.SchematicUtils;
import com.boatupgrades.utils.UpgradeVisibilityUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

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
    @Inject
    private UpgradeVisibilityUtils upgradeVisibilityUtils;
    @Inject
    private SchematicUtils schematicUtils;

    private final PanelComponent panelComponent = new PanelComponent();
    private List<UpgradeData.UpgradeOption> cachedAvailable = new ArrayList<>();
    private long cacheExpiryMillis = 0L;
    private boolean prevActive = false;
    private String lastCaptainName = "";
    private int lastBoatTypeRaw = -1;
    int constructionLevel;
    private String lastRenderSignature = "";
    private boolean lastActiveWasShipyard = false;



    @Inject
    public BoatUpgradesOverlay(Client client, BoatUpgradesConfig config, FacilityService facilityService, BoatUpgradesPanel panel, AvailableUpgradesService availableUpgradesService)
    {
        this.client = client;
        this.config = config;
        this.facilityService = facilityService;
        this.panel = panel;
        this.availableUpgradesService = availableUpgradesService;
        setPosition(OverlayPosition.TOP_LEFT);
        //setLayer(OverlayLayer.ABOVE_WIDGETS);
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

        String captain = client.getVarcStrValue(SAILING_SIDEPANEL_CAPTAIN_NAME).replace('\u00A0', ' ').trim();

        String localName = client.getLocalPlayer().getName();

        boolean userIsCaptain = localName != null && localName.equalsIgnoreCase(captain);
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

            List<UpgradeData.UpgradeOption> toDisplayLive = upgradeVisibilityUtils.getVisibleUpgrades(liveAvailable);

            checkRequirementSignatureChanged(toDisplayLive);

            boolean facilitiesReady = facilityService.hasDetectedAllFacilities();

            if (!facilitiesReady)
            {
                return null;
            }

            boolean changed = availableUpgradesService.updateIfChanged(toDisplayLive);

            if (changed && panel != null)
            {
                log.debug("[Overlay] Notifying panel of upgrade change");
                panel.onAvailableUpgradesChanged();
            }
            else if (panel == null)
            {
                log.warn("[Overlay] Panel is NULL — cannot notify");
            }

            if (!liveAvailable.isEmpty())
            {
                cachedAvailable = new ArrayList<>(liveAvailable);
                cacheExpiryMillis = Long.MAX_VALUE;
                lastCaptainName = captain;
                lastBoatTypeRaw = boatTypeRaw;
                if (isInShipyard)
                {
                    lastActiveWasShipyard = true;
                }
                else
                {
                    lastActiveWasShipyard = false;
                }
            }
            else
            {
                cachedAvailable.clear();
                cacheExpiryMillis = 0L;
                lastCaptainName = "";
                lastBoatTypeRaw = -1;
                lastActiveWasShipyard = false;
            }

            prevActive = true;

            if (liveAvailable.isEmpty())
            {
                return null;
            }
            if (toDisplayLive.isEmpty())
            {
                return null;
            }

            if (!config.displayOverlay())
            {
                return null;
            }

            if (config.overlayOnlyInShipyard() && !isInShipyard)
            {
                return null;
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

            List<UpgradeData.UpgradeOption> toDisplayCached = upgradeVisibilityUtils.getVisibleUpgrades(cachedAvailable);

            checkRequirementSignatureChanged(toDisplayCached);

            boolean changed = availableUpgradesService.updateIfChanged(toDisplayCached);

            if (changed && panel != null)
            {
                log.debug("[Overlay] Notifying panel of upgrade change");
                panel.onAvailableUpgradesChanged();
            }
            else if (panel == null)
            {
                log.warn("[Overlay] Panel is NULL — cannot notify");
            }
            facilityService.detectedFacilitiesComplete = false;

            if (!cachedAvailable.isEmpty() && now <= cacheExpiryMillis)
            {
                String showCaptain = lastCaptainName == null ? "" : lastCaptainName;

                if (showCaptain.isEmpty() || localName.isEmpty() || !localName.equalsIgnoreCase(showCaptain))
                {
                    return panelComponent.render(graphics);
                }

                if (!config.displayOverlay())
                {
                    return null;
                }

                if (config.overlayOnlyInShipyard() && !lastActiveWasShipyard)
                {
                    return null;
                }

                if (toDisplayCached.isEmpty())
                {
                    return null;
                }

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
            boolean hasSchematic = schematicUtils.hasSchematic(opt.displayName);
            boolean meetsConstruction = upgradeVisibilityUtils.meetsConstructionRequirement(opt);

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(" ")
                            .build()
            );

            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left(opt.displayName + ":")
                            .build()
            );

            String mats = opt.materials.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));

            panelComponent.getChildren().add(
                    LineComponent.builder().left(mats).build()
            );

            if (!hasSchematic && !config.filterSchematicRequirement())
            {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("(Requires schematic)")
                                .leftColor(Color.YELLOW)
                                .build()
                );
            }

            if (!meetsConstruction && !config.filterConstructionRequirement())
            {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left("(Requires " + opt.requiredConstructionLevel + " Con)")
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
    private String computeRequirementSignature(List<UpgradeData.UpgradeOption> options)
    {
        List<String> sig = new ArrayList<>();

        for (UpgradeData.UpgradeOption opt : options)
        {
            sig.add(opt.displayName);

            boolean hasSchematic = schematicUtils.hasSchematic(opt.displayName);
            boolean meetsConstruction = upgradeVisibilityUtils.meetsConstructionRequirement(opt);

            if (!hasSchematic && !config.filterSchematicRequirement())
            {
                sig.add("REQ_SCHEMATIC");
            }

            if (!meetsConstruction && !config.filterConstructionRequirement())
            {
                sig.add("REQ_CON");
            }
        }

        return String.join("|", sig);
    }

    private void checkRequirementSignatureChanged(List<UpgradeData.UpgradeOption> options)
    {
        String newSignature = computeRequirementSignature(options);

        if (!newSignature.equals(lastRenderSignature))
        {
            lastRenderSignature = newSignature;

            if (panel != null)
            {
                panel.onAvailableUpgradesChanged();
            }
        }
    }

}