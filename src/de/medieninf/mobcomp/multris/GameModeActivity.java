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
 *         Activity to choose the GameMode. Either Single or Multiplayer can be chosen. If user chooses singleplayer,
 *         the TetrisActivity will be started in singleplayer mode, else BTConnectionActivity will be started to set up
 *         the game with more players via Bluetooth
 * */
public class GameModeActivity extends Activity {
	// private static final String TAG = ConnectionActivity.class.getSimpleName();

	private static final int REQUEST_START_GAME = 3;

	private Button singlePlayerView, multiPlayerView;

	private OnClickListener buttonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int vId = v.getId();
			switch (vId) {
			case R.id.textview_singleplayer:
				goToGame();
				break;
			case R.id.textview_multiplayer:
				startBTConnectionActivity();
				break;
			}
		}

	};

	/** Creates Intent to start game in singleplayer mode */
	private void goToGame() {
		Intent intent = new Intent(GameModeActivity.this, TetrisActivity.class);
		intent.putExtra(TetrisActivity.EXTRA_MULTIPLAYER, false);
		startActivityForResult(intent, REQUEST_START_GAME);
	}

	/** Creates Intent to start BTConnectionActivity */
	private void startBTConnectionActivity() {
		Intent intent = new Intent(GameModeActivity.this, ConnectionActivity.class);
		startActivity(intent);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_game_mode);

		singlePlayerView = (Button) findViewById(R.id.textview_singleplayer);
		multiPlayerView = (Button) findViewById(R.id.textview_multiplayer);

		// add listeners
		singlePlayerView.setOnClickListener(buttonListener);
		multiPlayerView.setOnClickListener(buttonListener);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_START_GAME:
			// When the request to start the game returns
			break;
		}
	}
}
