package de.medieninf.mobcomp.multris.game.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * @author tina schedlbauer
 * 
 *         Class to manage 'communication' with a Highscore SQLite Database
 */
public class HighscoreDBAdapter {
	private static final String DB_NAME = "highscore.db";
	private static final int DB_VERSION = 1;
	private static final String DB_TABLE_HIGHSCORE = "highscore";

	// Column index
	public static final int COLUMN_ID = 0;
	public static final int COLUMN_NAME = 1;
	public static final int COLUMN_POINTS = 2;
	public static final int COLUMN_MULTIPLAYER = 3;

	// Column keys
	public static final String HIGHSCORE_ID = "_id";
	public static final String HIGHSCORE_NAME = "name";
	public static final String HIGHSCORE_POINTS = "points";
	public static final String HIGHSCORE_MULTIPLAYER = "multiplayer";

	private SQLiteDatabase db;
	private HighscoreDBHelper dbHelper;

	// To create database
	private static final String DB_TABLE_HIGHSCORE_CREATE = "CREATE TABLE " + DB_TABLE_HIGHSCORE + "(" + HIGHSCORE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + HIGHSCORE_NAME
			+ " TEXT NOT NULL, " + HIGHSCORE_POINTS + " INTEGER, " + HIGHSCORE_MULTIPLAYER + " INTEGER )";

	public HighscoreDBAdapter(Context context) {
		dbHelper = new HighscoreDBHelper(context, DB_NAME, null, DB_VERSION);
	}

	private static class HighscoreDBHelper extends SQLiteOpenHelper {
		public HighscoreDBHelper(Context context, String name, CursorFactory factory, int version) {
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DB_TABLE_HIGHSCORE_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE_HIGHSCORE);
			onCreate(db);
		}
	}

	public void open() throws SQLException {
		try {
			db = dbHelper.getWritableDatabase();
		} catch (SQLException e) {
			db = dbHelper.getReadableDatabase();
		}
	}

	public void close() {
		db.close();
	}

	/**
	 * 
	 * Inserts a new highscorew-entry into database
	 * 
	 * @param hs
	 *            Highscore to insert into database
	 * @return index
	 * */
	public int insertHighscore(Highscore hs) {
		int index;
		ContentValues hsValues = new ContentValues();
		hsValues.put(HIGHSCORE_NAME, hs.getName());
		hsValues.put(HIGHSCORE_POINTS, hs.getPoints());
		hsValues.put(HIGHSCORE_MULTIPLAYER, hs.isMultiplayer());

		index = (int) db.insert(DB_TABLE_HIGHSCORE, null, hsValues);
		return index;
	}

	/**
	 * 
	 * Deletes highscorew-entry with specific id
	 * 
	 * @param id
	 *            ID of entry that has to be deleted
	 * @return true if deletion successful, false otherwise
	 * */
	public boolean removeHighscore(int id) {
		return db.delete(DB_TABLE_HIGHSCORE, HIGHSCORE_ID + "=" + id, null) == 1;
	}

	/**
	 * @param sorted_by
	 *            flag by which column highscores shall be sorted
	 * @return cursor with all highscores
	 * */
	public Cursor getAllHighscores(String sorted_by) {
		String[] cols = new String[] { HIGHSCORE_ID, HIGHSCORE_NAME, HIGHSCORE_POINTS, HIGHSCORE_MULTIPLAYER };
		Cursor cursor = db.query(DB_TABLE_HIGHSCORE, cols, null, null, null, null, sorted_by + " ASC");
		return cursor;
	}

	/**
	 * @param multiplayer
	 *            Flag to request single or multiplayer highscore list
	 * @param sorted_by
	 *            flag by which column highscores shall be sorted
	 * @return cursor with all highscores
	 * */
	public Cursor getAllHighscores(int multiplayer, String sorted_by) {
		String[] cols = new String[] { HIGHSCORE_ID, HIGHSCORE_NAME, HIGHSCORE_POINTS, HIGHSCORE_MULTIPLAYER };
		Cursor cursor = db.query(DB_TABLE_HIGHSCORE, cols, HIGHSCORE_MULTIPLAYER + "=" + multiplayer, null, null, null, sorted_by + " DESC");
		return cursor;
	}
}
