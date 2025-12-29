package com.boatupgrades.utils;

import com.boatupgrades.BoatUpgradesConfig;
import com.boatupgrades.UpgradeData;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.inject.Inject;
import java.util.*;

public class UpgradeVisibilityUtils
{
    private final Client client;
    private final BoatUpgradesConfig config;
    private final SchematicUtils schematicUtils;

    @Inject
    public UpgradeVisibilityUtils(
            Client client,
            BoatUpgradesConfig config,
            SchematicUtils schematicUtils
    )
    {
        this.client = client;
        this.config = config;
        this.schematicUtils = schematicUtils;
    }

    public boolean meetsConstructionRequirement(UpgradeData.UpgradeOption opt)
    {
        int constructionLevel = client.getBoostedSkillLevel(Skill.CONSTRUCTION);
        return constructionLevel >= opt.requiredConstructionLevel;
    }

    public boolean shouldShowUpgrade(UpgradeData.UpgradeOption opt)
    {
        boolean hasSchematic = schematicUtils.hasSchematic(opt.displayName);
        boolean meetsConstruction = meetsConstructionRequirement(opt);

        if (config.filterSchematicRequirement() && !hasSchematic)
        {
            return false;
        }

        if (config.filterConstructionRequirement() && !meetsConstruction)
        {
            return false;
        }

        return true;
    }

    public List<UpgradeData.UpgradeOption> getVisibleUpgrades(List<UpgradeData.UpgradeOption> options)
    {
        if (options == null || options.isEmpty())
        {
            return Collections.emptyList();
        }

        List<UpgradeData.UpgradeOption> visible = new ArrayList<>();

        for (UpgradeData.UpgradeOption opt : options)
        {
            if (!isConfigEnabled(opt.partName))
            {
                continue;
            }

            boolean hasSchematic = schematicUtils.hasSchematic(opt.displayName);
            boolean meetsConstruction = meetsConstructionRequirement(opt);

            if (config.filterSchematicRequirement() && !hasSchematic)
            {
                continue;
            }

            if (config.filterConstructionRequirement() && !meetsConstruction)
            {
                continue;
            }

            visible.add(opt);
        }

        if (!config.hideLowerTiers())
        {
            return visible;
        }

        Map<String, UpgradeData.UpgradeOption> best = new LinkedHashMap<>();
        for (UpgradeData.UpgradeOption opt : visible)
        {
            best.merge(
                    opt.partName,
                    opt,
                    (a, b) -> b.targetTier > a.targetTier ? b : a
            );
        }

        List<UpgradeData.UpgradeOption> result = new ArrayList<>(best.values());

        result.sort(Comparator.comparingInt(o -> o.requiredSailingLevel));

        return result;
    }

    public boolean isConfigEnabled(String partName)
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
}
