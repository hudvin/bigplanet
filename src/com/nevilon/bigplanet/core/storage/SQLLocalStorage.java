package com.nevilon.bigplanet.core.storage;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.nevilon.bigplanet.core.RawTile;

public class SQLLocalStorage implements ILocalStorage {

	private static ILocalStorage localStorage;

	private static String X_COLUMN = "x";

	private static String Y_COLUMN = "y";

	private static String Z_COLUMN = "z";

	private static String S_COLUMN = "s";

	private static String IMAGE_COLUMN = "image";

	private static String TILES_TABLE = "tiles";

	private static String TABLE_DDL = "CREATE TABLE IF NOT EXISTS tiles( x int,y int, z int,s int, image blob)";

	private static String INDEX_DDL = "CREATE INDEX IF NOT EXISTS IND on tiles (x,y,z,s)";

	private static String DATA_FILE = "/sdcard/maps.data";

	private static String DELETE_SQL = "DELECT FROM tiles";

	private static String GET_SQL = "SELECT * FROM tiles WHERE x=? AND y=? AND z=? AND s=?";

	private static String COUNT_SQL = "SELECT COUNT(*) FROM tiles WHERE x=? AND y=? AND z=? AND s=?";
	
	private SQLiteDatabase db;

	public static ILocalStorage getInstance() {
		if (localStorage == null) {
			localStorage = new SQLLocalStorage();
		}
		return localStorage;
	}

	/**
	 * Конструктор Инициализация файлового кеша(если необходимо)
	 */
	private SQLLocalStorage() {
		db = SQLiteDatabase.openDatabase(SQLLocalStorage.DATA_FILE, null,
				SQLiteDatabase.CREATE_IF_NECESSARY);
		db.execSQL(SQLLocalStorage.TABLE_DDL);
		db.execSQL(SQLLocalStorage.INDEX_DDL);
	}

	public void clear() {
		db.execSQL(SQLLocalStorage.DELETE_SQL);
	}

	public BufferedInputStream get(RawTile tile) {
		String sql = SQLLocalStorage.GET_SQL;

		Cursor c = db.rawQuery(sql, new String[] { String.valueOf(tile.x),
				String.valueOf(tile.y), String.valueOf(tile.z),
				String.valueOf(tile.s), });

		BufferedInputStream io = null;
		if (c.getCount() != 0) {
			c.moveToFirst();
			byte[] d = c.getBlob(c.getColumnIndex(SQLLocalStorage.IMAGE_COLUMN));
			io = new BufferedInputStream(new ByteArrayInputStream(d));
		}
		c.close();
		return io;
	}

	public boolean isExists(RawTile tile) {
		Cursor c = db.rawQuery(SQLLocalStorage.COUNT_SQL, new String[] { String.valueOf(tile.x),
				String.valueOf(tile.y), String.valueOf(tile.z),
				String.valueOf(tile.s), });
		c.moveToFirst();
		int count = c.getInt(0);
		c.close();
		return count==1;
	}

	public void put(RawTile tile, byte[] data) {
		ContentValues initialValues = new ContentValues();
		initialValues.put(SQLLocalStorage.X_COLUMN, tile.x);
		initialValues.put(SQLLocalStorage.Y_COLUMN, tile.y);
		initialValues.put(SQLLocalStorage.Z_COLUMN, tile.z);
		initialValues.put(SQLLocalStorage.S_COLUMN, tile.s);
		initialValues.put(SQLLocalStorage.IMAGE_COLUMN, data);
		db.insert(SQLLocalStorage.TILES_TABLE, null, initialValues);
	}

}
