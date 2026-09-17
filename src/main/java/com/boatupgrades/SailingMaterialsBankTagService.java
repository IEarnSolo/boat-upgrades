package com.boatupgrades;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.plugins.banktags.tabs.Layout;
import net.runelite.client.plugins.banktags.tabs.LayoutManager;
import net.runelite.client.plugins.banktags.tabs.TabManager;
import net.runelite.client.plugins.banktags.tabs.TagTab;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class SailingMaterialsBankTagService
{
	private static final String NEW_TAG_TAB = "New tag tab";
	private static final String IMPORT_OPTION = "Import tag tab " + JagexColors.MENU_TARGET_TAG + "Boat Upgrades</col>";
	private static final String BASE_TAG = "sailing-materials";
	private static final int TAB_ICON_ITEM_ID = ItemID.ROSEWOOD_MAST_AND_COTTON_SAILS;
	private static final int ITEMS_PER_ROW = 8;
	private static final int EXPECTED_MATERIAL_COUNT = 89;

	private static final String[][] LAYOUT_ROWS =
	{
		{"Logs", "Oak logs", "Teak logs", "Mahogany logs", "Camphor logs", "Ironwood logs", "Rosewood logs"},
		{"Plank", "Oak plank", "Teak plank", "Mahogany plank", "Camphor plank", "Ironwood plank", "Rosewood plank"},
		{"Wooden hull parts", "Oak hull parts", "Teak hull parts", "Mahogany hull parts", "Camphor hull parts", "Ironwood hull parts", "Rosewood hull parts"},
		{"Large wooden hull parts", "Large oak hull parts", "Large teak hull parts", "Large mahogany hull parts", "Large camphor hull parts", "Large ironwood hull parts", "Large rosewood hull parts"},
		{},
		{"Bronze bar", "Iron bar", "Steel bar", "Mithril bar", "Adamantite bar", "Runite bar", "Dragon metal sheet"},
		{"Bronze nails", "Iron nails", "Steel nails", "Mithril nails", "Adamantite nails", "Rune nails", "Dragon nails"},
		{"Bronze keel parts", "Iron keel parts", "Steel keel parts", "Mithril keel parts", "Adamant keel parts", "Rune keel parts", "Dragon keel parts"},
		{"Large bronze keel parts", "Large iron keel parts", "Large steel keel parts", "Large mithril keel parts", "Large adamant keel parts", "Large rune keel parts", "Large dragon keel parts"},
		{"Lead bar", "Cupronickel bar", "Broken dragon hook", "Dragon cannon barrel"},
		{},
		{"Bolt of linen", "Bolt of canvas", "Bolt of cotton", "Linen yarn", "Hemp yarn", "Cotton yarn", "Rope"},
		{},
		{"Tinderbox", "Charcoal", "Molten glass", "Swamp tar", "Magic stone"},
		{"Air rune", "Water rune", "Law rune"},
		{"Bottled storm", "Echo pearl", "Swift albatross feather"},
		{"Barrel stand", "Captured wind mote", "Heart of ithell"},
		{"Knife", "Narwhal horn knife", "Fishing bait", "Ray barbs"},
		{"Te salt", "Efh salt", "Urt salt", "Relicym's balm(4)"}
	};
	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final PluginManager pluginManager;

	@Inject
	public SailingMaterialsBankTagService(Client client, ClientThread clientThread,
		ConfigManager configManager, PluginManager pluginManager)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.pluginManager = pluginManager;
	}

	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!NEW_TAG_TAB.equals(event.getOption()))
		{
			return;
		}

		Optional<BankTagsPlugin> bankTags = findActiveBankTagsPlugin();
		if (!bankTags.isPresent())
		{
			log.debug("[Sailing Tag] Ignored New tag tab entry because Bank Tags is inactive");
			return;
		}

		client.createMenuEntry(-1)
			.setOption(IMPORT_OPTION)
			.setTarget(event.getTarget())
			.setType(MenuAction.RUNELITE_LOW_PRIORITY)
			.setIdentifier(event.getIdentifier())
			.setParam0(event.getActionParam0())
			.setParam1(event.getActionParam1())
			.onClick(entry -> clientThread.invokeLater(this::install));
		//log.debug("[Sailing Tag] Added '{}' beside the Bank Tags new-tab action", IMPORT_OPTION);
	}

	private void install()
	{
		Optional<BankTagsPlugin> optionalPlugin = findActiveBankTagsPlugin();
		if (!optionalPlugin.isPresent())
		{
			notifyPlayer("Enable the Bank Tags plugin before importing the Boat Upgrades tag tab.");
			log.debug("[Sailing Tag] Aborted import because Bank Tags became inactive");
			return;
		}

		BankTagsPlugin plugin = optionalPlugin.get();
		TagManager tagManager = plugin.getInjector().getInstance(TagManager.class);
		TabManager tabManager = plugin.getInjector().getInstance(TabManager.class);
		LayoutManager layoutManager = plugin.getInjector().getInstance(LayoutManager.class);
		Map<Integer, Integer> layoutEntries;
		try
		{
			layoutEntries = validateAndBuildLayout();
		}
		catch (IllegalStateException ex)
		{
			notifyPlayer("Unable to import the Boat Upgrades tag tab because its material layout is invalid.");
			log.error("[Sailing Tag] Material-layout validation failed", ex);
			return;
		}

		String tag = findAvailableTagName(tabManager, tagManager);
		boolean tabAdded = false;
		try
		{
			for (Integer itemId : layoutEntries.values())
			{
				tagManager.addTag(itemId, tag, false);
				log.debug("[Sailing Tag] Appended tag '{}' to material item {}", tag, itemId);
			}

			Layout layout = new Layout(tag);
			layoutEntries.forEach((position, itemId) -> layout.setItemAtPos(itemId, position));
			layoutManager.saveLayout(layout);
			log.debug("[Sailing Tag] Saved {} positioned items across {} eight-slot rows", layoutEntries.size(), LAYOUT_ROWS.length);

			TagTab tab = new TagTab();
			tab.setTag(tag);
			tab.setIconItemId(TAB_ICON_ITEM_ID);
			tabManager.add(tab);
			tabAdded = true;
			tabManager.save();

			plugin.openBankTag(tag);
			notifyPlayer("Imported Boat Upgrades tag tab '" + tag + "'.");
			log.debug("[Sailing Tag] Imported and opened permanent tag tab '{}' with icon {}", tag, TAB_ICON_ITEM_ID);
		}
		catch (RuntimeException ex)
		{
			rollback(tag, tabAdded, plugin, tagManager, tabManager, layoutManager);
			notifyPlayer("Failed to import the Boat Upgrades tag tab; partial changes were removed.");
			log.error("[Sailing Tag] Import failed and was rolled back for '{}'", tag, ex);
		}
	}

	private Map<Integer, Integer> validateAndBuildLayout()
	{
		Set<String> catalogMaterials = new LinkedHashSet<>();
		UpgradeData.getAllOptions().forEach(option -> option.materials.forEach(material -> catalogMaterials.add(material.name)));

		Map<Integer, Integer> layout = new LinkedHashMap<>();
		Set<String> layoutNames = new HashSet<>();
		List<String> duplicateNames = new ArrayList<>();
		List<String> unmappedNames = new ArrayList<>();
		for (int row = 0; row < LAYOUT_ROWS.length; row++)
		{
			if (LAYOUT_ROWS[row].length > ITEMS_PER_ROW)
			{
				throw new IllegalStateException("Layout row " + (row + 1) + " exceeds eight slots");
			}
			for (int column = 0; column < LAYOUT_ROWS[row].length; column++)
			{
				String materialName = LAYOUT_ROWS[row][column];
				if (materialName == null)
				{
					log.debug("[Sailing Tag] Preserved empty layout slot at row {}, column {}", row + 1, column + 1);
					continue;
				}
				if (!layoutNames.add(materialName)) duplicateNames.add(materialName);
				Integer itemId = MaterialItemRegistry.getItemId(materialName);
				if (itemId == null) unmappedNames.add(materialName);
				else layout.put(row * ITEMS_PER_ROW + column, itemId);
			}
		}

		Set<String> missingNames = new LinkedHashSet<>(catalogMaterials);
		missingNames.removeAll(layoutNames);
		Set<String> extraNames = new LinkedHashSet<>(layoutNames);
		extraNames.removeAll(catalogMaterials);
		if (catalogMaterials.size() != EXPECTED_MATERIAL_COUNT || layoutNames.size() != EXPECTED_MATERIAL_COUNT
			|| !duplicateNames.isEmpty() || !unmappedNames.isEmpty() || !missingNames.isEmpty() || !extraNames.isEmpty())
		{
			throw new IllegalStateException("catalog=" + catalogMaterials.size() + ", layout=" + layoutNames.size()
				+ ", duplicates=" + duplicateNames + ", unmapped=" + unmappedNames
				+ ", missing=" + missingNames + ", extra=" + extraNames);
		}

		log.debug("[Sailing Tag] Validated all {} catalog materials exactly once", EXPECTED_MATERIAL_COUNT);
		return layout;
	}

	private String findAvailableTagName(TabManager tabManager, TagManager tagManager)
	{
		String candidate = BASE_TAG;
		int suffix = 2;
		while (tagExists(candidate, tabManager, tagManager))
		{
			log.debug("[Sailing Tag] Tag name '{}' is already occupied; trying a numbered copy", candidate);
			candidate = BASE_TAG + "-" + suffix++;
		}
		log.debug("[Sailing Tag] Selected unused permanent tag name '{}'", candidate);
		return candidate;
	}

	private boolean tagExists(String tag, TabManager tabManager, TagManager tagManager)
	{
		String standardized = Text.standardize(tag);
		return tabManager.find(standardized) != null
			|| !tagManager.getItemsForTag(standardized).isEmpty()
			|| configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_LAYOUT_PREFIX + standardized) != null
			|| configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + standardized) != null
			|| configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, "hidden_" + standardized) != null;
	}

	private void rollback(String tag, boolean tabAdded, BankTagsPlugin plugin, TagManager tagManager,
		TabManager tabManager, LayoutManager layoutManager)
	{
		try
		{
			if (tag.equals(plugin.getActiveTag())) plugin.closeBankTag();
			tagManager.removeTag(tag);
			layoutManager.removeLayout(tag);
			if (tabAdded) tabManager.remove(tag);
			tabManager.save();
			configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, BankTagsPlugin.TAG_ICON_PREFIX + Text.standardize(tag));
			log.debug("[Sailing Tag] Rolled back item tags, layout, icon, and tab for '{}'", tag);
		}
		catch (RuntimeException rollbackError)
		{
			log.error("[Sailing Tag] Rollback also failed for '{}'", tag, rollbackError);
		}
	}

	private Optional<BankTagsPlugin> findActiveBankTagsPlugin()
	{
		return pluginManager.getPlugins().stream()
			.filter(BankTagsPlugin.class::isInstance)
			.map(BankTagsPlugin.class::cast)
			.filter(pluginManager::isPluginActive)
			.findFirst();
	}

	private void notifyPlayer(String message)
	{
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
	}
}
