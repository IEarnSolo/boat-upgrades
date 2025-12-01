package com.boatupgrades;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("boatupgrades")
public interface BoatUpgradesConfig extends Config
{
	@ConfigItem(
			keyName = "persistMinutes",
			name = "Persist overlay",
			description = "Number of minutes the overlay persists after disembarking your boat/leaving shipyard",
			position = 0
	)
	default int persistMinutes()
	{
		return 20;
	}

	@ConfigItem(
			keyName = "comingSoon",
			name = "Boat facilities coming soon",
			description = "Boat facilities coming soon",
			position = 99
	)
	default boolean comingSoonNotice()
	{
		return true;
	}
	@ConfigItem(
			keyName = "lastSeenChangelogVersion",
			name = "lastSeenChangelogVersion",
			description = "",
			hidden = true
			//position = 800
	)
	default String lastSeenChangelogVersion()
	{
		return "";
	}

	@ConfigItem(
			keyName = "lastSeenChangelogVersion",
			name = "",
			description = "",
			hidden = true
	)
	void setLastSeenChangelogVersion(String value);
}