package de.medieninf.mobcomp.multris;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import de.medieninf.mobcomp.multris.R;
import de.medieninf.mobcomp.multris.game.GameThread;
import de.medieninf.mobcomp.multris.game.persistence.Highscore;
import de.medieninf.mobcomp.multris.game.persistence.HighscoreDBAdapter;
import de.medieninf.mobcomp.multris.network.BTMessage;
import de.medieninf.mobcomp.multris.view.TetrisView;

/**
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 * Activity that holds all the information during a running game.
 * It starts the GameService which communicates with the GameState. 
 * 
 * */
public class TetrisActivity extends Activity {

	private static final String TAG = TetrisActivity.class.getSimpleName();

	public static final String EXTRA_MULTIPLAYER = "multiplayer";
	public static final String EXTRA_PLAYER_NO = "player_no";
	public static final String EXTRA_PLAYER_COUNT = "player_count";

	public static final int RESULT_GAMEOVER = 1;
	public static final int DIALOG_NEW_HIGHSCORE = 1;
	public static final int DIALOG_RESUME_GAME = 2;

	private final int HIGHSCORE_MINIMUM = 5;

	private GameService gameService;
	private volatile GameThread gameThread;
	private TetrisView tetrisView;
	private boolean serviceBound;
	private boolean isMultiplayer;
	private int playerNo;
	private boolean newGame;

	private int playerCount;

	private ServiceConnection gameServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.v(TAG, "onServiceConnected");
			gameService = ((GameService.GameServiceBinder) service).getService();
			resumeGame();
			tetrisView.setGameService(gameService);
			serviceBound = true;
		}

		public void onServiceDisconnected(ComponentName name) {
			Log.v(TAG, "onServiceDisonnected");
			serviceBound = false;
		}
	};

	/**
	 * Handler that handles incoming Pause/Resume events from other devices, only for Mutiplayer
	 */
	private final Handler gameHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case BTMessage.PAUSE_GAME:
				Log.v(TAG, "Handle incoming pause");
				if (gameThread != null) {
					gameThread.setRunning(false);
				}
				break;
			case BTMessage.RESUME_GAME:
				Log.v(TAG, "Handle incoming resume");
				resumeGame();
				break;
			}
		}
	};

	/**
	 * Resumes the Game depending on the Mutiplayerstate
	 */
	private void resumeGame() {
		gameThread = new GameThread(tetrisView);
		gameThread.setRunning(true);
		if (isMultiplayer) {
			gameService.setGameHandler(gameHandler);
			if (gameService.isPaused() && (isMultiplayer == gameService.isMultiplayer()) && !newGame) {
				startTetrisView();
			} else {
				gameService.initNewGame(isMultiplayer, playerNo, playerCount);
				startTetrisView();
			}
			gameService.setPaused(false);
		} else {
			if (gameService.isPaused() && (isMultiplayer == gameService.isMultiplayer())) {
				showDialog(DIALOG_RESUME_GAME);
			} else {
				gameService.initNewGame(isMultiplayer, playerNo, playerCount);
				startTetrisView();
			}
		}
		newGame = false;
	}

	/**
	 * Pauses the Game depending on the Mutiplayerstate
	 */
	private void pauseGame() {
		gameThread.setRunning(false);
		gameService.setPaused(true);

	}

	/**
	 * Binds the GameService to this Activity, so that its methods can be used in this Activity.
	 */
	private void doBindGameService() {
		Intent intent = new Intent(this, GameService.class);
		bindService(intent, gameServiceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Starts the GameService
	 */
	private void doStartGameService() {
		Intent intent = new Intent(this, GameService.class);
		startService(intent);
	}

	/**
	 * Unbinds the GameService from this Activity.
	 */
	private void doUnBindGameService() {
		if (serviceBound) {
			serviceBound = false;
			unbindService(gameServiceConnection);
			gameThread.setRunning(false);
			gameThread = null;
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		isMultiplayer = (Boolean) getIntent().getExtras().get(EXTRA_MULTIPLAYER);
		if (isMultiplayer) {
			playerNo = (Integer) getIntent().getExtras().get(EXTRA_PLAYER_NO);
			playerCount = (Integer) getIntent().getExtras().get(EXTRA_PLAYER_COUNT);
		} else {
			playerNo = 0;
			playerCount = 1;
		}
		newGame = true;
		serviceBound = false;
		tetrisView = new TetrisView(this);
		setContentView(tetrisView);
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart...");
		doStartGameService();
		super.onStart();
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume...");
		super.onResume();
		doBindGameService();

	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop...");
		super.onStop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.d(TAG, "onPause...");
		pauseGame();
		doUnBindGameService();
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "Destroying...");
		super.onDestroy();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {

		case DIALOG_RESUME_GAME:
			final Dialog resumeDialog = new Dialog(TetrisActivity.this, R.style.CustomDialogStyle);
			resumeDialog.setContentView(R.layout.dialog_resume_game);

			final Button resumeButton = (Button) resumeDialog.findViewById(R.id.resume_button);
			final Button newGameButton = (Button) resumeDialog.findViewById(R.id.new_game_button);

			resumeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					resumeDialog.dismiss();
					startTetrisView();
				}
			});

			newGameButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					resumeDialog.dismiss();
					gameService.initNewGame(isMultiplayer, playerNo, playerCount);
					startTetrisView();
				}
			});

			resumeDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					TetrisActivity.this.finish();
				}
			});

			return resumeDialog;
		case DIALOG_NEW_HIGHSCORE:
			final Dialog highscoreDialog = new Dialog(TetrisActivity.this, R.style.CustomDialogStyle);
			highscoreDialog.setContentView(R.layout.dialog_newhighscore);

			highscoreDialog.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface dialog) {
					TetrisActivity.this.finish();
				}
			});

			final EditText nameInput = (EditText) highscoreDialog.findViewById(R.id.name_text);
			final Button saveHighscore = (Button) highscoreDialog.findViewById(R.id.save_button);
			Button cancelDialog = (Button) highscoreDialog.findViewById(R.id.cancel_button);
			saveHighscore.setEnabled(false);

			nameInput.addTextChangedListener(new TextWatcher() {

				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) {
					if (count < 1) {
						saveHighscore.setEnabled(false);
					} else {
						saveHighscore.setEnabled(true);
					}
				}

				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {
				}

				@Override
				public void afterTextChanged(Editable s) {
				}
			});

			saveHighscore.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					highscoreDialog.dismiss();
					final Highscore hs = new Highscore();
					hs.setName(nameInput.getText().toString());
					hs.setPoints(gameService.getPoints());
					hs.setMultiplayer(isMultiplayer);
					new Thread(new Runnable() {
						public void run() {
							HighscoreDBAdapter dba = new HighscoreDBAdapter(TetrisActivity.this);
							dba.open();
							dba.insertHighscore(hs);
							dba.close();
							showHighscore();
						}
					}).start();
					TetrisActivity.this.finish();
				}
			});

			cancelDialog.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					highscoreDialog.cancel();
				}
			});
			return highscoreDialog;
		}
		return null;
	}

	/**
	 * Allows to enter a highscore if the specefied HIGHSCORE_MINIMUM has been reached. Otherwise
	 * the TetrisActivity will be finished without entering a highscore.
	 */
	private void handleGameOverInUIThread() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				setResult(RESULT_GAMEOVER);
				if (gameService.getPoints() >= HIGHSCORE_MINIMUM) {
					showDialog(DIALOG_NEW_HIGHSCORE);
				} else {
					finish();
				}
			}
		});
	}

	/**
	 * Starts the TetrisView
	 */
	private void startTetrisView() {
		tetrisView.doOnSurfaceCreated(new Runnable() {
			@Override
			public void run() {
				gameThread.start();
			}
		});
	}

	/**
	 * Does all the things that needs to be done as a game reaches the "gameover" status: It stops
	 * the GameThread and offers the possibility to enter a highscore if a HIGHSCORE_MINIMUM was
	 * reached
	 */
	public void onGameOver() {
		gameThread.setRunning(false);
		handleGameOverInUIThread();
	}

	/**
	 * Creates an Intent to starts the HighscoreActivity.
	 */
	private void showHighscore() {
		Intent intent = new Intent(this, HighscoreActivity.class);
		intent.putExtra(HighscoreActivity.MULTIPLAYER_SCORE, isMultiplayer);
		startActivity(intent);
	}

	/**
	 * Getter for GameService
	 * 
	 * @return gameService
	 */
	public GameService getGameService() {
		return gameService;
	}

	/**
	 * Getter for GameThread
	 * 
	 * @return gameThread
	 */
	public GameThread getGameThread() {
		return gameThread;
	}
}
