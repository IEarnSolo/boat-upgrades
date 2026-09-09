package com.boatupgrades;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;

@Slf4j
@Singleton
public class MaterialOwnershipService
{
	public enum Status { INVENTORY, STORAGE, MISSING, UNKNOWN }

	private static final String CONFIG_GROUP = "boatupgrades";
	private static final String BANK_KEY = "materialBankItems";
	private static final String GROUP_KEY = "materialGroupStorageItems";
	public static final Color NEUTRAL_COLOR = Color.LIGHT_GRAY;

	private final ConfigManager configManager;
	private final Gson gson;
	private final BoatUpgradesConfig config;
	private volatile Map<Integer, Integer> inventory = Collections.emptyMap();
	private volatile Map<Integer, Integer> bank = Collections.emptyMap();
	private volatile Map<Integer, Integer> groupStorage = Collections.emptyMap();
	private volatile boolean bankKnown;
	private volatile boolean groupStorageKnown;
	private volatile String loadedProfileKey;
	private Item[] pendingGroupStorage;
	private Item[] groupInventoryBeforeEdit;
	private int pendingInventoryEvents;

	@Inject
	public MaterialOwnershipService(ConfigManager configManager, Gson gson, BoatUpgradesConfig config)
	{
		this.configManager = configManager;
		this.gson = gson;
		this.config = config;
	}

	public void validateRegistry()
	{
		Set<String> missing = MaterialItemRegistry.missingCatalogMaterials();
		if (missing.isEmpty()) log.debug("[Materials] Validated item-ID mappings for the complete upgrade catalog");
		else log.error("[Materials] Missing item-ID mappings for {}", missing);
	}

	public void loadProfile()
	{
		String profileKey = configManager.getRSProfileKey();
		if (profileKey == null)
		{
			log.debug("[Materials] RuneScape profile is not available yet; leaving storage caches unknown");
			return;
		}
		if (profileKey.equals(loadedProfileKey)) return;
		loadedProfileKey = profileKey;
		bank = loadSnapshot(BANK_KEY);
		bankKnown = bank != null;
		if (!bankKnown) bank = Collections.emptyMap();
		groupStorage = loadSnapshot(GROUP_KEY);
		groupStorageKnown = groupStorage != null;
		if (!groupStorageKnown) groupStorage = Collections.emptyMap();
		clearPendingGroupEdit();
		log.debug("[Materials] Loaded profile {}: bank known={}, group storage known={}", profileKey, bankKnown, groupStorageKnown);
	}

	public void clearProfile()
	{
		inventory = bank = groupStorage = Collections.emptyMap();
		bankKnown = groupStorageKnown = false;
		loadedProfileKey = null;
		clearPendingGroupEdit();
		log.debug("[Materials] Cleared in-memory ownership state");
	}

	public void updateInventory(Item[] items)
	{
		inventory = quantities(items);
		if (pendingGroupStorage != null && groupInventoryBeforeEdit != null)
		{
			if (sameItems(items, padInventory(groupInventoryBeforeEdit)))
			{
				log.debug("[Materials] Inventory confirmed the pending group-storage transaction");
				commitGroupStorage(pendingGroupStorage);
				clearPendingGroupEdit();
			}
			else if (++pendingInventoryEvents >= 2)
			{
				log.debug("[Materials] Discarding unconfirmed group-storage edit after {} inventory events", pendingInventoryEvents);
				clearPendingGroupEdit();
			}
		}
		log.debug("[Materials] Updated live inventory snapshot with {} distinct items", inventory.size());
	}

	public void updateBank(Item[] items)
	{
		bank = quantities(items); bankKnown = true; saveSnapshot(BANK_KEY, bank);
		log.debug("[Materials] Updated and saved bank snapshot with {} distinct items", bank.size());
	}

	public void updateGroupStorage(Item[] items, boolean hasUncommittedChanges)
	{
		if (hasUncommittedChanges)
		{
			pendingGroupStorage = copy(items);
			log.debug("[Materials] Deferred an uncommitted group-storage snapshot");
		}
		else commitGroupStorage(items);
	}

	public void updateGroupInventoryBeforeEdit(Item[] items)
	{
		groupInventoryBeforeEdit = copy(items); pendingInventoryEvents = 0;
		log.debug("[Materials] Captured inventory state for group-storage transaction confirmation");
	}

	public Status getStatus(String materialName, int requiredQuantity)
	{
		Integer itemId = MaterialItemRegistry.getItemId(materialName);
		if (itemId == null) return Status.UNKNOWN;
		int inventoryQuantity = inventory.getOrDefault(itemId, 0);
		if (inventoryQuantity >= requiredQuantity) return Status.INVENTORY;
		int total = inventoryQuantity + bank.getOrDefault(itemId, 0);
		if (config.includeGroupStorage()) total += groupStorage.getOrDefault(itemId, 0);
		if (total >= requiredQuantity) return Status.STORAGE;
		// Bank observation is sufficient to establish that a material is missing. Group storage is
		// optional supplemental storage: an unseen group-storage container must not leave regular
		// accounts permanently UNKNOWN, but a later observed snapshot still contributes above.
		return bankKnown ? Status.MISSING : Status.UNKNOWN;
	}

	public int getDisplayedOwnedQuantity(String materialName)
	{
		Integer itemId = MaterialItemRegistry.getItemId(materialName);
		if (itemId == null) return 0;
		int total = inventory.getOrDefault(itemId, 0) + bank.getOrDefault(itemId, 0);
		if (config.includeGroupStorage() && groupStorageKnown)
		{
			total += groupStorage.getOrDefault(itemId, 0);
		}
		return total;
	}

	public int getInventoryQuantity(String materialName)
	{
		Integer itemId = MaterialItemRegistry.getItemId(materialName);
		return itemId == null ? 0 : inventory.getOrDefault(itemId, 0);
	}

	public boolean isBankKnown()
	{
		return bankKnown;
	}

	public boolean isGroupStorageIncludedInDisplayedQuantity()
	{
		return config.includeGroupStorage() && groupStorageKnown;
	}

	public boolean hasLoadedProfile()
	{
		return loadedProfileKey != null;
	}

	public Color getColor(String materialName, int requiredQuantity)
	{
		switch (getStatus(materialName, requiredQuantity))
		{
			case INVENTORY: return ColorScheme.PROGRESS_COMPLETE_COLOR;
			case STORAGE: return Color.WHITE;
			case MISSING: return Color.RED;
			default: return NEUTRAL_COLOR;
		}
	}

	public void saveKnownSnapshots()
	{
		if (bankKnown) saveSnapshot(BANK_KEY, bank);
		if (groupStorageKnown) saveSnapshot(GROUP_KEY, groupStorage);
		log.debug("[Materials] Saved known ownership snapshots during lifecycle transition");
	}

	private void commitGroupStorage(Item[] items)
	{
		groupStorage = quantities(items); groupStorageKnown = true; saveSnapshot(GROUP_KEY, groupStorage);
		log.debug("[Materials] Committed and saved group-storage snapshot with {} distinct items", groupStorage.size());
	}

	private void saveSnapshot(String key, Map<Integer, Integer> values)
	{
		if (loadedProfileKey == null) return;
		configManager.setRSProfileConfiguration(CONFIG_GROUP, key, gson.toJson(toArray(values)));
	}

	private Map<Integer, Integer> loadSnapshot(String key)
	{
		String json = configManager.getRSProfileConfiguration(CONFIG_GROUP, key);
		if (json == null) return null;
		try
		{
			int[] values = gson.fromJson(json, int[].class);
			if (values == null || values.length % 2 != 0) throw new JsonSyntaxException("Expected item ID/quantity pairs");
			Map<Integer, Integer> result = new HashMap<>();
			for (int i = 0; i < values.length; i += 2) if (values[i] >= 0 && values[i + 1] > 0) result.merge(values[i], values[i + 1], Integer::sum);
			return result;
		}
		catch (RuntimeException ex)
		{
			log.warn("[Materials] Ignoring malformed {} profile snapshot", key, ex);
			return null;
		}
	}

	private static Map<Integer, Integer> quantities(Item[] items)
	{
		Map<Integer, Integer> result = new HashMap<>();
		if (items != null) for (Item item : items) if (item != null && item.getId() >= 0 && item.getQuantity() > 0) result.merge(item.getId(), item.getQuantity(), Integer::sum);
		return result;
	}

	private static int[] toArray(Map<Integer, Integer> values)
	{
		int[] result = new int[values.size() * 2]; int index = 0;
		for (Map.Entry<Integer, Integer> entry : values.entrySet()) { result[index++] = entry.getKey(); result[index++] = entry.getValue(); }
		return result;
	}

	private static Item[] copy(Item[] items) { return items == null ? null : items.clone(); }
	private static boolean sameItems(Item[] left, Item[] right) { return java.util.Arrays.equals(left, right); }
	private static Item[] padInventory(Item[] items)
	{
		Item[] padded = new Item[28]; int length = Math.min(items.length, padded.length);
		System.arraycopy(items, 0, padded, 0, length);
		for (int i = length; i < padded.length; i++) padded[i] = new Item(-1, 0);
		return padded;
	}
	private void clearPendingGroupEdit() { pendingGroupStorage = null; groupInventoryBeforeEdit = null; pendingInventoryEvents = 0; }
}
