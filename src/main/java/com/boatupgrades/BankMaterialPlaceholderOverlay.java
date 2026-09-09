package com.boatupgrades;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ItemQuantityMode;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;

@Slf4j
@Singleton
public class BankMaterialPlaceholderOverlay extends WidgetItemOverlay
{
	private final Client client;
	private final BankMaterialViewService bankMaterialViewService;
	private final TooltipManager tooltipManager;

	@Inject
	public BankMaterialPlaceholderOverlay(Client client, BankMaterialViewService bankMaterialViewService,
		TooltipManager tooltipManager)
	{
		this.client = client;
		this.bankMaterialViewService = bankMaterialViewService;
		this.tooltipManager = tooltipManager;
		drawAfterLayer(InterfaceID.Bankmain.ITEMS);
		log.debug("[Bank View] Initialized temporary-placeholder tooltip overlay");
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem widgetItem)
	{
		Widget widget = widgetItem.getWidget();
		if (widget.getId() != InterfaceID.Bankmain.ITEMS
			|| widget.getItemQuantity() != Integer.MAX_VALUE
			|| widget.getItemQuantityMode() != ItemQuantityMode.NEVER)
		{
			return;
		}

		Rectangle bounds = widgetItem.getCanvasBounds();
		Point mouse = client.getMouseCanvasPosition();
		if (bounds == null || mouse == null || !bounds.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		String tooltip = bankMaterialViewService.getFakePlaceholderTooltip(itemId);
		if (tooltip != null)
		{
			tooltipManager.add(new Tooltip(tooltip));
		}
	}
}
