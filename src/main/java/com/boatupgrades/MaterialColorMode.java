package com.boatupgrades;

public enum MaterialColorMode
{
	BOTH("Both", true, true),
	TEXT_OVERLAY_ONLY("Text overlay", true, false),
	SIDE_PANEL_ONLY("Side panel", false, true),
	OFF("Off", false, false);

	private final String label;
	private final boolean overlay;
	private final boolean sidePanel;

	MaterialColorMode(String label, boolean overlay, boolean sidePanel)
	{
		this.label = label;
		this.overlay = overlay;
		this.sidePanel = sidePanel;
	}

	public boolean colorsOverlay() { return overlay; }
	public boolean colorsSidePanel() { return sidePanel; }

	@Override
	public String toString() { return label; }
}
