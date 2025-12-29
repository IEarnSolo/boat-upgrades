package com.boatupgrades;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("boatupgrades")
public interface BoatUpgradesConfig extends Config
{

	@ConfigSection(
			name = "Text overlay",
			description = "Toggle for text overlay visibility",
			position = 100
	)
	String overlaySection = "overlaySection";

	@ConfigSection(
			name = "Side panel",
			description = "Side panel position",
			position = 200
	)
	String sidePanelSection = "sidePanelSection";

	@ConfigSection(
			name = "Core boat parts",
			description = "Toggle which core boat parts are shown in the overlay",
			position = 300
	)
	String coreBoatPartsSection = "coreBoatPartsSection";

	@ConfigSection(
			name = "Boat facilities",
			description = "Toggle which boat facilities are shown in the overlay",
			position = 400
	)
	String facilitiesSection = "facilitiesSection";

	@ConfigItem(
			keyName = "displayOverlay",
			name = "Display overlay",
			description = "Enable to display the overlay when boarding your boat or entering shipyard - disable to hide it",
			section = overlaySection
	)
	default boolean displayOverlay()
	{
		return true;
	}
	@ConfigItem(
			keyName = "filterConstructionRequirement",
			name = "Filter construction requirement",
			description = "Enable to hide upgrades that you don't meet the construction level for",
			section = overlaySection
	)
	default boolean filterConstructionRequirement()
	{
		return false;
	}

	@ConfigItem(
			keyName = "filterSchematicRequirement",
			name = "Filter schematic requirement",
			description = "Enable to hide upgrades that you haven't discovered the schematic for",
			section = overlaySection
	)
	default boolean filterSchematicRequirement()
	{
		return false;
	}

	@ConfigItem(
			keyName = "hideLowerTiers",
			name = "Hide lower tiers",
			description = "Hide lower tier upgrades from the overlay when a higher tier upgrade is available for the same boat part or facility",
			section = overlaySection
	)
	default boolean hideLowerTiers()
	{
		return false;
	}

	@ConfigItem(
			keyName = "persistMinutes",
			name = "Persist overlay",
			description = "Number of minutes the overlay persists after disembarking your boat/leaving shipyard (0 will hide it immediately)",
			section = overlaySection
	)
	default int persistMinutes()
	{
		return 10;
	}

	@ConfigItem(
			keyName = "overlayOnlyInShipyard",
			name = "Show only in shipyard",
			description = "Show the text overlay only when you are in the shipyard",
			section = overlaySection
	)
	default boolean overlayOnlyInShipyard()
	{
		return false;
	}

	@ConfigItem(
			keyName = "panelPosition",
			name = "Side panel position",
			description = "Specify where the Boat Upgrades panel appears in the sidebar (lower = higher up)",
			section = sidePanelSection
	)
	default int panelPosition()
	{
		return 5;
	}

	@ConfigItem(
			keyName = "showBaseHull",
			name = "Base/Hull",
			description = "Show base/hull upgrades",
			section = coreBoatPartsSection
	)
	default boolean showBaseHull() { return true; }

	@ConfigItem(
			keyName = "showHelm",
			name = "Helm",
			description = "Show helm upgrades",
			section = coreBoatPartsSection
	)
	default boolean showHelm() { return true; }

	@ConfigItem(
			keyName = "showSails",
			name = "Sails",
			description = "Show sails upgrades",
			section = coreBoatPartsSection
	)
	default boolean showSails() { return true; }

	@ConfigItem(
			keyName = "showKeel",
			name = "Keel",
			description = "Show keel upgrades",
			section = coreBoatPartsSection
	)
	default boolean showKeel() { return true; }

	@ConfigItem(
			keyName = "showSalvagingHook",
			name = "Salvaging hook",
			description = "Show Salvaging Hook upgrades",
			section = facilitiesSection
	)
	default boolean showSalvagingHook() { return false; }

	@ConfigItem(
			keyName = "showCargoHold",
			name = "Cargo hold",
			description = "Show Cargo hold upgrades",
			section = facilitiesSection
	)
	default boolean showCargoHold() { return false; }

	@ConfigItem(
			keyName = "showCannon",
			name = "Cannon",
			description = "Show Cannon upgrades",
			section = facilitiesSection
	)
	default boolean showCannon() { return false; }

	@ConfigItem(
			keyName = "showTeleportFocus",
			name = "Teleport focus",
			description = "Show Teleport focus upgrades",
			section = facilitiesSection
	)
	default boolean showTeleportFocus() { return false; }

	@ConfigItem(
			keyName = "showWindDevice",
			name = "Wind/Gale catcher",
			description = "Show Wind/Gale catcher upgrades",
			section = facilitiesSection
	)
	default boolean showWindDevice() { return false; }

	@ConfigItem(
			keyName = "showTrawlingNet",
			name = "Trawling net",
			description = "Show Trawling net upgrades",
			section = facilitiesSection
	)
	default boolean showTrawlingNet() { return false; }

	@ConfigItem(
			keyName = "showChumStation",
			name = "Chum station",
			description = "Show Chum station upgrades",
			section = facilitiesSection
	)
	default boolean showChumStation() { return false; }

	@ConfigItem(
			keyName = "showFathomDevice",
			name = "Fathom stone/pearl",
			description = "Show Fathom stone/pearl upgrades",
			section = facilitiesSection
	)
	default boolean showFathomDevice() { return false; }

	@ConfigItem(
			keyName = "showRange",
			name = "Range",
			description = "Show Range upgrades",
			section = facilitiesSection
	)
	default boolean showRange() { return false; }

	@ConfigItem(
			keyName = "showKeg",
			name = "Keg",
			description = "Show Keg upgrades",
			section = facilitiesSection
	)
	default boolean showKeg() { return false; }

	@ConfigItem(
			keyName = "showAnchor",
			name = "Anchor",
			description = "Show Anchor upgrades",
			section = facilitiesSection
	)
	default boolean showAnchor() { return false; }

	@ConfigItem(
			keyName = "showInoculationStation",
			name = "Inoculation station",
			description = "Show Inoculation station upgrades",
			section = facilitiesSection
	)
	default boolean showInoculationStation() { return false; }

	@ConfigItem(
			keyName = "showSalvagingStation",
			name = "Salvaging station",
			description = "Show Salvaging station upgrades",
			section = facilitiesSection
	)
	default boolean showSalvagingStation() { return false; }

	@ConfigItem(
			keyName = "showCrystalExtractor",
			name = "Crystal extractor",
			description = "Show Crystal extractor upgrades",
			section = facilitiesSection
	)
	default boolean showCrystalExtractor() { return false; }

	@ConfigItem(
			keyName = "showEternalBrazier",
			name = "Eternal brazier",
			description = "Show Eternal brazier upgrades",
			section = facilitiesSection
	)
	default boolean showEternalBrazier() { return false; }

	@ConfigItem(
			keyName = "comingSoon",
			name = "Schematic checks under development",
			description = "Schematic checks for text overlay under development",
			//position = 400,
			hidden = true
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