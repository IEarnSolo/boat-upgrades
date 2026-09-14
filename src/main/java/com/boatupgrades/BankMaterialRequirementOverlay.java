package com.boatupgrades;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.WidgetItemOverlay;

@Slf4j
@Singleton
public class BankMaterialRequirementOverlay extends WidgetItemOverlay
{
	private final BoatUpgradesConfig config;
	private final BankMaterialViewService bankMaterialViewService;
	private final MaterialOwnershipService materialOwnershipService;

	@Inject
	public BankMaterialRequirementOverlay(BoatUpgradesConfig config,
		BankMaterialViewService bankMaterialViewService, MaterialOwnershipService materialOwnershipService)
	{
		this.config = config;
		this.bankMaterialViewService = bankMaterialViewService;
		this.materialOwnershipService = materialOwnershipService;
		drawAfterLayer(InterfaceID.Bankmain.ITEMS);
		log.debug("[Bank View] Initialized per-upgrade inventory requirement progress overlay");
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		if (!config.showBankRequirementProgress()) return;

		Widget widget = widgetItem.getWidget();
		if (widget.getId() != InterfaceID.Bankmain.ITEMS) return;
		BankMaterialViewService.SectionRequirement requirement =
			bankMaterialViewService.getActiveSectionRequirement(widget.getIndex());
		if (requirement == null) return;

		int required = requirement.getRequiredQuantity();
		int inventory = Math.min(materialOwnershipService.getInventoryQuantity(requirement.getMaterialName()), required);
		String text = formatQuantity(inventory) + "/" + formatQuantity(required);
		Rectangle bounds = widgetItem.getCanvasBounds();
		if (bounds == null) return;

		Font baseFont = FontManager.getRunescapeSmallFont();
		Font font = baseFont.deriveFont((float) Math.max(7, baseFont.getSize() - 2));
		graphics.setFont(font);
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(text);
		int x = bounds.x + bounds.width - width - 2;
		int baseline = bounds.y + bounds.height - metrics.getDescent() - 2;
		graphics.setColor(Color.BLACK);
		graphics.drawString(text, x - 1, baseline);
		graphics.drawString(text, x + 1, baseline);
		graphics.drawString(text, x, baseline - 1);
		graphics.drawString(text, x, baseline + 1);
		graphics.setColor(materialOwnershipService.getColor(requirement.getMaterialName(), required));
		graphics.drawString(text, x, baseline);
	}

	private static String formatQuantity(int quantity)
	{
		return quantity >= 1_000 ? quantity / 1_000 + "k" : String.valueOf(quantity);
	}
}
