package com.nevilon.moow.core;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Bitmap.Config;

public class PhysicMap {

	private TileProvider tileProvider;

	private Bitmap[][] cells = new Bitmap[3][3];

	private RawTile defTile;

	private int zoom;

	public boolean canDraw = true;

	public Point globalOffset = new Point();

	public PhysicMap(RawTile defTile) {
		this.defTile = defTile;
		this.zoom = defTile.z;
		tileProvider = new TileProvider(this);
		loadCells(defTile);
	}

	public RawTile getDefaultTile() {
		return this.defTile;
	}

	public int getZoomLevel() {
		return this.zoom;
	}

	/**
	 * Callback method
	 * 
	 * @param bitmap
	 * @param tile
	 */
	public synchronized void update(Bitmap bitmap, RawTile tile) {
		int dx = tile.x - defTile.x;
		int dy = tile.y - defTile.y;
		if (dx <= 2 && dy <= 2 && tile.z == defTile.z) {
			if (dx >= 0 && dy >= 0) {
				try {
					cells[dx][dy] = bitmap;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void move(int dx, int dy) {
		reload(defTile.x - dx, defTile.y - dy, defTile.z);

	}

	public Bitmap[][] getCells() {
		return cells;
	}

	public void zoom(int x, int y, int z) {
		reload(x, y, z);
	}


	/**
	 * Уменьшение уровня детализации
	 */
	public void zoomOut() {
		if ((zoom) < 16) {
			int currentZoomX = getDefaultTile().x * 256 - globalOffset.x + 320/2;
			int currentZoomY = getDefaultTile().y * 256 - globalOffset.y + 480/2;

			// получение координат точки предудущем уровне
			int nextZoomX = currentZoomX / 2;
			int nextZoomY = currentZoomY / 2;

			// получение координат угла экрана на новом уровне
			nextZoomX = nextZoomX - 320 / 2;
			nextZoomY = nextZoomY - 480 / 2;

			// получение углового тайла
			int tileX = (nextZoomX / 256);
			int tileY = nextZoomY / 256;

			// отступ всегда один - точка должна находится в центре экрана
			int correctionX = nextZoomX - tileX * 256;
			int correctionY = nextZoomY - tileY * 256;

			globalOffset.x = -(correctionX);
			globalOffset.y = -(correctionY);
			zoom++;
			zoom(tileX, tileY, zoom);

		}

	}

	/**
	 * Увеличение уровня детализации с центрированием
	 */
	public void zoomInCenter() {
		zoomIn(160, 240);
	}

	/**
	 * Увеличение уровня детализации
	 * 
	 * @param offsetX
	 * @param offsetY
	 */
	public void zoomIn(int offsetX, int offsetY) {
		if (zoom > 0) {
			// получение отступа он начала координат
			int currentZoomX = getDefaultTile().x * 256 - globalOffset.x
					+ offsetX;
			int currentZoomY = getDefaultTile().y * 256 - globalOffset.y
					+ offsetY;

			// получение координат точки на новом уровне
			int nextZoomX = currentZoomX * 2;
			int nextZoomY = currentZoomY * 2;

			// получение координат угла экрана на новом уровне
			nextZoomX = nextZoomX - 320 / 2;
			nextZoomY = nextZoomY - 480 / 2;

			// получение углового тайла
			int tileX = (nextZoomX / 256);
			int tileY = nextZoomY / 256;

			// отступ всегда один - точка должна находится в центре экрана
			int correctionX = nextZoomX - tileX * 256;
			int correctionY = nextZoomY - tileY * 256;

			globalOffset.x = -(correctionX);
			globalOffset.y = -(correctionY);
			zoom--;
			zoom(tileX, tileY, zoom);
		}
	}

	private void reload(int x, int y, int z) {
		defTile.x = x;
		defTile.y = y;
		defTile.z = z;
		loadCells(defTile);
	}

	/**
	 * Проверяет на допустимость параметры тайла
	 * 
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private boolean checkTileXY(int x, int y, int z) {
		if (x < 0 || y < 0 || z < 0) {
			return false;
		}
		int maxTile = (int) Math.pow(2, 17 - z) - 1;
		if (x > maxTile || y > maxTile) {
			return false;
		}
		return true;

	}

	/**
	 * Очистка in-memory кеша
	 */
	public void gc() {
		tileProvider.inMemoryCache.gc();
	}

	/**
	 * Запрос на загрузку тайлов для данной группы ячеек (определяется по
	 * крайней левой верхней)
	 * 
	 * @param tile
	 */
	private synchronized void loadCells(RawTile tile) {
		canDraw = false;
		Bitmap tmpBitmap;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int x, y;
				x = (tile.x + i);
				y = (tile.y + j);
				if (!checkTileXY(x, y, tile.z)) {
					cells[i][j] = null;
				} else {
					tmpBitmap = tileProvider.inMemoryCache.get(new RawTile(x,
							y, tile.z));
					if (tmpBitmap != null) {
						cells[i][j] = tmpBitmap;
					} else {
						 cells[i][j] = null;
						tileProvider.getTile(new RawTile(x, y, tile.z));
					}
				}

			}
		}
		canDraw = true;
	}
}
