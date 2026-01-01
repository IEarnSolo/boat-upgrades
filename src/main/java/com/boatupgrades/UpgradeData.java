package com.boatupgrades;

import java.util.*;

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
        public final String partName;
        public final int boatType;
        public final int targetTier;
        public final int requiredSailingLevel;
        public final int requiredConstructionLevel;
        public final String displayName;
        public final List<Material> materials;

        public UpgradeOption(String partName, int boatType, int targetTier, int requiredSailingLevel, int requiredConstructionLevel, String displayName, List<Material> materials)
        {
            this.partName = partName;
            this.boatType = boatType;
            this.targetTier = targetTier;
            this.requiredSailingLevel = requiredSailingLevel;
            this.requiredConstructionLevel = requiredConstructionLevel;
            this.displayName = displayName;
            this.materials = materials == null ? Collections.emptyList() : materials;
        }
    }

    private static final List<UpgradeOption> OPTIONS = new ArrayList<>();
    private static final Set<String> RAFT_EXCLUDED_FACILITIES = Set.of(
            "Inoculation Station",
            "Trawling Net",
            "Chum Station",
            "Anchor",
            "Fathom Device",
            "Salvaging Station",
            "Crystal Extractor"
    );

    static
    {
    // Core boat parts
        // Wooden base/hull (tier 0)
        OPTIONS.add(new UpgradeOption("Base", 0, 0, 1, 1, "Wooden base",
                Arrays.asList(new Material("Logs",10), new Material("Rope",6), new Material("Swamp tar",10))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 0, 1, 1, "Wooden hull",
                Arrays.asList(new Material("Wooden hull parts",10), new Material("Bronze nails",300), new Material("Swamp tar",20))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 0, 1, 1, "Wooden hull",
                Arrays.asList(new Material("Large wooden hull parts",16), new Material("Bronze nails",600), new Material("Swamp tar",25))));

        // Bronze helm (tier 0)
        OPTIONS.add(new UpgradeOption("Helm", 0, 0, 1, 1, "Bronze helm",
                Arrays.asList(new Material("Plank",2), new Material("Bronze bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 0, 1, 1, "Bronze helm",
                Arrays.asList(new Material("Plank",3), new Material("Bronze bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 0, 1, 1, "Bronze helm",
                Arrays.asList(new Material("Plank",4), new Material("Bronze bar",8))));

        // Wooden sails (tier 0)
        OPTIONS.add(new UpgradeOption("Sails", 0, 0, 1, 1, "Wooden mast & linen sails",
                Arrays.asList(new Material("Logs",5), new Material("Bronze nails",20), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 0, 1, 1, "Wooden mast & linen sails",
                Arrays.asList(new Material("Logs",10), new Material("Bronze nails",40), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 0, 1, 1, "Wooden mast & linen sails",
                Arrays.asList(new Material("Logs",15), new Material("Bronze nails",60), new Material("Bolt of linen",10))));

        // Bronze keel (tier 0)
        OPTIONS.add(new UpgradeOption("Keel", 1, 0, 1, 1, "Bronze keel",
                Arrays.asList(new Material("Bronze keel parts",10))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 0, 1, 1, "Bronze keel",
                Arrays.asList(new Material("Large bronze keel parts",16))));

        // Iron helm (tier 1)
        OPTIONS.add(new UpgradeOption("Helm", 0, 1, 17, 14, "Iron helm",
                Arrays.asList(new Material("Oak plank",2), new Material("Iron bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 1, 17, 14, "Iron helm",
                Arrays.asList(new Material("Oak plank",3), new Material("Iron bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 1, 17, 14, "Iron helm",
                Arrays.asList(new Material("Oak plank",4), new Material("Iron bar",8))));

        // Oak base/hull (tier 1)
        OPTIONS.add(new UpgradeOption("Base", 0, 1, 20, 8, "Oak base",
                Arrays.asList(new Material("Oak logs",10), new Material("Rope",6), new Material("Swamp tar",10))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 1, 20, 8, "Oak hull",
                Arrays.asList(new Material("Oak hull parts",10), new Material("Iron nails",300), new Material("Swamp tar",20))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 1, 20, 8, "Oak hull",
                Arrays.asList(new Material("Large oak hull parts",16), new Material("Iron nails",600), new Material("Swamp tar",25))));

        // Iron keel (tier 1)
        OPTIONS.add(new UpgradeOption("Keel", 1, 1, 22, 17, "Iron keel",
                Arrays.asList(new Material("Iron keel parts",10))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 1, 22, 17, "Iron keel",
                Arrays.asList(new Material("Large iron keel parts",16))));

        // Oak sails (tier 1)
        OPTIONS.add(new UpgradeOption("Sails", 0, 1, 24, 11, "Oak mast & linen sails",
                Arrays.asList(new Material("Oak logs",5), new Material("Iron nails",20), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 1, 24, 11, "Oak mast & linen sails",
                Arrays.asList(new Material("Oak logs",10), new Material("Iron nails",40), new Material("Bolt of linen",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 1, 24, 11, "Oak mast & linen sails",
                Arrays.asList(new Material("Oak logs",15), new Material("Iron nails",60), new Material("Bolt of linen",10))));

        // Teak base/hull (tier 2)
        OPTIONS.add(new UpgradeOption("Base", 0, 2, 31, 23, "Teak base",
                Arrays.asList(new Material("Teak logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 2, 31, 23, "Teak hull",
                Arrays.asList(new Material("Teak hull parts",10), new Material("Steel nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 2, 31, 23, "Teak hull",
                Arrays.asList(new Material("Large teak hull parts",16), new Material("Steel nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Teak sails (tier 2)
        OPTIONS.add(new UpgradeOption("Sails", 0, 2, 36, 26, "Teak mast & canvas sails",
                Arrays.asList(new Material("Teak logs",5), new Material("Steel nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 2, 36, 26, "Teak mast & canvas sails",
                Arrays.asList(new Material("Teak logs",10), new Material("Steel nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 2, 36, 26, "Teak mast & canvas sails",
                Arrays.asList(new Material("Teak logs",15), new Material("Steel nails",60), new Material("Bolt of canvas",10))));

        // Steel helm (tier 2)
        OPTIONS.add(new UpgradeOption("Helm", 0, 2, 38, 30, "Steel helm",
                Arrays.asList(new Material("Teak plank",2), new Material("Steel bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 2, 38, 30, "Steel helm",
                Arrays.asList(new Material("Teak plank",3), new Material("Steel bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 2, 38, 30, "Steel helm",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel bar",8))));

        // Steel keel (tier 2)
        OPTIONS.add(new UpgradeOption("Keel", 1, 2, 39, 32, "Steel keel",
                Arrays.asList(new Material("Steel keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 2, 39, 32, "Steel keel",
                Arrays.asList(new Material("Large steel keel parts",16), new Material("Lead bar",5))));

        // Mahogany base/hull (tier 3)
        OPTIONS.add(new UpgradeOption("Base", 0, 3, 48, 41, "Mahogany base",
                Arrays.asList(new Material("Mahogany logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 3, 48, 41, "Mahogany hull",
                Arrays.asList(new Material("Mahogany hull parts",10), new Material("Mithril nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 3, 48, 41, "Mahogany hull",
                Arrays.asList(new Material("Large mahogany hull parts",16), new Material("Mithril nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Mahogany sails (tier 3)
        OPTIONS.add(new UpgradeOption("Sails", 0, 3, 52, 45, "Mahogany mast & canvas sails",
                Arrays.asList(new Material("Mahogany logs",5), new Material("Mithril nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 3, 52, 45, "Mahogany mast & canvas sails",
                Arrays.asList(new Material("Mahogany logs",10), new Material("Mithril nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 3, 52, 45, "Mahogany mast & canvas sails",
                Arrays.asList(new Material("Mahogany logs",15), new Material("Mithril nails",60), new Material("Bolt of canvas",10))));

        // Mithril keel (tier 3)
        OPTIONS.add(new UpgradeOption("Keel", 1, 3, 54, 50, "Mithril keel",
                Arrays.asList(new Material("Mithril keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 3, 54, 50, "Mithril keel",
                Arrays.asList(new Material("Large mithril keel parts",16), new Material("Lead bar",5))));

        // Mithril helm (tier 3)
        OPTIONS.add(new UpgradeOption("Helm", 0, 3, 55, 47, "Mithril helm",
                Arrays.asList(new Material("Mahogany plank",2), new Material("Mithril bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 3, 55, 47, "Mithril helm",
                Arrays.asList(new Material("Mahogany plank",3), new Material("Mithril bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 3, 55, 47, "Mithril helm",
                Arrays.asList(new Material("Mahogany plank",4), new Material("Mithril bar",8))));

        // Adamant keel (tier 4)
        OPTIONS.add(new UpgradeOption("Keel", 1, 4, 66, 62, "Adamant keel",
                Arrays.asList(new Material("Adamant keel parts",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 4, 66, 62, "Adamant keel",
                Arrays.asList(new Material("Large adamant keel parts",16), new Material("Lead bar",5))));

        // Camphor base/hull (tier 4)
        OPTIONS.add(new UpgradeOption("Base", 0, 4, 67, 59, "Camphor base",
                Arrays.asList(new Material("Camphor logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 4, 67, 59, "Camphor hull",
                Arrays.asList(new Material("Camphor hull parts",10), new Material("Adamantite nails",300), new Material("Swamp tar",20), new Material("Lead bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 4, 67, 59, "Camphor hull",
                Arrays.asList(new Material("Large camphor hull parts",16), new Material("Adamantite nails",600), new Material("Swamp tar",25), new Material("Lead bar",5))));

        // Camphor sails (tier 4)
        OPTIONS.add(new UpgradeOption("Sails", 0, 4, 68, 60, "Camphor mast & canvas sails",
                Arrays.asList(new Material("Camphor logs",5), new Material("Adamantite nails",20), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 4, 68, 60, "Camphor mast & canvas sails",
                Arrays.asList(new Material("Camphor logs",10), new Material("Adamantite nails",40), new Material("Bolt of canvas",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 4, 68, 60, "Camphor mast & canvas sails",
                Arrays.asList(new Material("Camphor logs",15), new Material("Adamantite nails",60), new Material("Bolt of canvas",10))));

        // Adamant helm (tier 4)
        OPTIONS.add(new UpgradeOption("Helm", 0, 4, 72, 59, "Adamant helm",
                Arrays.asList(new Material("Camphor plank",2), new Material("Adamantite bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 4, 72, 59, "Adamant helm",
                Arrays.asList(new Material("Camphor plank",3), new Material("Adamantite bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 4, 72, 59, "Adamant helm",
                Arrays.asList(new Material("Camphor plank",4), new Material("Adamantite bar",8))));

        // Ironwood base/hull (tier 5)
        OPTIONS.add(new UpgradeOption("Base", 0, 5, 81,75,  "Ironwood base",
                Arrays.asList(new Material("Ironwood logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 5, 81, 75, "Ironwood hull",
                Arrays.asList(new Material("Ironwood hull parts",10), new Material("Rune nails",300), new Material("Swamp tar",20), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 5, 81, 75, "Ironwood hull",
                Arrays.asList(new Material("Large ironwood hull parts",16), new Material("Rune nails",600), new Material("Swamp tar",25), new Material("Cupronickel bar",5))));

        // Ironwood sails (tier 5)
        OPTIONS.add(new UpgradeOption("Sails", 0, 5, 83, 77, "Ironwood mast & cotton sails",
                Arrays.asList(new Material("Ironwood logs",5), new Material("Rune nails",20), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 5, 83, 77, "Ironwood mast & cotton sails",
                Arrays.asList(new Material("Ironwood logs",10), new Material("Rune nails",40), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 5, 83, 77, "Ironwood mast & cotton sails",
                Arrays.asList(new Material("Ironwood logs",15), new Material("Rune nails",60), new Material("Bolt of cotton",10))));

        // Rune keel (tier 5)
        OPTIONS.add(new UpgradeOption("Keel", 1, 5, 85, 78, "Rune keel",
                Arrays.asList(new Material("Rune keel parts",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 5, 85, 78, "Rune keel",
                Arrays.asList(new Material("Large rune keel parts",16), new Material("Cupronickel bar",5))));

        // Rune helm (tier 5)
        OPTIONS.add(new UpgradeOption("Helm", 0, 5, 87, 81, "Rune helm",
                Arrays.asList(new Material("Ironwood plank",2), new Material("Runite bar",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 5, 87, 81, "Rune helm",
                Arrays.asList(new Material("Ironwood plank",3), new Material("Runite bar",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 5, 87, 81, "Rune helm",
                Arrays.asList(new Material("Ironwood plank",4), new Material("Runite bar",8))));

        // Rosewood base/hull (tier 6)
        OPTIONS.add(new UpgradeOption("Base", 0, 6, 93, 84, "Rosewood base",
                Arrays.asList(new Material("Rosewood logs",10), new Material("Rope",6), new Material("Swamp tar",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 1, 6, 93, 84, "Rosewood hull",
                Arrays.asList(new Material("Rosewood hull parts",10), new Material("Dragon nails",300), new Material("Swamp tar",20), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Hull", 2, 6, 93, 84, "Rosewood hull",
                Arrays.asList(new Material("Large rosewood hull parts",16), new Material("Dragon nails",600), new Material("Swamp tar",25), new Material("Cupronickel bar",5))));

        // Rosewood sails (tier 6)
        OPTIONS.add(new UpgradeOption("Sails", 0, 6, 94, 85, "Rosewood mast & cotton sails",
                Arrays.asList(new Material("Rosewood logs",5), new Material("Dragon nails",20), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 1, 6, 94, 85, "Rosewood mast & cotton sails",
                Arrays.asList(new Material("Rosewood logs",10), new Material("Dragon nails",40), new Material("Bolt of cotton",5))));
        OPTIONS.add(new UpgradeOption("Sails", 2, 6, 94, 85, "Rosewood mast & cotton sails",
                Arrays.asList(new Material("Rosewood logs",15), new Material("Dragon nails",60), new Material("Bolt of cotton",10))));

        // Dragon helm (tier 6)
        OPTIONS.add(new UpgradeOption("Helm", 0, 6, 96, 86, "Dragon helm",
                Arrays.asList(new Material("Rosewood plank",2), new Material("Dragon metal sheet",4))));
        OPTIONS.add(new UpgradeOption("Helm", 1, 6, 96, 86, "Dragon helm",
                Arrays.asList(new Material("Rosewood plank",3), new Material("Dragon metal sheet",6))));
        OPTIONS.add(new UpgradeOption("Helm", 2, 6, 96, 86, "Dragon helm",
                Arrays.asList(new Material("Rosewood plank",4), new Material("Dragon metal sheet",8))));

        // Dragon keel (tier 6)
        OPTIONS.add(new UpgradeOption("Keel", 1, 6, 97, 87, "Dragon keel",
                Arrays.asList(new Material("Dragon keel parts",10), new Material("Cupronickel bar",5))));
        OPTIONS.add(new UpgradeOption("Keel", 2, 6, 97, 87, "Dragon keel",
                Arrays.asList(new Material("Large dragon keel parts",16), new Material("Cupronickel bar",5))));

    // Facilities

        // Cargo hold
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 0, 1, 1, "Wooden cargo hold",
                Arrays.asList(new Material("Plank",8), new Material("Bronze nails",32))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 1, 18, 11, "Oak cargo hold",
                Arrays.asList(new Material("Oak plank",8), new Material("Iron nails",32))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 2, 29, 21, "Teak cargo hold",
                Arrays.asList(new Material("Teak plank",8), new Material("Steel nails",32), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 3, 46, 41, "Mahogany cargo hold",
                Arrays.asList(new Material("Mahogany plank",8), new Material("Mithril nails",32), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 4, 60, 53, "Camphor cargo hold",
                Arrays.asList(new Material("Camphor plank",8), new Material("Adamantite nails",32), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 5, 80, 77, "Ironwood cargo hold",
                Arrays.asList(new Material("Ironwood plank",8), new Material("Rune nails",32), new Material("Cupronickel bar",3))));
        OPTIONS.add(new UpgradeOption("Cargo Hold", -1, 6, 89, 84, "Rosewood cargo hold",
                Arrays.asList(new Material("Rosewood plank",8), new Material("Dragon nails",32), new Material("Cupronickel bar",3))));

        // Salvaging hook
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 0, 15, 1, "Bronze salvaging hook",
                Arrays.asList(new Material("Plank",4), new Material("Bronze nails",16), new Material("Bronze bar",6), new Material("Rope",1))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 1, 21, 9, "Iron salvaging hook",
                Arrays.asList(new Material("Oak plank",4), new Material("Iron nails",16), new Material("Iron bar",6), new Material("Rope",1))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 2, 27, 18, "Steel salvaging hook",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel nails",16), new Material("Steel bar",8), new Material("Rope",1), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 3, 44, 30, "Mithril salvaging hook",
                Arrays.asList(new Material("Mahogany plank",4), new Material("Mithril nails",16), new Material("Mithril bar",6), new Material("Rope",1), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 4, 59, 52, "Adamant salvaging hook",
                Arrays.asList(new Material("Camphor plank",4), new Material("Adamantite nails",16), new Material("Adamantite",6), new Material("Rope",1), new Material("Lead bar",3))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 5, 74, 66, "Rune salvaging hook",
                Arrays.asList(new Material("Ironwood plank",4), new Material("Rune nails",16), new Material("Runite bar",6), new Material("Rope",1), new Material("Lead bar",4), new Material("Cupronickel bar",4))));
        OPTIONS.add(new UpgradeOption("Salvaging Hook", -1, 6, 86, 78, "Dragon salvaging hook",
                Arrays.asList(new Material("Rosewood plank",4), new Material("Dragon nails",16), new Material("Dragon metal sheet",6), new Material("Rope",1), new Material("Cupronickel bar",4), new Material("Broken dragon hook",1))));

        // Cannon
        OPTIONS.add(new UpgradeOption("Cannon", -1, 0, 28, 21, "Bronze cannon",
                Arrays.asList(new Material("Plank",4), new Material("Bronze nails",16), new Material("Bronze bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 1, 35, 28, "Iron cannon",
                Arrays.asList(new Material("Oak plank",4), new Material("Iron nails",16), new Material("Iron bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 2, 47, 39, "Steel cannon",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel nails",16), new Material("Steel bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 3, 57, 50, "Mithril cannon",
                Arrays.asList(new Material("Mahogany plank",4), new Material("Mithril nails",16), new Material("Mithril bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 4, 69, 61, "Adamant cannon",
                Arrays.asList(new Material("Camphor plank",4), new Material("Adamantite nails",16), new Material("Adamantite bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 5, 80, 76, "Rune cannon",
                Arrays.asList(new Material("Ironwood plank",4), new Material("Rune nails",16), new Material("Runite bar",8))));
        OPTIONS.add(new UpgradeOption("Cannon", -1, 6, 92, 84, "Dragon cannon",
                Arrays.asList(new Material("Rosewood plank",4), new Material("Dragon nails",16), new Material("Dragon metal sheet",8), new Material("Dragon cannon barrel",1))));

        // Teleport focus
        OPTIONS.add(new UpgradeOption("Teleport Focus", -1, 0, 55, 49, "Teleport focus",
                Arrays.asList(new Material("Mahogany plank",8), new Material("Mithril nails",32), new Material("Lead bar",4), new Material("Magic stone",1))));
        OPTIONS.add(new UpgradeOption("Teleport Focus", -1, 1, 75, 69, "Greater teleport focus",
                Arrays.asList(new Material("Ironwood plank",8), new Material("Rune nails",32), new Material("Cupronickel bar",4), new Material("Magic stone",2), new Material("Bottled storm",1))));

        // Wind/Gale catcher
        OPTIONS.add(new UpgradeOption("Wind Device", -1, 0, 53, 47, "Wind catcher",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel nails",16), new Material("Steel bar",8), new Material("Lead bar",4), new Material("Air rune",10000), new Material("Captured wind mote",1))));
        OPTIONS.add(new UpgradeOption("Wind Device", -1, 1, 79, 70, "Gale catcher",
                Arrays.asList(new Material("Camphor plank",4), new Material("Adamantite nails",16), new Material("Adamantite bar",8), new Material("Cupronickel bar",4), new Material("Air rune",25000), new Material("Captured wind mote",1), new Material("Swift albatross feather",5))));

        // Trawling net
        OPTIONS.add(new UpgradeOption("Trawling Net", -1, 0, 56, 45, "Rope trawling net",
                Arrays.asList(new Material("Rope",7), new Material("Teak plank",4), new Material("Steel bar",4), new Material("Lead bar",2))));
        OPTIONS.add(new UpgradeOption("Trawling Net", 1, 1, 65, 53, "Linen trawling net",
                Arrays.asList(new Material("Linen yarn",6), new Material("Mahogany plank",4), new Material("Rope",1), new Material("Mithril bar",4), new Material("Lead bar",2))));
        OPTIONS.add(new UpgradeOption("Trawling Net", 2, 1, 68, 61, "Linen trawling net",
                Arrays.asList(new Material("Linen yarn",6), new Material("Mahogany plank",4), new Material("Rope",1), new Material("Mithril bar",4), new Material("Lead bar",2))));
        OPTIONS.add(new UpgradeOption("Trawling Net", 1, 2, 79, 73, "Hemp trawling net",
                Arrays.asList(new Material("Hemp yarn",6), new Material("Camphor plank",4), new Material("Rope",1), new Material("Adamantite bar",4), new Material("Cupronickel bar",2), new Material("Ray barbs",4))));
        OPTIONS.add(new UpgradeOption("Trawling Net", 2, 2, 76, 65, "Hemp trawling net",
                Arrays.asList(new Material("Hemp yarn",6), new Material("Camphor plank",4), new Material("Rope",1), new Material("Adamantite bar",4), new Material("Cupronickel bar",2), new Material("Ray barbs",4))));
        OPTIONS.add(new UpgradeOption("Trawling Net", -1, 3, 84, 73, "Cotton trawling net",
                Arrays.asList(new Material("Cotton yarn",6), new Material("Ironwood plank",4), new Material("Rope",1), new Material("Runite bar",4), new Material("Cupronickel bar",2), new Material("Ray barbs",8))));

        // Chum station/spreader
        OPTIONS.add(new UpgradeOption("Chum Station", -1, 0, 56, 45, "Chum station",
                Arrays.asList(new Material("Mahogany plank",10), new Material("Mithril nails",40), new Material("Steel bar",2), new Material("Fishing bait",1000), new Material("Knife",1))));
        OPTIONS.add(new UpgradeOption("Chum Station", -1, 1, 68, 61, "Advanced chum station",
                Arrays.asList(new Material("Camphor plank",10), new Material("Adamantite nails",40), new Material("Steel bar",2), new Material("Fishing bait",1000), new Material("Knife",1))));
        OPTIONS.add(new UpgradeOption("Chum Station", -1, 2, 82, 74, "Chum spreader",
                Arrays.asList(new Material("Ironwood plank",10), new Material("Rune nails",40), new Material("Cupronickel bar",5), new Material("Fishing bait",10000), new Material("Narwhal horn knife",1))));

        // Fathom stone/pearl
        OPTIONS.add(new UpgradeOption("Fathom Device", -1, 0, 70, 62, "Fathom stone",
                Arrays.asList(new Material("Camphor plank",10), new Material("Adamantite nails",40), new Material("Molten glass",4), new Material("Cupronickel bar",2))));
        OPTIONS.add(new UpgradeOption("Fathom Device", -1, 1, 91, 83, "Fathom pearl",
                Arrays.asList(new Material("Rosewood plank",10), new Material("Dragon nails",40), new Material("Dragon metal sheet",2), new Material("Echo pearl",1))));

        // Misc
        OPTIONS.add(new UpgradeOption("Range", -1, 0, 16, 6, "Range",
                Arrays.asList(new Material("Steel bar",4), new Material("Charcoal",2), new Material("Tinderbox",1))));
        OPTIONS.add(new UpgradeOption("Keg", -1, 0, 33, 25, "Keg",
                Arrays.asList(new Material("Oak plank",5), new Material("Iron nails",20), new Material("Barrel stand",1))));
        OPTIONS.add(new UpgradeOption("Anchor", -1, 0, 37, 29, "Anchor",
                Arrays.asList(new Material("Steel bar",8), new Material("Lead bar",6), new Material("Rope",1))));
        OPTIONS.add(new UpgradeOption("Inoculation Station", -1, 0, 40, 37, "Inoculation station",
                Arrays.asList(new Material("Teak plank",8), new Material("Steel nails",32), new Material("Relicym's balm(4)",6))));
        OPTIONS.add(new UpgradeOption("Salvaging Station", -1, 0, 42, 34, "Salvaging station",
                Arrays.asList(new Material("Teak plank",4), new Material("Steel nails",16))));
        OPTIONS.add(new UpgradeOption("Crystal Extractor", -1, 0, 73, 67, "Crystal extractor",
                Arrays.asList(new Material("Ironwood plank",6), new Material("Cupronickel bar",5), new Material("Magic stone",2), new Material("Heart of ithell",1))));
        OPTIONS.add(new UpgradeOption("Eternal Brazier", -1, 0, 78, 72, "Eternal brazier",
                Arrays.asList(new Material("Ironwood plank",4), new Material("Rune nails",16), new Material("Runite bar",6), new Material("Cupronickel bar",6), new Material("Te salt",250), new Material("Efh salt",250), new Material("Urt salt",250))));
    }

    public static List<UpgradeOption> getAvailableOptions(
            int boatType,
            Map<String, Integer> currentTiers,
            int playerSailingLevel,
            int playerConstructionLevel,
            BoatUpgradesConfig config
    )
    {
        List<UpgradeOption> out = new ArrayList<>();

        boolean isRaft = boatType == 0;
        boolean filterConstruction = config.filterConstructionRequirement();

        for (UpgradeOption o : OPTIONS)
        {
            if (o.boatType != -1 && o.boatType != boatType)
            {
                continue;
            }

            if (isRaft && RAFT_EXCLUDED_FACILITIES.contains(o.partName))
            {
                continue;
            }

            Integer cur = currentTiers.get(o.partName);
            int curTier = cur == null ? -1 : cur;

            if (o.targetTier <= curTier)
            {
                continue;
            }

            if (playerSailingLevel < o.requiredSailingLevel)
            {
                continue;
            }

            if (filterConstruction && playerConstructionLevel < o.requiredConstructionLevel)
            {
                continue;
            }

            out.add(o);
        }

        out.sort(Comparator.comparingInt(o -> o.requiredSailingLevel));
        return out;
    }
}