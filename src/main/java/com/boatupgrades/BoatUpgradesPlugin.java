package com.boatupgrades;

import com.google.inject.Provides;
import com.google.common.primitives.Ints;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

@Slf4j
@PluginDescriptor(
	name = "Boat Upgrades",
	description = "Display currently available upgrades for your boat",
	tags = {"sailing", "ship"}
)
public class BoatUpgradesPlugin extends Plugin
{
	@Inject
	private Client client;
	@Inject
	private BoatUpgradesConfig config;
	@Inject
	private BoatUpgradesOverlay boatUpgradesOverlay;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private ChangelogService changelogService;
	@Inject
	private ClientThread clientThread;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(boatUpgradesOverlay);
		log.info("Boat Upgrades started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(boatUpgradesOverlay);
		log.info("Boat Upgrades stopped");
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
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();

		if (state == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(() ->
			{
				changelogService.showChangelogIfNeeded();
			});
		}
	}

	@Provides
	BoatUpgradesConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BoatUpgradesConfig.class);
	}
}