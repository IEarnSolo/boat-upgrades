package com.boatupgrades;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.banktags.BankTagsPlugin;
import net.runelite.client.plugins.banktags.BankTagsService;
import net.runelite.client.plugins.banktags.TagManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.util.Text;

@Slf4j
@Singleton
public class BankMaterialViewService
{
	private static final class PreparedMaterialView
	{
		private final List<Integer> itemIds;
		private final Map<Integer, String> fallbackTooltips;
		private final Map<Integer, String> positionTooltips;
		private final Map<Integer, String> sectionHeaders;
		private final Map<Integer, SectionRequirement> sectionRequirements;

		private PreparedMaterialView(List<Integer> itemIds, Map<Integer, String> fallbackTooltips,
			Map<Integer, String> positionTooltips, Map<Integer, String> sectionHeaders,
			Map<Integer, SectionRequirement> sectionRequirements)
		{
			this.itemIds = itemIds;
			this.fallbackTooltips = fallbackTooltips;
			this.positionTooltips = positionTooltips;
			this.sectionHeaders = sectionHeaders;
			this.sectionRequirements = sectionRequirements;
		}
	}

	public static final class SectionRequirement
	{
		private final String materialName;
		private final int requiredQuantity;

		private SectionRequirement(String materialName, int requiredQuantity)
		{
			this.materialName = materialName;
			this.requiredQuantity = requiredQuantity;
		}

		public String getMaterialName() { return materialName; }
		public int getRequiredQuantity() { return requiredQuantity; }
	}

	public static final class MaterialSection
	{
		private final String name;
		private final Map<String, Integer> materialRequirements;

		public MaterialSection(String name, Map<String, Integer> materialRequirements)
		{
			this.name = name;
			this.materialRequirements = Collections.unmodifiableMap(new LinkedHashMap<>(materialRequirements));
		}

		public String getName() { return name; }
		public Map<String, Integer> getMaterialRequirements() { return materialRequirements; }
	}

	public enum Availability
	{
		AVAILABLE(null),
		BANK_CLOSED("Open your bank to view these materials"),
		BANK_TAGS_INACTIVE("Enable the Bank Tags plugin to view materials in your bank"),
		NO_MATERIALS("No recognized materials are available to view");

		private final String reason;
		Availability(String reason) { this.reason = reason; }
		public String getReason() { return reason; }
	}
	private static final String TEMP_TAG = "boat-upgrades-material-view";
	private static final String LAYOUT_KEY = BankTagsPlugin.TAG_LAYOUT_PREFIX + Text.standardize(TEMP_TAG);
	private static final String BANK_TAGS_ACTIVE_TAB_KEY = "tab";
	private static final int BANK_ITEMS_PER_ROW = BankTagsPlugin.BANK_ITEMS_PER_ROW;

	private final Client client;
	private final ClientThread clientThread;
	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final ItemManager itemManager;
	private final BoatUpgradesConfig config;
	private volatile boolean bankOpen;
	private volatile BankTagsPlugin bankTagsPlugin;
	private volatile String activeViewName;
	private volatile List<Integer> activeItemIds = Collections.emptyList();
	private volatile Map<Integer, String> activePlaceholderTooltips = Collections.emptyMap();
	private volatile Map<Integer, String> activePositionTooltips = Collections.emptyMap();
	private volatile Map<Integer, String> activeSectionHeaders = Collections.emptyMap();
	private volatile Map<Integer, SectionRequirement> activeSectionRequirements = Collections.emptyMap();
	private volatile boolean restoreOnNextBankOpen;
	private volatile boolean activeTotalMaterialsView;

	@Inject
	public BankMaterialViewService(Client client, ClientThread clientThread, ConfigManager configManager,
		PluginManager pluginManager, ItemManager itemManager, BoatUpgradesConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.itemManager = itemManager;
		this.config = config;
	}

	public void startUp()
	{
		clearRememberedTemporaryTab();
		refreshBankTagsIntegration();
		clientThread.invokeLater(() ->
		{
			Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS);
			setBankOpen(bank != null && !bank.isHidden());
		});
		log.debug("[Bank View] Started optional Bank Tags integration discovery");
	}

	public void shutDown()
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		if (plugin != null && pluginManager.isPluginActive(plugin)
			&& activeViewName != null && TEMP_TAG.equals(plugin.getActiveTag()))
		{
			clientThread.invokeLater(plugin::closeBankTag);
		}
		clearRememberedTemporaryTab();
		bankTagsPlugin = null;
		clearTemporaryState();
		bankOpen = false;
		log.debug("[Bank View] Shut down and cleared the temporary integration state");
	}

	public boolean refreshBankTagsIntegration()
	{
		BankTagsPlugin previous = bankTagsPlugin;
		Optional<BankTagsPlugin> active = pluginManager.getPlugins().stream()
			.filter(BankTagsPlugin.class::isInstance).map(BankTagsPlugin.class::cast)
			.filter(pluginManager::isPluginActive).findFirst();
		bankTagsPlugin = active.orElse(null);
		if (bankTagsPlugin == null)
		{
			clearRememberedTemporaryTab();
			clearTemporaryState();
		}
		boolean changed = previous != bankTagsPlugin;
		log.debug("[Bank View] Bank Tags integration active={}", bankTagsPlugin != null);
		return changed;
	}

	public void setBankOpen(boolean open)
	{
		if (bankOpen != open)
		{
			bankOpen = open;
			log.debug("[Bank View] Normal bank open state changed to {}", open);
		}
	}

	public void closeTemporaryView()
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		boolean temporaryViewActive = plugin != null && pluginManager.isPluginActive(plugin)
			&& activeViewName != null && TEMP_TAG.equals(plugin.getActiveTag());
		if (!temporaryViewActive)
		{
			if (activeViewName != null)
			{
				log.debug("[Bank View] Discarded retained temporary state because another bank view was active at close");
				clearTemporaryState();
			}
			return;
		}

		if (config.keepTemporaryBankView())
		{
			restoreOnNextBankOpen = true;
			log.debug("[Bank View] Retained temporary '{}' view for the next bank opening", activeViewName);
			return;
		}

		activePlaceholderTooltips = Collections.emptyMap();
		// WidgetClosed is posted from inside the bank close script. BankTagsService.closeBankTag()
		// requests a bank relayout, so invoking it here would attempt to run a nested client script.
		// Capture the active state now, then clean it up after the current script has completed.
		clientThread.invokeLater(() ->
		{
			if (pluginManager.isPluginActive(plugin))
			{
				plugin.closeBankTag();
				clearTemporaryState();
				log.debug("[Bank View] Closed the temporary material view after the bank close script completed");
			}
		});
		log.debug("[Bank View] Deferred temporary material-view cleanup until after the bank close script");
	}

	public void restoreTemporaryViewIfNeeded()
	{
		if (!restoreOnNextBankOpen || !config.keepTemporaryBankView()) return;
		String displayName = activeViewName;
		List<Integer> itemIds = activeItemIds;
		Map<Integer, String> placeholderTooltips = activePlaceholderTooltips;
		Map<Integer, String> positionTooltips = activePositionTooltips;
		Map<Integer, String> sectionHeaders = activeSectionHeaders;
		Map<Integer, SectionRequirement> sectionRequirements = activeSectionRequirements;
		boolean totalMaterialsView = activeTotalMaterialsView;
		restoreOnNextBankOpen = false;
		clientThread.invokeLater(() ->
		{
			if (!bankOpen || displayName == null || itemIds.isEmpty())
			{
				log.debug("[Bank View] Cancelled retained temporary view restoration because its state is no longer available");
				clearTemporaryState();
				return;
			}
			log.debug("[Bank View] Restoring retained temporary '{}' view after reopening the bank", displayName);
			openMaterialView(displayName, itemIds, placeholderTooltips, positionTooltips, sectionHeaders,
				sectionRequirements, totalMaterialsView);
		});
	}

	public void onKeepTemporaryViewChanged()
	{
		if (!config.keepTemporaryBankView() && !bankOpen && restoreOnNextBankOpen)
		{
			clearRememberedTemporaryTab();
			clearTemporaryState();
			log.debug("[Bank View] Cleared retained temporary view and Bank Tags selection after persistence was disabled");
		}
	}

	public void clearRetainedTemporaryView()
	{
		clearRememberedTemporaryTab();
		if (activeViewName == null) return;
		clearTemporaryState();
		log.debug("[Bank View] Cleared temporary view state at the end of the login session");
	}

	public Availability getAvailability(Collection<String> materialNames)
	{
		if (MaterialItemRegistry.resolveItemIds(materialNames).isEmpty()) return Availability.NO_MATERIALS;
		if (!bankOpen) return Availability.BANK_CLOSED;
		if (bankTagsPlugin == null || !pluginManager.isPluginActive(bankTagsPlugin)) return Availability.BANK_TAGS_INACTIVE;
		return Availability.AVAILABLE;
	}

	public void viewMaterials(String viewName, Map<String, Integer> materialRequirements,
		boolean showRequirementProgress)
	{
		Collection<String> materialNames = materialRequirements.keySet();
		List<Integer> itemIds = MaterialItemRegistry.resolveItemIds(materialNames);
		materialNames.stream().filter(name -> MaterialItemRegistry.getItemId(name) == null)
			.forEach(name -> log.debug("[Bank View] Skipping unmapped material '{}'", name));
		Availability availability = getAvailability(materialNames);
		if (availability != Availability.AVAILABLE)
		{
			log.debug("[Bank View] Rejected material view request: {}", availability);
			return;
		}

		Map<Integer, String> placeholderTooltips = new LinkedHashMap<>();
		Map<Integer, SectionRequirement> sectionRequirements = new LinkedHashMap<>();
		Set<Integer> positionedItemIds = new LinkedHashSet<>();
		materialRequirements.forEach((name, quantity) ->
		{
			Integer itemId = MaterialItemRegistry.getItemId(name);
			if (itemId != null)
			{
				placeholderTooltips.put(itemId, quantity + " x " + JagexColors.MENU_TARGET_TAG + name + "</col>");
				if (showRequirementProgress && positionedItemIds.add(itemId))
				{
					sectionRequirements.put(sectionRequirements.size(), new SectionRequirement(name, quantity));
				}
				log.debug("[Bank View] Prepared orange item-name tooltip for {} x {}", quantity, name);
			}
		});

		String displayName = normalizeViewName(viewName);
		log.debug("[Bank View] Prepared {} individual-upgrade requirement progress positions for '{}'",
			sectionRequirements.size(), displayName);
		clientThread.invoke(() -> openMaterialView(displayName, itemIds, placeholderTooltips,
			Collections.emptyMap(), Collections.emptyMap(), sectionRequirements, false));
	}

	public void viewMaterialSections(String viewName, Map<String, Integer> totalRequirements,
		List<MaterialSection> sections)
	{
		PreparedMaterialView prepared = prepareMaterialSections(totalRequirements, sections);
		List<String> recognizedNames = totalRequirements.keySet().stream()
			.filter(name -> MaterialItemRegistry.getItemId(name) != null).collect(java.util.stream.Collectors.toList());
		Availability availability = getAvailability(recognizedNames);
		if (availability != Availability.AVAILABLE)
		{
			log.debug("[Bank View] Rejected sectioned material view request: {}", availability);
			return;
		}

		String displayName = normalizeViewName(viewName);
		log.debug("[Bank View] Prepared total-material layout with {} aggregate items and {} upgrade sections",
			recognizedNames.size(), prepared.sectionHeaders.size());
		clientThread.invoke(() -> openMaterialView(displayName, prepared.itemIds, prepared.fallbackTooltips,
			prepared.positionTooltips, prepared.sectionHeaders, prepared.sectionRequirements, true));
	}

	private PreparedMaterialView prepareMaterialSections(Map<String, Integer> totalRequirements,
		List<MaterialSection> sections)
	{
		List<Integer> layout = new ArrayList<>();
		Map<Integer, String> sectionHeaders = new LinkedHashMap<>();
		Map<Integer, String> positionTooltips = new LinkedHashMap<>();
		Map<Integer, String> fallbackTooltips = new LinkedHashMap<>();
		Map<Integer, SectionRequirement> sectionRequirements = new LinkedHashMap<>();

		sectionHeaders.put(0, "All materials");
		for (int i = 0; i < BANK_ITEMS_PER_ROW; i++) layout.add(-1);
		appendMaterials(layout, totalRequirements, positionTooltips, fallbackTooltips, null);
		log.debug("[Bank View] Added the All materials header to the aggregate material section");
		for (MaterialSection section : sections)
		{
			while (layout.size() % BANK_ITEMS_PER_ROW != 0) layout.add(-1);
			int headerSlot = layout.size();
			sectionHeaders.put(headerSlot, section.getName());
			for (int i = 0; i < BANK_ITEMS_PER_ROW; i++) layout.add(-1);
			appendMaterials(layout, section.getMaterialRequirements(), positionTooltips, fallbackTooltips,
				sectionRequirements);
		}

		log.debug("[Bank View] Prepared {} per-upgrade material requirement positions", sectionRequirements.size());
		return new PreparedMaterialView(layout, fallbackTooltips, positionTooltips, sectionHeaders,
			sectionRequirements);
	}

	public void refreshActiveTotalMaterialsView(String viewName, Map<String, Integer> totalRequirements,
		List<MaterialSection> sections)
	{
		if (!activeTotalMaterialsView)
		{
			return;
		}

		PreparedMaterialView prepared = prepareMaterialSections(totalRequirements, sections);
		if (MaterialItemRegistry.resolveItemIds(totalRequirements.keySet()).isEmpty())
		{
			log.debug("[Bank View] Closing active total-material view because the active list has no materials");
			closeActiveTotalMaterialsView();
			return;
		}

		String displayName = normalizeViewName(viewName);
		if (!bankOpen)
		{
			if (restoreOnNextBankOpen && config.keepTemporaryBankView())
			{
				storeActiveView(displayName, prepared.itemIds, prepared.fallbackTooltips,
					prepared.positionTooltips, prepared.sectionHeaders, prepared.sectionRequirements, true);
				restoreOnNextBankOpen = true;
				log.debug("[Bank View] Rebuilt retained total-material view '{}' while the bank was closed", displayName);
			}
			return;
		}

		clientThread.invokeLater(() ->
		{
			BankTagsPlugin plugin = bankTagsPlugin;
			if (plugin == null || !pluginManager.isPluginActive(plugin) || !TEMP_TAG.equals(plugin.getActiveTag()))
			{
				log.debug("[Bank View] Stopped tracking the total-material view because the player left the temporary tab");
				clearTemporaryState();
				return;
			}
			openMaterialView(displayName, prepared.itemIds, prepared.fallbackTooltips,
				prepared.positionTooltips, prepared.sectionHeaders, prepared.sectionRequirements, true);
			log.debug("[Bank View] Refreshed open total-material view '{}' after list requirements changed", displayName);
		});
	}

	private void appendMaterials(List<Integer> layout, Map<String, Integer> requirements,
		Map<Integer, String> positionTooltips, Map<Integer, String> fallbackTooltips,
		Map<Integer, SectionRequirement> sectionRequirements)
	{
		for (Map.Entry<String, Integer> requirement : requirements.entrySet())
		{
			Integer itemId = MaterialItemRegistry.getItemId(requirement.getKey());
			if (itemId == null)
			{
				log.debug("[Bank View] Skipping unmapped section material '{}'", requirement.getKey());
				continue;
			}
			String tooltip = requirement.getValue() + " x " + JagexColors.MENU_TARGET_TAG
				+ requirement.getKey() + "</col>";
			positionTooltips.put(layout.size(), tooltip);
			if (sectionRequirements != null)
			{
				sectionRequirements.put(layout.size(), new SectionRequirement(requirement.getKey(), requirement.getValue()));
			}
			fallbackTooltips.put(itemId, tooltip);
			layout.add(itemId);
		}
	}

	private void openMaterialView(String displayName, List<Integer> itemIds, Map<Integer, String> placeholderTooltips,
		Map<Integer, String> positionTooltips, Map<Integer, String> sectionHeaders,
		Map<Integer, SectionRequirement> sectionRequirements, boolean totalMaterialsView)
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		Widget bank = client.getWidget(InterfaceID.Bankmain.ITEMS);
		if (plugin == null || !pluginManager.isPluginActive(plugin) || bank == null || bank.isHidden())
		{
			log.debug("[Bank View] Bank or Bank Tags became unavailable before the material view opened");
			return;
		}

		Set<Integer> acceptedIds = Collections.unmodifiableSet(new LinkedHashSet<>(itemIds));
		Map<Integer, String> canonicalTooltips = new LinkedHashMap<>();
		placeholderTooltips.forEach((itemId, tooltip) -> canonicalTooltips.put(itemManager.canonicalize(itemId), tooltip));
		TagManager tagManager = plugin.getInjector().getInstance(TagManager.class);
		String previousLayout = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, LAYOUT_KEY);
		String layout = itemIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
		try
		{
			tagManager.registerTag(TEMP_TAG, itemId -> acceptedIds.contains(itemManager.canonicalize(itemId)));
			configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, LAYOUT_KEY, layout);
			storeActiveView(displayName, itemIds, canonicalTooltips, positionTooltips, sectionHeaders,
				sectionRequirements, totalMaterialsView);
			restoreOnNextBankOpen = false;
			plugin.openBankTag(TEMP_TAG, BankTagsService.OPTION_HIDE_TAG_NAME);
			applyFriendlyBankTitle();
			log.debug("[Bank View] Opened temporary '{}' bank view for {} material item IDs with {} fake-placeholder tooltips",
				displayName, itemIds.size(), canonicalTooltips.size());
		}
		catch (RuntimeException ex)
		{
			clearTemporaryState();
			log.warn("[Bank View] Failed to open temporary material bank view", ex);
		}
		finally
		{
			tagManager.unregisterTag(TEMP_TAG);
			if (previousLayout == null) configManager.unsetConfiguration(BankTagsPlugin.CONFIG_GROUP, LAYOUT_KEY);
			else configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, LAYOUT_KEY, previousLayout);
			log.debug("[Bank View] Restored temporary Bank Tags registration and layout configuration");
		}
	}

	private void storeActiveView(String displayName, List<Integer> itemIds, Map<Integer, String> placeholderTooltips,
		Map<Integer, String> positionTooltips, Map<Integer, String> sectionHeaders,
		Map<Integer, SectionRequirement> sectionRequirements, boolean totalMaterialsView)
	{
		Map<Integer, String> canonicalTooltips = new LinkedHashMap<>();
		placeholderTooltips.forEach((itemId, tooltip) -> canonicalTooltips.put(itemManager.canonicalize(itemId), tooltip));
		activeViewName = displayName;
		activeItemIds = Collections.unmodifiableList(new ArrayList<>(itemIds));
		activePlaceholderTooltips = Collections.unmodifiableMap(canonicalTooltips);
		activePositionTooltips = Collections.unmodifiableMap(new LinkedHashMap<>(positionTooltips));
		activeSectionHeaders = Collections.unmodifiableMap(new LinkedHashMap<>(sectionHeaders));
		activeSectionRequirements = Collections.unmodifiableMap(new LinkedHashMap<>(sectionRequirements));
		activeTotalMaterialsView = totalMaterialsView;
	}

	private void closeActiveTotalMaterialsView()
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		clearRememberedTemporaryTab();
		clearTemporaryState();
		if (bankOpen && plugin != null && pluginManager.isPluginActive(plugin))
		{
			clientThread.invokeLater(() ->
			{
				if (pluginManager.isPluginActive(plugin) && TEMP_TAG.equals(plugin.getActiveTag()))
				{
					plugin.closeBankTag();
					log.debug("[Bank View] Exited the empty total-material temporary bank view");
				}
			});
		}
	}

	public String getFakePlaceholderTooltip(int itemId, int layoutPosition)
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		if (plugin == null || activeViewName == null || !TEMP_TAG.equals(plugin.getActiveTag()))
		{
			return null;
		}
		String positioned = activePositionTooltips.get(layoutPosition);
		return positioned != null ? positioned : activePlaceholderTooltips.get(itemManager.canonicalize(itemId));
	}

	public Map<Integer, String> getActiveSectionHeaders()
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		if (plugin == null || activeViewName == null || !TEMP_TAG.equals(plugin.getActiveTag()))
		{
			return Collections.emptyMap();
		}
		return activeSectionHeaders;
	}

	public SectionRequirement getActiveSectionRequirement(int layoutPosition)
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		if (plugin == null || activeViewName == null
			|| !TEMP_TAG.equals(plugin.getActiveTag()))
		{
			return null;
		}
		return activeSectionRequirements.get(layoutPosition);
	}

	public void applyFriendlyBankTitle()
	{
		BankTagsPlugin plugin = bankTagsPlugin;
		String displayName = activeViewName;
		if (plugin == null || displayName == null || !TEMP_TAG.equals(plugin.getActiveTag())) return;
		Widget title = client.getWidget(InterfaceID.Bankmain.TITLE);
		if (title != null)
		{
			title.setText(Text.escapeJagex(displayName));
			log.debug("[Bank View] Applied friendly temporary bank title '{}'", displayName);
		}
	}

	private static String normalizeViewName(String viewName)
	{
		String normalized = viewName == null ? "" : viewName.trim();
		return normalized.isEmpty() ? "Upgrade materials" : normalized;
	}

	private void clearTemporaryState()
	{
		activeViewName = null;
		activeItemIds = Collections.emptyList();
		activePlaceholderTooltips = Collections.emptyMap();
		activePositionTooltips = Collections.emptyMap();
		activeSectionHeaders = Collections.emptyMap();
		activeSectionRequirements = Collections.emptyMap();
		restoreOnNextBankOpen = false;
		activeTotalMaterialsView = false;
	}

	private void clearRememberedTemporaryTab()
	{
		String rememberedTab = configManager.getConfiguration(BankTagsPlugin.CONFIG_GROUP, BANK_TAGS_ACTIVE_TAB_KEY);
		if (TEMP_TAG.equals(rememberedTab))
		{
			configManager.setConfiguration(BankTagsPlugin.CONFIG_GROUP, BANK_TAGS_ACTIVE_TAB_KEY, "");
			log.debug("[Bank View] Cleared Bank Tags' remembered temporary material tab");
		}
	}
}
