package de.medieninf.mobcomp.multris.game;

import de.medieninf.mobcomp.multris.view.TetrisView;
import android.view.SurfaceHolder;
import android.graphics.Canvas;
import android.util.Log;

/**
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         This thread is running during a game. It handles gameprogress and its rendering
 * */
public class GameThread extends Thread {
	private static final String TAG = GameThread.class.getSimpleName();

	public final static int FPS = 40; // FPS
	private final static int FRAME_PERIOD = 1000 / FPS; // period in ms

	private SurfaceHolder surfaceHolder;
	private TetrisView tetrisView;
	private boolean running; // If game is running

	public GameThread(TetrisView tetrisView) {
		super();
		this.surfaceHolder = tetrisView.getHolder();
		this.tetrisView = tetrisView;
	}

	/** Getter for the current Thread state */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Setter for the current Thread state
	 * 
	 * @param running
	 */
	public void setRunning(boolean running) {
		this.running = running;
	}

	@Override
	public void run() {
		Canvas canvas;
		Log.d(TAG, "Starting game loop");

		long beginTime;
		long timeDiff;
		int sleepTime;

		sleepTime = 0;

		while (running) {
			beginTime = System.currentTimeMillis();
			// / Progress
			this.tetrisView.tick();
			canvas = null;
			// try locking the canvas for exclusive pixel edit ing on the
			// surface
			try {
				canvas = this.surfaceHolder.lockCanvas();
				synchronized (surfaceHolder) {
					// / view
					this.tetrisView.render(canvas);
					// How long did it take?
					timeDiff = System.currentTimeMillis() - beginTime;
					sleepTime = (int) (FRAME_PERIOD - timeDiff);

					if (sleepTime > 0) {
						try {
							Thread.sleep(sleepTime); //
						} catch (InterruptedException e) {
							throw new RuntimeException("Sleeping Thread interrupted", e);
						}
					}
				}
			} finally {
				// in case of an exception the surface is not left in
				// an inconsistent state
				if (canvas != null) {
					surfaceHolder.unlockCanvasAndPost(canvas);
				}
			} 
		}
	}
}
