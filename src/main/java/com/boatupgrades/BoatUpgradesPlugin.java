package com.boatupgrades;

import com.boatupgrades.utils.SchematicUtils;
import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemContainer;
import net.runelite.api.ScriptID;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
	name = "Boat Upgrades",
	description = "Display currently available upgrades for your boat when you board or enter the shipyard; create upgrade lists; find materials instantly in your bank",
	tags = {"sailing", "ship", "facility", "pimp my ride"}
)
public class BoatUpgradesPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private BoatUpgradesConfig config;
	private BoatUpgradesOverlay boatUpgradesOverlay;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ChangelogService changelogService;
	@Inject
	private ClientThread clientThread;
	@Inject
	private EventBus eventBus;
	@Inject
	private FacilityService facilityService;
	@Inject
	private SchematicUtils schematicUtils;
	@Inject
	private MyUpgradeListService myUpgradeListService;
	@Inject
	private AvailableUpgradesService availableUpgradesService;
	@Inject
	private MaterialOwnershipService materialOwnershipService;
	@Inject
	private BankMaterialViewService bankMaterialViewService;
	@Inject
	private BankMaterialPlaceholderOverlay bankMaterialPlaceholderOverlay;
	@Inject
	private BankMaterialSectionOverlay bankMaterialSectionOverlay;
	@Inject
	private BankMaterialRequirementOverlay bankMaterialRequirementOverlay;
	@Inject
	private SailingMaterialsBankTagService sailingMaterialsBankTagService;
	@Inject
	private ClientToolbar clientToolbar;
	private BoatUpgradesPanel panel;
	private NavigationButton navButton;

	private long lastAccountHash = -1;

	@Override
	protected void startUp() throws Exception
	{
		// Inject after startup so that the BoatUpgradesPanel dependency is created
		// after default SwingUI styling is applied
		if (boatUpgradesOverlay == null) boatUpgradesOverlay = injector.getInstance(BoatUpgradesOverlay.class);
		overlayManager.add(boatUpgradesOverlay);
		overlayManager.add(bankMaterialPlaceholderOverlay);
		overlayManager.add(bankMaterialSectionOverlay);
		overlayManager.add(bankMaterialRequirementOverlay);
		facilityService.start();
		materialOwnershipService.validateRegistry();
		bankMaterialViewService.startUp();
		log.info("Boat Upgrades started");

		panel = injector.getInstance(BoatUpgradesPanel.class);
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");


		navButton = NavigationButton.builder()
				.tooltip("Boat Upgrades")
				.icon(icon)
				.panel(panel)
				.priority(config.panelPosition())
				.build();

		clientToolbar.addNavigation(navButton);
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() != GameState.LOGGED_IN) return;
			schematicUtils.refreshCachedUnlockStates();
			materialOwnershipService.loadProfile();
			ItemContainer inventory = client.getItemContainer(InventoryID.INV);
			if (inventory != null) materialOwnershipService.updateInventory(inventory.getItems());
			if (panel != null)
			{
				panel.refreshMaterialColors();
				panel.refreshSchematicColors();
				panel.refreshViewInBankButtons();
			}
			log.debug("[Materials] Initialized ownership state while the plugin was started in game");
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		materialOwnershipService.saveKnownSnapshots();
		materialOwnershipService.clearProfile();
		schematicUtils.clearCachedUnlockStates();
		bankMaterialViewService.shutDown();
		overlayManager.remove(boatUpgradesOverlay);
		overlayManager.remove(bankMaterialPlaceholderOverlay);
		overlayManager.remove(bankMaterialSectionOverlay);
		overlayManager.remove(bankMaterialRequirementOverlay);
		facilityService.stop();
		log.info("Boat Upgrades stopped");

		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted event)
	{
		if (event.getCommand().equalsIgnoreCase("boatvars"))
		{
			int[] values = new int[]
			{
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT0),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT1),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT2),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT3),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT4),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT5),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT6),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT7),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT8),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT9),
					client.getVarbitValue(VarbitID.SAILING_SIDEPANEL_FACILITY_HOTSPOT10)
			};

			StringBuilder sb = new StringBuilder();
			sb.append("Hotspot Varbits:");
			for (int i = 0; i < values.length; i++)
			{
				sb.append(System.lineSeparator()).append("Hotspot ").append(i).append(": ").append(values[i]);
			}

			final String output = sb.toString();

			try
			{
				StringSelection selection = new StringSelection(output);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);

				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Hotspot varbits copied to clipboard!", null);
				log.info("Hotspot varbits copied to clipboard!");
				log.info("Hotspot varbits:\n{}", output);
			}
			catch (Throwable ex)
			{
				log.warn("Failed to copy hotspot varbits to clipboard", ex);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"boatupgrades".equals(event.getGroup()))
		{
			return;
		}

		if ("persistMinutes".equals(event.getKey()))
		{
			boatUpgradesOverlay.refreshCacheExpiry();
		}
		if ("filterSchematicRequirement".equals(event.getKey()) || "filterConstructionRequirement".equals(event.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				panel.onAvailableUpgradesChanged();
			});
		}
		if ("coloredItemRequirements".equals(event.getKey()) || "includeGroupStorage".equals(event.getKey()))
		{
			log.debug("[Materials] Ownership display configuration changed: {}", event.getKey());
			if (panel != null) panel.refreshMaterialColors();
		}
		if ("showBankRequirementProgress".equals(event.getKey()))
		{
			log.debug("[Bank View] Bank upgrade-section requirement progress enabled={}",
				config.showBankRequirementProgress());
		}
		if ("keepTemporaryBankView".equals(event.getKey()))
		{
			bankMaterialViewService.onKeepTemporaryViewChanged();
			log.debug("[Bank View] Keep filtered view setting changed to {}", config.keepTemporaryBankView());
		}
		if (event.getKey().equals("panelPosition"))
		{
			panel = injector.getInstance(BoatUpgradesPanel.class);
			final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

				clientToolbar.removeNavigation(navButton);

				navButton = NavigationButton.builder()
						.tooltip("Boat Upgrades")
						.icon(icon)
						.panel(panel)
						.priority(config.panelPosition())
						.build();

				clientToolbar.addNavigation(navButton);
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();

		if (state == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				schematicUtils.refreshCachedUnlockStates();
				materialOwnershipService.loadProfile();
				ItemContainer inventory = client.getItemContainer(InventoryID.INV);
				if (inventory != null) materialOwnershipService.updateInventory(inventory.getItems());
				boolean accountChanged = client.getAccountHash() != lastAccountHash;
				if (accountChanged)
				{
					lastAccountHash = client.getAccountHash();
					availableUpgradesService.reset();
					log.debug("Reloading Lists selections for account hash {}", lastAccountHash);
					myUpgradeListService.load();
				}
				if (panel != null)
				{
					if (accountChanged)
					{
						panel.onListsReloaded();
						panel.onAvailableUpgradesChanged();
					}
					panel.refreshMaterialColors();
					panel.refreshSchematicColors();
				}
				if (accountChanged) changelogService.showChangelogIfNeeded();
			});
		}
		else if (state == GameState.LOGIN_SCREEN)
		{
			materialOwnershipService.saveKnownSnapshots();
			materialOwnershipService.clearProfile();
			schematicUtils.clearCachedUnlockStates();
			bankMaterialViewService.setBankOpen(false);
			bankMaterialViewService.clearRetainedTemporaryView();
			if (panel != null)
			{
				panel.refreshMaterialColors();
				panel.refreshSchematicColors();
				panel.refreshViewInBankButtons();
				log.debug("[Requirements] Reset side-panel material and schematic colors after logout");
			}
			lastAccountHash = -1;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		int containerId = event.getContainerId();
		if (containerId == InventoryID.INV)
		{
			materialOwnershipService.updateInventory(event.getItemContainer().getItems());
		}
		else if (containerId == InventoryID.BANK)
		{
			materialOwnershipService.updateBank(event.getItemContainer().getItems());
			bankMaterialViewService.setBankOpen(true);
		}
		else if (containerId == InventoryID.INV_GROUP_TEMP)
		{
			boolean edited = client.getVarbitValue(VarbitID.GIM_SHARED_BANK_HASEDITED) == 1;
			materialOwnershipService.updateGroupStorage(event.getItemContainer().getItems(), edited);
		}
		else if (containerId == InventoryID.INV_PLAYER_TEMP)
		{
			materialOwnershipService.updateGroupInventoryBeforeEdit(event.getItemContainer().getItems());
		}
		else return;

		if (panel != null)
		{
			panel.refreshMaterialColors();
			if (containerId == InventoryID.BANK) panel.refreshViewInBankButtons();
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN)
		{
			bankMaterialViewService.setBankOpen(true);
			bankMaterialViewService.restoreTemporaryViewIfNeeded();
			if (panel != null) panel.refreshViewInBankButtons();
		}
	}

	// Bank Tags clears its active-tag information during its onWidgetClosed handler
	// Boat Upgrades first needs to check whether the active tag is the temporary one
	@Subscribe(priority = 1.0f)
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == InterfaceID.BANKMAIN && event.isUnload())
		{
			bankMaterialViewService.closeTemporaryView();
			bankMaterialViewService.setBankOpen(false);
			if (panel != null) panel.refreshViewInBankButtons();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		sailingMaterialsBankTagService.onMenuEntryAdded(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (schematicUtils.refreshCachedUnlockState(event.getVarbitId()) && panel != null)
		{
			panel.refreshSchematicColors();
			log.debug("[Schematics] Requested targeted side-panel refresh for varbit {}", event.getVarbitId());
		}
	}

	// Bank Tags restores its internal tag name whenever the bank layout is rebuilt.
	// Run after that handler so temporary material views retain their friendly title.
	@Subscribe(priority = -1.0f)
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == ScriptID.BANKMAIN_FINISHBUILDING)
		{
			bankMaterialViewService.applyFriendlyBankTitle();
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() instanceof net.runelite.client.plugins.banktags.BankTagsPlugin)
		{
			bankMaterialViewService.refreshBankTagsIntegration();
			if (panel != null) panel.refreshViewInBankButtons();
			log.debug("[Bank View] Refreshed optional integration after Bank Tags plugin state changed");
		}
	}

	@Provides
	BoatUpgradesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoatUpgradesConfig.class);
	}
}
