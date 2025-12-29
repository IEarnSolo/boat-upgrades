package com.boatupgrades.utils;

import net.runelite.api.Client;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import java.util.*;

public class SchematicUtils
{
    private final Client client;

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
}