/*
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * Copyright (c) 2020, ConorLeckey <https://github.com/ConorLeckey>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tileman;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import javax.inject.Inject;
import java.awt.*;
import java.util.Collection;
import net.runelite.api.Tile;

public class TilemanModeOverlay extends Overlay
{

	private final Client client;
	private final TilemanModePlugin plugin;

	@Inject
	private TilemanModeConfig config;

	@Inject
	private TilemanModeOverlay(Client client, TilemanModeConfig config, TilemanModePlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Collection<WorldPoint> points = plugin.getPoints();
		for (final WorldPoint point : points)
		{
			if (point.getPlane() != client.getPlane())
			{
				continue;
			}

			drawTile(graphics, point);
		}
	
		// Tiles Hovered
		if (config.showHoveredTile())
		{
			// If we have tile "selected" render it
			if (client.getSelectedSceneTile() != null)
			{
				renderInfoTile(graphics, client.getSelectedSceneTile(), new Color(0x00000000,true));
			}
		}     

		return null;
	}
	
	private void renderInfoTile(final Graphics2D graphics, final Tile destTile, final Color color)
	{
		if (destTile == null || destTile.getLocalLocation() == null)
		{
			return;
		}
		LocalPoint point = destTile.getLocalLocation();
		
		
		LocalPoint infoPoint = new LocalPoint(point.getX(),point.getY());
		boolean walkable = plugin.isWalkable(point,0,0);

		final Polygon poly = Perspective.getCanvasTilePoly(client, infoPoint);

		if (poly == null)
		{
			return;
		}


		
		//Point p1 = localToCanvas(client, swX, swY, swHeight);
		//Point p2 = localToCanvas(client, nwX, nwY, nwHeight);
		//Point p3 = localToCanvas(client, neX, neY, neHeight);
		//Point p4 = localToCanvas(client, seX, seY, seHeight);
		/*
		3-------2
		|       |
		|       |
		0-------1
		*/
		OverlayUtil.renderPolygon(graphics, poly, walkable ? color : Color.RED);
		
		// North
		if(!plugin.isWalkable(point, 0, 1))
			polyWall(graphics, poly, 3, 2);
		// South               
		if(!plugin.isWalkable(point, 0, -1))
			polyWall(graphics, poly, 0, 1);
		// East
		if(!plugin.isWalkable(point, 1, 0))
			polyWall(graphics, poly, 1, 2);
		// West
		if(!plugin.isWalkable(point, -1, 0))
			polyWall(graphics, poly, 0, 3);
	}
	
	private void polyWall(final Graphics2D graphics,Polygon poly,int a,int b)
	{
		Polygon polyN = new Polygon();
		polyN.addPoint(poly.xpoints[a], poly.ypoints[a]);
		polyN.addPoint(poly.xpoints[b], poly.ypoints[b]);
		polyN.addPoint(poly.xpoints[a], poly.ypoints[a]);
		polyN.addPoint(poly.xpoints[b], poly.ypoints[b]);

		OverlayUtil.renderPolygon(graphics, polyN, Color.RED);
	}

	private void drawTile(Graphics2D graphics, WorldPoint point)
	{
		WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

		if (point.distanceTo(playerLocation) >= config.maxDrawDistance())
		{
			return;
		}

		LocalPoint lp = LocalPoint.fromWorld(client, point);
		if (lp == null)
		{
			return;
		}

		Polygon poly = Perspective.getCanvasTilePoly(client, lp);
		if (poly == null)
		{
			return;
		}

		OverlayUtil.renderPolygon(graphics, poly, getTileColor());
	}

	private Color getTileColor() {
		if (plugin.getRemainingTiles() <= 0) {
			return Color.RED;
		} else if (plugin.getRemainingTiles() <= config.warningLimit()) {
			return Color.ORANGE;
		}
		return config.markerColor();
	}
}
