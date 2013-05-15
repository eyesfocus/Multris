package de.medieninf.mobcomp.multris;

import de.medieninf.mobcomp.multris.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         Activity that holds two buttons and offers to change to HighscoreActivity or GameModeActivity
 * */
public class EntryActivity extends Activity {

	private Button newGameButton, highscoreButton;

	private OnClickListener buttonListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int viewId = v.getId();
			switch (viewId) {
			case R.id.button_new_game:
				goToGameMode();
				break;
			case R.id.button_viewscore:
				goToHighscore();
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_entry);

		newGameButton = (Button) findViewById(R.id.button_new_game);
		highscoreButton = (Button) findViewById(R.id.button_viewscore);
		newGameButton.setOnClickListener(buttonListener);
		highscoreButton.setOnClickListener(buttonListener);
	}

	/**
	 * starts GameModeActivity
	 * */
	private void goToGameMode() {
		Intent intent = new Intent(EntryActivity.this, GameModeActivity.class);
		startActivity(intent);
	}

	/**
	 * starts HighscoreActivity
	 * */
	private void goToHighscore() {
		Intent intent = new Intent(EntryActivity.this, HighscoreActivity.class);
		intent.putExtra(HighscoreActivity.MULTIPLAYER_SCORE, false);
		startActivity(intent);
	}
}
