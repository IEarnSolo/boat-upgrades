package com.boatupgrades;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.runelite.api.ItemID;

final class MaterialItemRegistry
{
	private static final Map<String, Integer> ITEM_IDS;

	static
	{
		Map<String, Integer> ids = new LinkedHashMap<>();
		ids.put("Adamant keel parts", ItemID.ADAMANT_KEEL_PARTS); ids.put("Adamantite bar", ItemID.ADAMANTITE_BAR);
		ids.put("Adamantite nails", ItemID.ADAMANTITE_NAILS); ids.put("Air rune", ItemID.AIR_RUNE);
		ids.put("Barrel stand", ItemID.BARREL_STAND); ids.put("Bolt of canvas", ItemID.BOLT_OF_CANVAS);
		ids.put("Bolt of cotton", ItemID.BOLT_OF_COTTON); ids.put("Bolt of linen", ItemID.BOLT_OF_LINEN);
		ids.put("Bottled storm", ItemID.BOTTLED_STORM); ids.put("Broken dragon hook", ItemID.BROKEN_DRAGON_HOOK);
		ids.put("Bronze bar", ItemID.BRONZE_BAR); ids.put("Bronze keel parts", ItemID.BRONZE_KEEL_PARTS);
		ids.put("Bronze nails", ItemID.BRONZE_NAILS); ids.put("Camphor hull parts", ItemID.CAMPHOR_HULL_PARTS);
		ids.put("Camphor logs", ItemID.CAMPHOR_LOGS); ids.put("Camphor plank", ItemID.CAMPHOR_PLANK);
		ids.put("Captured wind mote", ItemID.CAPTURED_WIND_MOTE); ids.put("Charcoal", ItemID.CHARCOAL);
		ids.put("Cotton yarn", ItemID.COTTON_YARN); ids.put("Cupronickel bar", ItemID.CUPRONICKEL_BAR);
		ids.put("Dragon cannon barrel", ItemID.DRAGON_CANNON_BARREL); ids.put("Dragon keel parts", ItemID.DRAGON_KEEL_PARTS);
		ids.put("Dragon metal sheet", ItemID.DRAGON_METAL_SHEET); ids.put("Dragon nails", ItemID.DRAGON_NAILS);
		ids.put("Echo pearl", ItemID.ECHO_PEARL); ids.put("Efh salt", ItemID.EFH_SALT);
		ids.put("Fishing bait", ItemID.FISHING_BAIT); ids.put("Heart of ithell", ItemID.HEART_OF_ITHELL);
		ids.put("Hemp yarn", ItemID.HEMP_YARN); ids.put("Iron bar", ItemID.IRON_BAR);
		ids.put("Iron keel parts", ItemID.IRON_KEEL_PARTS); ids.put("Iron nails", ItemID.IRON_NAILS);
		ids.put("Ironwood hull parts", ItemID.IRONWOOD_HULL_PARTS); ids.put("Ironwood logs", ItemID.IRONWOOD_LOGS);
		ids.put("Ironwood plank", ItemID.IRONWOOD_PLANK); ids.put("Knife", ItemID.KNIFE);
		ids.put("Law rune", ItemID.LAW_RUNE);
		ids.put("Large adamant keel parts", ItemID.LARGE_ADAMANT_KEEL_PARTS); ids.put("Large bronze keel parts", ItemID.LARGE_BRONZE_KEEL_PARTS);
		ids.put("Large camphor hull parts", ItemID.LARGE_CAMPHOR_HULL_PARTS); ids.put("Large dragon keel parts", ItemID.LARGE_DRAGON_KEEL_PARTS);
		ids.put("Large iron keel parts", ItemID.LARGE_IRON_KEEL_PARTS); ids.put("Large ironwood hull parts", ItemID.LARGE_IRONWOOD_HULL_PARTS);
		ids.put("Large mahogany hull parts", ItemID.LARGE_MAHOGANY_HULL_PARTS); ids.put("Large mithril keel parts", ItemID.LARGE_MITHRIL_KEEL_PARTS);
		ids.put("Large oak hull parts", ItemID.LARGE_OAK_HULL_PARTS); ids.put("Large rosewood hull parts", ItemID.LARGE_ROSEWOOD_HULL_PARTS);
		ids.put("Large rune keel parts", ItemID.LARGE_RUNE_KEEL_PARTS); ids.put("Large steel keel parts", ItemID.LARGE_STEEL_KEEL_PARTS);
		ids.put("Large teak hull parts", ItemID.LARGE_TEAK_HULL_PARTS); ids.put("Large wooden hull parts", ItemID.LARGE_WOODEN_HULL_PARTS);
		ids.put("Lead bar", ItemID.LEAD_BAR); ids.put("Linen yarn", ItemID.LINEN_YARN); ids.put("Logs", ItemID.LOGS);
		ids.put("Magic stone", ItemID.MAGIC_STONE_8788); ids.put("Mahogany hull parts", ItemID.MAHOGANY_HULL_PARTS);
		ids.put("Mahogany logs", ItemID.MAHOGANY_LOGS); ids.put("Mahogany plank", ItemID.MAHOGANY_PLANK);
		ids.put("Mithril bar", ItemID.MITHRIL_BAR); ids.put("Mithril keel parts", ItemID.MITHRIL_KEEL_PARTS);
		ids.put("Mithril nails", ItemID.MITHRIL_NAILS); ids.put("Molten glass", ItemID.MOLTEN_GLASS);
		ids.put("Narwhal horn knife", ItemID.NARWHAL_HORN_KNIFE); ids.put("Oak hull parts", ItemID.OAK_HULL_PARTS);
		ids.put("Oak logs", ItemID.OAK_LOGS); ids.put("Oak plank", ItemID.OAK_PLANK); ids.put("Plank", ItemID.PLANK);
		ids.put("Ray barbs", ItemID.RAY_BARBS); ids.put("Relicym's balm(4)", ItemID.RELICYMS_BALM4);
		ids.put("Rope", ItemID.ROPE); ids.put("Rosewood hull parts", ItemID.ROSEWOOD_HULL_PARTS);
		ids.put("Rosewood logs", ItemID.ROSEWOOD_LOGS); ids.put("Rosewood plank", ItemID.ROSEWOOD_PLANK);
		ids.put("Rune keel parts", ItemID.RUNE_KEEL_PARTS); ids.put("Rune nails", ItemID.RUNE_NAILS);
		ids.put("Runite bar", ItemID.RUNITE_BAR); ids.put("Steel bar", ItemID.STEEL_BAR);
		ids.put("Steel keel parts", ItemID.STEEL_KEEL_PARTS); ids.put("Steel nails", ItemID.STEEL_NAILS);
		ids.put("Swamp tar", ItemID.SWAMP_TAR); ids.put("Swift albatross feather", ItemID.SWIFT_ALBATROSS_FEATHER);
		ids.put("Te salt", ItemID.TE_SALT); ids.put("Teak hull parts", ItemID.TEAK_HULL_PARTS);
		ids.put("Teak logs", ItemID.TEAK_LOGS); ids.put("Teak plank", ItemID.TEAK_PLANK);
		ids.put("Tinderbox", ItemID.TINDERBOX); ids.put("Urt salt", ItemID.URT_SALT);
		ids.put("Water rune", ItemID.WATER_RUNE);
		ids.put("Wooden hull parts", ItemID.WOODEN_HULL_PARTS);
		ITEM_IDS = Collections.unmodifiableMap(ids);
	}

	static Integer getItemId(String materialName) { return ITEM_IDS.get(materialName); }

	static List<Integer> resolveItemIds(Collection<String> materialNames)
	{
		LinkedHashSet<Integer> resolved = new LinkedHashSet<>();
		for (String materialName : materialNames)
		{
			Integer itemId = ITEM_IDS.get(materialName);
			if (itemId != null) resolved.add(itemId);
		}
		return new ArrayList<>(resolved);
	}

	static Set<String> missingCatalogMaterials()
	{
		return UpgradeData.getAllOptions().stream().flatMap(o -> o.materials.stream()).map(m -> m.name)
			.filter(name -> !ITEM_IDS.containsKey(name)).collect(Collectors.toSet());
	}

	private MaterialItemRegistry() {}
}
