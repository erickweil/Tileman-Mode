/*
 * Copyright (c) 2019, Benjamin <https://github.com/genetic-soybean>
 * Copyright (c) 2020, Bram91 <https://github.com/Bram91>
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

import java.awt.*;
import java.awt.geom.Area;
import java.util.Collection;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.RenderOverview;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

class TilemanModeWorldMapOverlay extends Overlay {
    private static final int REGION_SIZE = 1 << 6;
    // Bitmask to return first coordinate in region
    private static final int REGION_TRUNCATE = ~((1 << 6) - 1);    

        private final Client client;
	private final TilemanModePlugin plugin;

	@Inject
	private TilemanModeConfig config;

        
        @Inject
	private TilemanModeWorldMapOverlay(Client client, TilemanModeConfig config, TilemanModePlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ABOVE_MAP);
	}
        
    @Override
    public Dimension render(Graphics2D graphics) {
            
            if(!config.drawTilesOnWorldMap()) return null;
            //World Map
            Widget map = client.getWidget(WidgetInfo.WORLD_MAP_VIEW);
            if (map == null)
            {
                    return null;
            }

            final Rectangle worldMapRect = map.getBounds();
            final Area mapViewArea = getWorldMapClipArea(worldMapRect);

            graphics.setClip(mapViewArea);
            
            RenderOverview ro = client.getRenderOverview();
            float pixelsPerTile =ro.getWorldMapZoom();
            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);
            
            Point worldMapPosition = ro.getWorldMapPosition();

            // Offset in tiles from anchor sides
            int yTileMin = worldMapPosition.getY() - heightInTiles / 2;
            int xRegionMin = (worldMapPosition.getX() - widthInTiles / 2) & REGION_TRUNCATE;
            int xRegionMax = ((worldMapPosition.getX() + widthInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;
            int yRegionMin = (yTileMin & REGION_TRUNCATE);
            int yRegionMax = ((worldMapPosition.getY() + heightInTiles / 2) & REGION_TRUNCATE) + REGION_SIZE;
            int regionPixelSize = (int) Math.ceil(REGION_SIZE * pixelsPerTile);

            for (int x = xRegionMin; x < xRegionMax; x += REGION_SIZE) {
                for (int y = yRegionMin; y < yRegionMax; y += REGION_SIZE) {
                    int regionId = ((x >> 6) << 8) | (y >> 6);
                    for (final TilemanModeTile tile : plugin.getTilesFromCache(regionId)) {
                        if(tile.getZ() != client.getPlane()) {
                            continue;
                        }
            
                        drawTileOnWorldMap(graphics,WorldPoint.fromRegion(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ()),ro,map,pixelsPerTile);
                    }
                }
            }
            //if(config.drawTilesOnWorldMap() == TilemanModeConfig.TilemanWorldMapDrawStyle.ONLYLOADED)
            //{
            //    final Collection<WorldPoint> points = plugin.getPoints();
            //    for (final WorldPoint point : points)
            //    {
            //        drawTileOnWorldMap(graphics, point,ro,map,pixelsPerTile);
            //    }
            //}
            /*else if(config.drawTilesOnWorldMap() == TilemanModeConfig.TilemanWorldMapDrawStyle.EVERYTHING)
            {
                final HashMap<Integer,Collection<WorldPoint>> everySinglePoint = plugin.getEverySinglePoint();
                Set<Integer> keySet = everySinglePoint.keySet();
                for(Integer regionId : keySet)
                {
                    final Collection<WorldPoint> points = everySinglePoint.get(regionId);
                    for (final WorldPoint point : points)
                    {
                        drawTileOnWorldMap(graphics, point,ro,map,pixelsPerTile);
                    }
                }
            }*/
            
            return null;
    }
    
    private void drawTileOnWorldMap(final Graphics2D graphics,WorldPoint worldPoint,RenderOverview ro,Widget map,float pixelsPerTile)
        {
            net.runelite.api.Point drawPoint = mapWorldPointToGraphicsPoint(worldPoint,ro,map,pixelsPerTile);
            
            if(drawPoint == null)
            {
                return;
            }
            
            int drawX = drawPoint.getX();
            int drawY = drawPoint.getY();
            
            int tileWidth = Math.max(config.worldMapTilesMinWidth(),(int)pixelsPerTile);
            
            drawX -= tileWidth / 2;
            drawY -= tileWidth / 2;
            
            graphics.setColor(config.markerColor());
            graphics.drawRect(drawX, drawY, tileWidth,tileWidth);
            if(config.worldMapFill())
            graphics.fillRect(drawX, drawY, tileWidth,tileWidth);
        }
        
        // Copiado de WorldMapOverlay
        	/**
	 * Gets a clip area which excludes the area of widgets which overlay the world map.
	 *
	 * @param baseRectangle The base area to clip from
	 * @return              An {@link Area} representing <code>baseRectangle</code>, with the area
	 *                      of visible widgets overlaying the world map clipped from it.
	 */
	private Area getWorldMapClipArea(Rectangle baseRectangle)
	{
		final Widget overview = client.getWidget(WidgetInfo.WORLD_MAP_OVERVIEW_MAP);
		final Widget surfaceSelector = client.getWidget(WidgetInfo.WORLD_MAP_SURFACE_SELECTOR);

		Area clipArea = new Area(baseRectangle);

		if (overview != null && !overview.isHidden())
		{
			clipArea.subtract(new Area(overview.getBounds()));
		}

		if (surfaceSelector != null && !surfaceSelector.isHidden())
		{
			clipArea.subtract(new Area(surfaceSelector.getBounds()));
		}

		return clipArea;
	}
        
        // Copiado de WorldMapOverlay
        /**
	 * Get the screen coordinates for a WorldPoint on the world map
	 * @param worldPoint WorldPoint to get screen coordinates of
	 * @return Point of screen coordinates of the center of the world point
	 */
	public net.runelite.api.Point mapWorldPointToGraphicsPoint(WorldPoint worldPoint,final RenderOverview ro,final Widget map,float pixelsPerTile)
	{
		if (!ro.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY()))
		{
			return null;
		}

		if (map != null)
		{
			Rectangle worldMapRect = map.getBounds();

			int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
			int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

			net.runelite.api.Point worldMapPosition = ro.getWorldMapPosition();

			//Offset in tiles from anchor sides
			int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
			int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
			int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

			int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
			int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

			//Center on tile.
			yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
			xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

			yGraphDiff = worldMapRect.height - yGraphDiff;
			yGraphDiff += (int) worldMapRect.getY();
			xGraphDiff += (int) worldMapRect.getX();

			return new net.runelite.api.Point(xGraphDiff, yGraphDiff);
		}
		return null;
	}
}
