package com.boatupgrades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class UpgradeData
{
    private UpgradeData() {}

    public static final class Material
    {
        public final String name;
        public final int qty;

        public Material(String name, int qty)
        {
            this.name = name;
            this.qty = qty;
        }

        @Override
        public String toString()
        {
            return qty + " x " + name;
        }
    }

    public static final class UpgradeOption
    {
        public final String partName;       // "Base", "Hull", "Helm", "Sails", "Keel"
        public final int boatType;         // 0 - raft, 1 - skiff, 2 - sloop
        public final int targetTier;       // Tier varbit value
        public final int requiredLevel;    // Sailing level required
        public final String displayName;   // Display name of boat part/facility
        public final List<Material> materials;

        public UpgradeOption(String partName, int boatType, int targetTier, int requiredLevel, String displayName, List<Material> materials)
        {
            this.partName = partName;
            this.boatType = boatType;
            this.targetTier = targetTier;
            this.requiredLevel = requiredLevel;
            this.displayName = displayName;
            this.materials = materials == null ? Collections.emptyList() : materials;
        }
    }

    private static final List<UpgradeOption> OPTIONS = new ArrayList<>();

    static
    {
        // Wooden base/hull (tier 0)
        OPTIONS.add(new UpgradeOption("Base", 0, 0, 1, "Wooden base (raft)",
                Arrays.asList(new Material("Logs",10), new Material("Rope",6), new Material("Swamp tar",10))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 0, 1, "Wooden hull (skiff)",
                Arrays.asList(new Material("Wooden hull parts",10), new Material("Bronze nails",300), new Material("Swamp tar",20))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 0, 1, "Wooden hull (sloop)",
                Arrays.asList(new Material("Large wooden hull parts",16), new Material("Bronze nails",600), new Material("Swamp tar",25))));

        // Bronze helm (tier 0)
        OPTIONS.add(new UpgradeOption("Helm", 0, 0, 1, "Bronze helm (raft)",
                Arrays.asList(new Material("Plank",2), new Material("Bronze bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 0, 1, "Bronze helm (skiff)",
                Arrays.asList(new Material("Plank",3), new Material("Bronze bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 0, 1, "Bronze helm (sloop)",
                Arrays.asList(new Material("Plank",4), new Material("Bronze bar",8))));

        // Wooden sails (tier 0)
        OPTIONS.add(new UpgradeOption("Sails", 0, 0, 1, "Wooden mast & linen sails (raft)",
                Arrays.asList(new Material("Logs",5), new Material("Bronze nails",20), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 0, 1, "Wooden mast & linen sails (skiff)",
                Arrays.asList(new Material("Logs",10), new Material("Bronze nails",40), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 0, 1, "Wooden mast & linen sails (sloop)",
                Arrays.asList(new Material("Logs",15), new Material("Bronze nails",60), new Material("Bolt of linen",10))));

        // Bronze keel (tier 0)
        OPTIONS.add(new UpgradeOption("Keel", 1, 0, 1, "Bronze keel (skiff)",
                Arrays.asList(new Material("Bronze keel parts",10))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 0, 1, "Bronze keel (sloop)",
                Arrays.asList(new Material("Large bronze keel parts",16))));

        // Iron helm (tier 1)
        OPTIONS.add(new UpgradeOption("Helm", 0, 1, 17, "Iron helm (raft)",
                Arrays.asList(new Material("Oak plank",2), new Material("Iron bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 1, 17, "Iron helm (skiff)",
                Arrays.asList(new Material("Oak plank",3), new Material("Iron bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 1, 17, "Iron helm (sloop)",
                Arrays.asList(new Material("Oak plank",4), new Material("Iron bar",8))));

        // Oak base/hull (tier 1)
        OPTIONS.add(new UpgradeOption("Base", 0, 1, 20, "Oak base (raft)",
                Arrays.asList(new Material("Oak logs",10), new Material("Rope",6), new Material("Swamp tar",10))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 1, 20, "Oak hull (skiff)",
                Arrays.asList(new Material("Oak hull parts",10), new Material("Iron nails",300), new Material("Swamp tar",20))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 1, 20, "Oak hull (sloop)",
                Arrays.asList(new Material("Large oak hull parts",16), new Material("Iron nails",600), new Material("Swamp tar",25))));

        // Iron keel (tier 1)
        OPTIONS.add(new UpgradeOption("Keel", 1, 1, 22, "Iron keel (skiff)",
                Arrays.asList(new Material("Iron keel parts",10))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 1, 22, "Iron keel (sloop)",
                Arrays.asList(new Material("Large iron keel parts",16))));

        // Oak sails (tier 1)
        OPTIONS.add(new UpgradeOption("Sails", 0, 1, 24, "Oak mast & linen sails (raft)",
                Arrays.asList(new Material("Oak logs",5), new Material("Iron nails",20), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 1, 24, "Oak mast & linen sails (skiff)",
                Arrays.asList(new Material("Oak logs",10), new Material("Iron nails",40), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 1, 24, "Oak mast & linen sails (sloop)",
                Arrays.asList(new Material("Oak logs",15), new Material("Iron nails",60), new Material("Bolt of linen",10))));

        // Teak base/hull (tier 2)
        OPTIONS.add(new UpgradeOption("Base", 0, 2, 31, "Teak base (raft)",
                Arrays.asList(new Material("Teak logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 2, 31, "Teak hull (skiff)",
                Arrays.asList(new Material("Teak hull parts",10), new Material("Steel nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 2, 31, "Teak hull (sloop)",
                Arrays.asList(new Material("Large teak hull parts",16), new Material("Steel nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Teak sails (tier 2)
        OPTIONS.add(new UpgradeOption("Sails", 0, 2, 36, "Teak mast & canvas sails (raft)",
                Arrays.asList(new Material("Teak logs",5), new Material("Steel nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 2, 36, "Teak mast & canvas sails (skiff)",
                Arrays.asList(new Material("Teak logs",10), new Material("Steel nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 2, 36, "Teak mast & canvas sails (sloop)",
                Arrays.asList(new Material("Teak logs",15), new Material("Steel nails",60), new Material("Bolt of canvas",10))));

        // Steel helm (tier 2)
        OPTIONS.add(new UpgradeOption("Helm", 0, 2, 38, "Steel helm (raft)",
                Arrays.asList(new Material("Teak plank",2), new Material("Steel bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 2, 38, "Steel helm (skiff)",
                Arrays.asList(new Material("Teak plank",3), new Material("Steel bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 2, 38, "Steel helm (sloop)",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel bar",8))));

        // Steel keel (tier 2)
        OPTIONS.add(new UpgradeOption("Keel", 1, 2, 39, "Steel keel (skiff)",
                Arrays.asList(new Material("Steel keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 2, 39, "Steel keel (sloop)",
                Arrays.asList(new Material("Large steel keel parts",16), new Material("Lead bar",5))));

        // Mahogany base/hull (tier 3)
        OPTIONS.add(new UpgradeOption("Base", 0, 3, 48, "Mahogany base (raft)",
                Arrays.asList(new Material("Mahogany logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 3, 48, "Mahogany hull (skiff)",
                Arrays.asList(new Material("Mahogany hull parts",10), new Material("Mithril nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 3, 48, "Mahogany hull (sloop)",
                Arrays.asList(new Material("Large mahogany hull parts",16), new Material("Mithril nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Mahogany sails (tier 3)
        OPTIONS.add(new UpgradeOption("Sails", 0, 3, 52, "Mahogany mast & canvas sails (raft)",
                Arrays.asList(new Material("Mahogany logs",5), new Material("Mithril nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 3, 52, "Mahogany mast & canvas sails (skiff)",
                Arrays.asList(new Material("Mahogany logs",10), new Material("Mithril nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 3, 52, "Mahogany mast & canvas sails (sloop)",
                Arrays.asList(new Material("Mahogany logs",15), new Material("Mithril nails",60), new Material("Bolt of canvas",10))));

        // Mithril keel (tier 3)
        OPTIONS.add(new UpgradeOption("Keel", 1, 3, 54, "Mithril keel (skiff)",
                Arrays.asList(new Material("Mithril keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 3, 54, "Mithril keel (sloop)",
                Arrays.asList(new Material("Large mithril keel parts",16), new Material("Lead bar",5))));

        // Mithril helm (tier 3)
        OPTIONS.add(new UpgradeOption("Helm", 0, 3, 55, "Mithril helm (raft)",
                Arrays.asList(new Material("Mahogany plank",2), new Material("Mithril bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 3, 55, "Mithril helm (skiff)",
                Arrays.asList(new Material("Mahogany plank",3), new Material("Mithril bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 3, 55, "Mithril helm (sloop)",
                Arrays.asList(new Material("Mahogany plank",4), new Material("Mithril bar",8))));

        // Adamant keel (tier 4)
        OPTIONS.add(new UpgradeOption("Keel", 1, 4, 66, "Adamant keel (skiff)",
                Arrays.asList(new Material("Adamant keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 4, 66, "Adamant keel (sloop)",
                Arrays.asList(new Material("Large adamant keel parts",16), new Material("Lead bar",5))));

        // Camphor base/hull (tier 4)
        OPTIONS.add(new UpgradeOption("Base", 0, 4, 67, "Camphor base (raft)",
                Arrays.asList(new Material("Camphor logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 4, 67, "Camphor hull (skiff)",
                Arrays.asList(new Material("Camphor hull parts",10), new Material("Adamantite nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 4, 67, "Camphor hull (sloop)",
                Arrays.asList(new Material("Large camphor hull parts",16), new Material("Adamantite nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Camphor sails (tier 4)
        OPTIONS.add(new UpgradeOption("Sails", 0, 4, 68, "Camphor mast & canvas sails (raft)",
                Arrays.asList(new Material("Camphor logs",5), new Material("Adamantite nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 4, 68, "Camphor mast & canvas sails (skiff)",
                Arrays.asList(new Material("Camphor logs",10), new Material("Adamantite nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 4, 68, "Camphor mast & canvas sails (sloop)",
                Arrays.asList(new Material("Camphor logs",15), new Material("Adamantite nails",60), new Material("Bolt of canvas",10))));

        // Adamant helm (tier 4)
        OPTIONS.add(new UpgradeOption("Helm", 0, 4, 72, "Adamant helm (raft)",
                Arrays.asList(new Material("Camphor plank",2), new Material("Adamantite bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 4, 72, "Adamant helm (skiff)",
                Arrays.asList(new Material("Camphor plank",3), new Material("Adamantite bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 4, 72, "Adamant helm (sloop)",
                Arrays.asList(new Material("Camphor plank",4), new Material("Adamantite bar",8))));

        // Ironwood base/hull (tier 5)
        OPTIONS.add(new UpgradeOption("Base", 0, 5, 81, "Ironwood base (raft)",
                Arrays.asList(new Material("Ironwood logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 5, 81, "Ironwood hull (skiff)",
                Arrays.asList(new Material("Ironwood hull parts",10), new Material("Rune nails",300), new Material("Swamp tar",20), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 5, 81, "Ironwood hull (sloop)",
                Arrays.asList(new Material("Large ironwood hull parts",16), new Material("Rune nails",600), new Material("Swamp tar",25), new Material("Cupronickel bar",5))));

        // Ironwood sails (tier 5)
        OPTIONS.add(new UpgradeOption("Sails", 0, 5, 83, "Ironwood mast & cotton sails (raft)",
                Arrays.asList(new Material("Ironwood logs",5), new Material("Rune nails",20), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 5, 83, "Ironwood mast & cotton sails (skiff)",
                Arrays.asList(new Material("Ironwood logs",10), new Material("Rune nails",40), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 5, 83, "Ironwood mast & cotton sails (sloop)",
                Arrays.asList(new Material("Ironwood logs",15), new Material("Rune nails",60), new Material("Bolt of cotton",10))));

        // Rune keel (tier 5)
        OPTIONS.add(new UpgradeOption("Keel", 1, 5, 85, "Rune keel (skiff)",
                Arrays.asList(new Material("Rune keel parts",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 5, 85, "Rune keel (sloop)",
                Arrays.asList(new Material("Large rune keel parts",16), new Material("Cupronickel bar",5))));

        // Rune helm (tier 5)
        OPTIONS.add(new UpgradeOption("Helm", 0, 5, 87, "Rune helm (raft)",
                Arrays.asList(new Material("Ironwood plank",2), new Material("Runite bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 5, 87, "Rune helm (skiff)",
                Arrays.asList(new Material("Ironwood plank",3), new Material("Runite bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 5, 87, "Rune helm (sloop)",
                Arrays.asList(new Material("Ironwood plank",4), new Material("Runite bar",8))));

        // Rosewood base/hull (tier 6)
        OPTIONS.add(new UpgradeOption("Base", 0, 6, 93, "Rosewood base (raft)",
                Arrays.asList(new Material("Rosewood logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 6, 93, "Rosewood hull (skiff)",
                Arrays.asList(new Material("Rosewood hull parts",10), new Material("Dragon nails",300), new Material("Swamp tar",20), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 6, 93, "Rosewood hull (sloop)",
                Arrays.asList(new Material("Large rosewood hull parts",16), new Material("Dragon nails",600), new Material("Swamp tar",25), new Material("Cupronickel bar",5))));

        // Rosewood sails (tier 6)
        OPTIONS.add(new UpgradeOption("Sails", 0, 6, 94, "Rosewood mast & cotton sails (raft)",
                Arrays.asList(new Material("Rosewood logs",5), new Material("Dragon nails",20), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 6, 94, "Rosewood mast & cotton sails (skiff)",
                Arrays.asList(new Material("Rosewood logs",10), new Material("Dragon nails",40), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 6, 94, "Rosewood mast & cotton sails (sloop)",
                Arrays.asList(new Material("Rosewood logs",15), new Material("Dragon nails",60), new Material("Bolt of cotton",10))));

        // Dragon helm (tier 6)
        OPTIONS.add(new UpgradeOption("Helm", 0, 6, 96, "Dragon helm (raft)",
                Arrays.asList(new Material("Rosewood plank",2), new Material("Dragon metal sheet",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 6, 96, "Dragon helm (skiff)",
                Arrays.asList(new Material("Rosewood plank",3), new Material("Dragon metal sheet",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 6, 96, "Dragon helm (sloop)",
                Arrays.asList(new Material("Rosewood plank",4), new Material("Dragon metal sheet",8))));

        // Dragon keel (tier 6)
        OPTIONS.add(new UpgradeOption("Keel", 1, 6, 97, "Dragon keel (skiff)",
                Arrays.asList(new Material("Dragon keel parts",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 6, 97, "Dragon keel (sloop)",
                Arrays.asList(new Material("Large dragon keel parts",16), new Material("Cupronickel bar",5))));
    }

    public static List<UpgradeOption> getAvailableOptions(int boatType, java.util.Map<String, Integer> currentTiers, int playerSailingLevel)
    {
        List<UpgradeOption> out = new ArrayList<>();
        for (UpgradeOption o : OPTIONS)
        {
            if (o.boatType != boatType) continue;

            Integer cur = currentTiers.get(o.partName);
            int curTier = cur == null ? -1 : cur;

            if (o.targetTier > curTier && playerSailingLevel >= o.requiredLevel)
            {
                out.add(o);
            }
        }
        return out;
    }
}