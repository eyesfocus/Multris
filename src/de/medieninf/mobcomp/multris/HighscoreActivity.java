package de.medieninf.mobcomp.multris;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ViewFlipper;
import de.medieninf.mobcomp.multris.R;
import de.medieninf.mobcomp.multris.game.persistence.HighscoreDBAdapter;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 * 
 *         Activity that shows up the highscore of multiplayer mode if started inserting a new highscore after a
 *         multiplayer game, else shows highscore of singleplayer mode. you can also switch between those highscores
 *         with touching the radiobuttons or with a swiping gesture
 * */
public class HighscoreActivity extends Activity {
	private static final String TAG = HighscoreActivity.class.getSimpleName();

	public static final String MULTIPLAYER_SCORE = "multi_score";
	private final String SORTED_BY = HighscoreDBAdapter.HIGHSCORE_POINTS;

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_MAX_OFF_PATH = 250;
	private boolean multiplayerScore;

	private GestureDetector gestureDetector;

	private TableLayout table_mp, table_sp;
	private TextView header;
	private ViewFlipper viewflipper;
	private View highscoreLayout;
	private RadioGroup group;

	private HighscoreDBAdapter dba;
	private Cursor cursor;

	private OnCheckedChangeListener buttonGroupListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.radio_button_sp:
				switchToView(table_sp);
				break;
			case R.id.radio_button_mp:
				switchToView(table_mp);
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "+ ON CREATE +");

		multiplayerScore = (Boolean) getIntent().getExtras().get(MULTIPLAYER_SCORE);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_highscore);

		// init layout params
		highscoreLayout = (View) findViewById(R.id.highscore_layout);
		viewflipper = (ViewFlipper) findViewById(R.id.viewflipper);
		table_mp = (TableLayout) findViewById(R.id.highscore_table_layout_mp);
		table_sp = (TableLayout) findViewById(R.id.highscore_table_layout_sp);
		header = (TextView) findViewById(R.id.header);
		group = (RadioGroup) findViewById(R.id.buttongroup);

		// add listeners to layout
		gestureDetector = new GestureDetector(new MyGestureDetector());
		prepareGestures();
		group.setOnCheckedChangeListener(buttonGroupListener);

		// update databasae
		dba = new HighscoreDBAdapter(this);
		dba.open();
		updateHighscores();
		dba.close();

		initViewContent();
	}

	/**
	 * fills the highscore table, header with initial content depending on the activities intent extra "MULTIPLAYER"S
	 * */
	private void initViewContent() {
		if (multiplayerScore) {
			viewflipper.setDisplayedChild(1); // multiplayerscore table
			group.check(R.id.radio_button_mp);
			header.setText(getResources().getString(R.string.highscore_title, "MULTRIS"));

		} else {
			viewflipper.setDisplayedChild(0); // singleplayer table
			group.check(R.id.radio_button_sp);
			header.setText(getResources().getString(R.string.highscore_title, "TETRIS"));
		}
	}

	/**
	 * Adds onTouchListener to highscorelayout
	 * 
	 * */
	private void prepareGestures() {
		highscoreLayout.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if (gestureDetector.onTouchEvent(event)) {
					return true;
				}
				return false;
			}
		});
	}

	/**
	 * fills the given table with its data
	 * 
	 * @param table
	 *            TableLayout that has to be filled
	 * */
	private void fillTable(TableLayout table) {
		if (table.getChildCount() > 0) {
			table.removeViews(0, table.getChildCount() - 1);
		}
		cursor = table.equals(table_sp) ? dba.getAllHighscores(0, SORTED_BY) : dba.getAllHighscores(1, SORTED_BY);
		startManagingCursor(cursor);
		cursor.requery();
		LayoutInflater inflater = getLayoutInflater();

		if (cursor.moveToFirst()) {
			do {
				TableRow hsTableRow = (TableRow) inflater.inflate(R.layout.activity_highscore_row, table, false);

				TextView actPos = (TextView) hsTableRow.findViewById(R.id.highscore_no_textview);
				TextView actname = (TextView) hsTableRow.findViewById(R.id.highscore_name_textview);
				TextView actpoints = (TextView) hsTableRow.findViewById(R.id.highscore_points_textview);

				actPos.setText("" + (cursor.getPosition() + 1));
				actname.setText("" + cursor.getString(HighscoreDBAdapter.COLUMN_NAME));
				actpoints.setText("" + cursor.getInt(HighscoreDBAdapter.COLUMN_POINTS));

				table.addView(hsTableRow);
			} while (cursor.moveToNext());
		}
		cursor.close();
	}

	/**
	 * fills both tablelayouts. one with singleplayer highscore, one with multiplayer
	 * */
	private void updateHighscores() {
		fillTable(table_sp);
		fillTable(table_mp);
	}

	/**
	 * Changes the view (Table, Header, Radiobuttons)
	 * 
	 * @param table
	 *            Table the view has to be changed to
	 * */
	private void switchToView(TableLayout table) {
		int visibleLayout = table.equals(table_sp) ? 0 : 1;
		String headerstring = table.equals(table_sp) ? getResources().getString(R.string.highscore_title, "TETRIS") : getResources().getString(R.string.highscore_title, "MULTRIS");
		viewflipper.setDisplayedChild(visibleLayout);
		header.setText(headerstring);
	}

	class MyGestureDetector extends SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

			if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
				return false;
			}

			// right to left swipe
			if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE) {
				if (viewflipper.getDisplayedChild() == 0) {
					group.check(R.id.radio_button_mp);
				}
			} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE) { // left to right
				if (viewflipper.getDisplayedChild() == 1) {
					group.check(R.id.radio_button_sp);
				}
			}

			return false;
		}

		// Necessary to return true from onDown for onFling event to register
		@Override
		public boolean onDown(MotionEvent e) {
			return true;
		}
	}

}
