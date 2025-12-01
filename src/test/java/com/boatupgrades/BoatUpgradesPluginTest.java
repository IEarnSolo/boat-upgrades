package com.boatupgrades;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BoatUpgradesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BoatUpgradesPlugin.class);
		RuneLite.main(args);
	}
}