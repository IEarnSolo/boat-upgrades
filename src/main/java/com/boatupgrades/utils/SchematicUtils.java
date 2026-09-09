package com.boatupgrades.utils;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class SchematicUtils
{
    private final Client client;
    private final Map<String, Boolean> cachedUnlockStates = new ConcurrentHashMap<>();
    private volatile boolean cachedStatesKnown;

    @Inject
    public SchematicUtils(Client client)
    {
        this.client = client;
    }

    private static final List<SchematicEntry> SCHEMATICS = Arrays.asList(
            new SchematicEntry("Salvaging station schematic", "Salvaging station", VarbitID.LOST_SCHEMATIC_SALVAGING_STATION),
            new SchematicEntry("Gale catcher schematic", "Gale catcher", VarbitID.LOST_SCHEMATIC_GALE_CATCHER),
            new SchematicEntry("Rosewood hull schematic", "Rosewood hull", VarbitID.LOST_SCHEMATIC_ROSEWOOD_HULL),
            new SchematicEntry("Rosewood cargo hold schematic", "Rosewood cargo hold", VarbitID.LOST_SCHEMATIC_ROSEWOOD_CARGO_HOLD),
            new SchematicEntry("Dragon helm schematic", "Dragon helm", VarbitID.LOST_SCHEMATIC_DRAGON_TILLER),
            new SchematicEntry("Dragon salvaging hook schematic", "Dragon salvaging hook", VarbitID.LOST_SCHEMATIC_DRAGON_SALVAGING_HOOK),
            new SchematicEntry("Eternal brazier schematic", "Eternal brazier", VarbitID.LOST_SCHEMATIC_ETERNAL_BRAZIER),
            new SchematicEntry("Dragon cannon schematic", "Dragon cannon", VarbitID.LOST_SCHEMATIC_DRAGON_CANNON),
            new SchematicEntry("Rosewood & cotton sails schematic", "Rosewood mast & cotton sails", VarbitID.LOST_SCHEMATIC_ROSEWOOD_SAIL),
            new SchematicEntry("Dragon keel schematic", "Dragon keel", VarbitID.LOST_SCHEMATIC_DRAGON_KEEL)
    );

    public boolean hasSchematic(String upgradeName)
    {
        Optional<SchematicEntry> entry = SCHEMATICS.stream()
                .filter(s -> s.getUpgradeName().equals(upgradeName))
                .findFirst();

        return entry.map(schematicEntry -> client.getVarbitValue(schematicEntry.getVarbit()) == 1).orElse(true);

    }

    public String getSchematicNameForUpgrade(String upgradeName)
    {
        return SCHEMATICS.stream()
                .filter(s -> s.getUpgradeName().equals(upgradeName))
                .map(SchematicEntry::getSchematicName)
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns the last schematic state captured on the client thread. This is safe for Swing rendering.
     * A tracked schematic defaults to locked until the first client-thread refresh completes.
     */
    public boolean hasCachedSchematic(String upgradeName)
    {
        return SCHEMATICS.stream()
                .filter(entry -> entry.getUpgradeName().equals(upgradeName))
                .findFirst()
                .map(entry -> cachedUnlockStates.getOrDefault(upgradeName, false))
                .orElse(true);
    }

    public boolean areCachedStatesKnown()
    {
        return cachedStatesKnown;
    }

    public void refreshCachedUnlockStates()
    {
        for (SchematicEntry entry : SCHEMATICS)
        {
            boolean unlocked = client.getVarbitValue(entry.getVarbit()) == 1;
            cachedUnlockStates.put(entry.getUpgradeName(), unlocked);
            log.debug("[Schematics] Cached {} unlocked={}", entry.getUpgradeName(), unlocked);
        }
        cachedStatesKnown = true;
        log.debug("[Schematics] Refreshed {} schematic states on the client thread", SCHEMATICS.size());
    }

    public void clearCachedUnlockStates()
    {
        cachedUnlockStates.clear();
        cachedStatesKnown = false;
        log.debug("[Schematics] Cleared cached unlock states; schematic status is unknown until login");
    }

    public boolean refreshCachedUnlockState(int varbitId)
    {
        Optional<SchematicEntry> changed = SCHEMATICS.stream()
                .filter(entry -> entry.getVarbit() == varbitId)
                .findFirst();
        if (!changed.isPresent())
        {
            return false;
        }

        SchematicEntry entry = changed.get();
        boolean unlocked = client.getVarbitValue(entry.getVarbit()) == 1;
        cachedUnlockStates.put(entry.getUpgradeName(), unlocked);
        log.debug("[Schematics] Updated cached {} unlocked={} from varbit {}", entry.getUpgradeName(), unlocked, varbitId);
        return true;
    }

}
