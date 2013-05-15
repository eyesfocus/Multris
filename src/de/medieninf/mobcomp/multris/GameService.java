package de.medieninf.mobcomp.multris;

import java.util.Observable;
import java.util.Observer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import de.medieninf.mobcomp.multris.R;
import de.medieninf.mobcomp.multris.game.GameState;
import de.medieninf.mobcomp.multris.game.data.Shape;
import de.medieninf.mobcomp.multris.network.BTMessage;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         GameService is a Service that allows clients to bind to this service. This service offers all the methods to
 *         handle the tetris game, such as start, stop, pause, resume the game.
 * 
 */
public class GameService extends Service implements Observer {
	private static final String TAG = GameService.class.getSimpleName();

	private IBinder binder = new GameServiceBinder();

	private volatile Observer changeObjectListener;
	private boolean isServer;

	// for drawing
	private Bitmap[] bitmaps;
	private int bricksize;

	// game components
	private GameState gameState;
	
	// handler to communicate pause and resume events that are caused by other
	// devices back to the UI Activity (TetrisActivity)
	private Handler gameHandler;

	private String myId;

	// game field dimensions
	private int left, right, top, bottom;

	public class GameServiceBinder extends Binder {
		public GameService getService() {
			return GameService.this;
		}
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "+++ ON CREATE +++");
		super.onCreate();
		// init presentation
		initMeasures();
		initBitmaps();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "++ ON START COMMAND ++");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "--- ON DESTROY ---");
		super.onDestroy();
	}

	/**
	 * Initializes the GameState
	 * 
	 * */
	public void initNewGame(boolean isMultiplayer, int playerNo, int playerCount) {
		// initGameState
		gameState = new GameState(isServer, isMultiplayer, playerNo, playerCount);
		gameState.setMyId(myId);

		// Add this Service as Observer for getting information about the
		// changes of the GameState-Components
		if (isMultiplayer) {
			gameState.getMyShape().addObserver(this);
			gameState.addObserver(this);
			if (isServer) {
				gameState.getWall().addObserver(this);
			}
		}
	}

	/**
	 * Calculates the measures of the game area
	 * 
	 * @return number of rows
	 * */
	private void initMeasures() {
		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		int width = display.getWidth();
		int height = display.getHeight();
		bricksize = width / GameState.COLS;
		if (height / GameState.ROWS < bricksize) {
			bricksize = height / GameState.ROWS;
		}
		left = 0;
		right = bricksize * GameState.COLS;
		top = height - bricksize * GameState.ROWS;
		bottom = bricksize * GameState.ROWS + top;
	}

	/**
	 * Initializes the Bitmaps that are used to display the Shapes and stores them in the global Array "bitmaps".
	 */
	private void initBitmaps() {
		Bitmap bm_0 = BitmapFactory.decodeResource(getResources(), R.drawable.brick_transparent);
		Bitmap bm_blue = BitmapFactory.decodeResource(getResources(), R.drawable.brick_o);
		Bitmap bm_cyan = BitmapFactory.decodeResource(getResources(), R.drawable.brick_t);
		Bitmap bm_green = BitmapFactory.decodeResource(getResources(), R.drawable.brick_i);
		Bitmap bm_magenta = BitmapFactory.decodeResource(getResources(), R.drawable.brick_j);
		Bitmap bm_orange = BitmapFactory.decodeResource(getResources(), R.drawable.brick_l);
		Bitmap bm_red = BitmapFactory.decodeResource(getResources(), R.drawable.brick_s);
		Bitmap bm_yellow = BitmapFactory.decodeResource(getResources(), R.drawable.brick_z);

		bitmaps = new Bitmap[8];
		bitmaps[0] = Bitmap.createScaledBitmap(bm_0, bricksize, bricksize, false);
		bitmaps[1] = Bitmap.createScaledBitmap(bm_blue, bricksize, bricksize, false);
		bitmaps[2] = Bitmap.createScaledBitmap(bm_cyan, bricksize, bricksize, false);
		bitmaps[3] = Bitmap.createScaledBitmap(bm_green, bricksize, bricksize, false);
		bitmaps[4] = Bitmap.createScaledBitmap(bm_magenta, bricksize, bricksize, false);
		bitmaps[5] = Bitmap.createScaledBitmap(bm_orange, bricksize, bricksize, false);
		bitmaps[6] = Bitmap.createScaledBitmap(bm_red, bricksize, bricksize, false);
		bitmaps[7] = Bitmap.createScaledBitmap(bm_yellow, bricksize, bricksize, false);
	}

	/**
	 * HelperMethod to draw Wall and Shapes of GameState
	 * 
	 * @param canvas
	 *            Canvas the environment is drawn on
	 */
	public void drawTetrisEnvironment(Canvas canvas) {
		if (canvas != null) {
			canvas.drawColor(getResources().getColor(R.color.one));
			Paint fieldPaint = new Paint();
			fieldPaint.setColor(getResources().getColor(R.color.one));
			canvas.drawRect(new Rect(left, top, right, bottom), fieldPaint);
			gameState.getWall().draw(canvas, bitmaps, top, left, bricksize);
			// Draw myShape
			gameState.getMyShape().draw(canvas, bitmaps, bricksize, top, left, true);
			// Draw otherShapes
			for (Shape shape : gameState.getOtherShapes()) {
				shape.draw(canvas, bitmaps, bricksize, top, left, false);
			}

			Paint textPaint = new Paint();
			textPaint.setTextAlign(Paint.Align.LEFT);
			textPaint.setColor(getResources().getColor(R.color.white));
			textPaint.setTextSize(20);
			canvas.drawText("Score: " + String.valueOf(gameState.getPoints()), 0, top + textPaint.getTextSize(), textPaint);
		}
	}

	/**
	 * Delegates the speedUp Event to the GameState
	 */
	public void registerSpeedUp() {
		gameState.registerSpeedUp();

	}

	/**
	 * Delegates the slowDown Event to the GameState
	 */
	public void registerSlowDown() {
		gameState.registerSlowDown();

	}

	/**
	 * Delegates the rotate Event to the GameState
	 */
	public void registerRotate() {
		gameState.registerRotate();

	}

	/**
	 * Delegates the moveLeft Event to the GameState
	 */
	public void registerMoveLeft() {
		gameState.registerMoveLeft();

	}

	/**
	 * Delegates the moveRight Event to the GameState
	 */
	public void registerMoveRight() {
		gameState.registerMoveRight();
	}

	/**
	 * Delegates incoming events to the gameState
	 * 
	 * @param pId
	 * @param data
	 *            new data
	 * */
	public void setIncomingData(BTMessage message) {
		if (gameState != null) {
			gameState.setIncomingData(message);
		}
	}

	/**
	 * Delegates the tick to the GameState
	 */
	public void tick() {
		gameState.tick();
	}

	public void setPlayerID(String id) {
		myId = id;
	}

	public boolean isMultiplayer() {
		return gameState.isMultiplayerMode();
	}

	public boolean isServer() {
		return isServer;
	}

	public void setServer(boolean isServer) {
		this.isServer = isServer;
	}

	/**
	 * Setter for pause
	 * 
	 * @param pause
	 */
	public void setPaused(boolean pause) {
		if (gameState != null) {
			gameState.setPaused(pause, true);
		}
	}

	/**
	 * Getter for pause
	 * 
	 * @return true if paused, otherwise false
	 */
	public boolean isPaused() {
		if (gameState == null) { // there is no active game running at the
									// moment
			return false;
		} else {
			return gameState.isPaused();
		}
	}

	/**
	 * Getter for points
	 * 
	 * @return points
	 */
	public int getPoints() {
		return gameState.getPoints();
	}

	/**
	 * Getter for gameover
	 * 
	 * @return gameover
	 */
	public boolean getGameOver() {
		return gameState.getGameOver();
	}

	@Override
	public void update(Observable observable, Object data) {
		if (observable instanceof Shape) {
			data = myId;
		}
		if (changeObjectListener != null) {
			changeObjectListener.update(observable, data);
		}
	}

	/**
	 * Registers a Listener to communicate events to the BluetoothService
	 * 
	 * @param changeObjectListener
	 */
	public void registerChangeObjectListener(Observer changeObjectListener) {
		this.changeObjectListener = changeObjectListener;
	}

	/**
	 * Deregisters the Listener
	 */
	public void deregisterChangeObjectListener() {
		this.changeObjectListener = null;
	}

	/**
	 * Setter for game handler
	 * 
	 * @param gameHandler
	 */
	public void setGameHandler(Handler gameHandler) {
		this.gameHandler = gameHandler;

	}

	/**
	 * Handles incoming pause events caused by other decives
	 */
	public void handleIncomingPause() {
		gameHandler.obtainMessage(BTMessage.PAUSE_GAME).sendToTarget();
		gameState.setPaused(true, false);
	}

	/**
	 * Handles incoming resume events caused by other devices
	 */
	public void handleIncomingResume() {
		gameHandler.obtainMessage(BTMessage.RESUME_GAME).sendToTarget();
		gameState.setPaused(false, false);
	}
}
