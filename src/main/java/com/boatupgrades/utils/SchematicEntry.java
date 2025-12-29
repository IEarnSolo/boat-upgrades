package com.boatupgrades.utils;

public class SchematicEntry
{
    private final String schematicName;
    private final String upgradeName;
    private final int varbit;

    public SchematicEntry(String schematicName, String upgradeName, int varbitID)
    {
        this.schematicName = schematicName;
        this.upgradeName = upgradeName;
        this.varbit = varbitID;
    }

    public String getSchematicName()
    {
        return schematicName;
    }

    public String getUpgradeName()
    {
        return upgradeName;
    }

    public int getVarbit()
    {
        return varbit;
    }
}

