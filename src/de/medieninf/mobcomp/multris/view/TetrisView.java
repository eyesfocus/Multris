package de.medieninf.mobcomp.multris.view;

import android.content.Context;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import de.medieninf.mobcomp.multris.GameService;
import de.medieninf.mobcomp.multris.TetrisActivity;

/**
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         TetrisView is a SurfaceView that registers events the user initializes
 * 
 * */
public class TetrisView extends SurfaceView implements Callback, SensorEventListener {

	private SensorManager sensorManager;
	private long lastUpdate;
	private float lastZ, lastY;
	private Context context;
	private volatile GameService gameService;
	private volatile boolean surfaceCreated = false;
	private Object onSurfaceCreatedLock = new Object();
	private Runnable runOnSurfaceCreated = null;

	public TetrisView(Context context) {
		super(context);
		this.context = context;
		getHolder().addCallback(this);// Setzt Klasse als Handler fuer Events

		setFocusable(true);// GamePanel focusable, damit es Events handeln kann
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		lastUpdate = System.currentTimeMillis();
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

		synchronized (onSurfaceCreatedLock) {
			surfaceCreated = true;
			if (runOnSurfaceCreated != null) {
				runOnSurfaceCreated.run();
				runOnSurfaceCreated = null;
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	/**
	 * Setter for GameService
	 * 
	 * @param gameService
	 */
	public void setGameService(GameService gameService) {
		this.gameService = gameService;
	}

	public void doOnSurfaceCreated(Runnable doit) {
		synchronized (onSurfaceCreatedLock) {
			if (surfaceCreated) {
				doit.run();
			} else {
				runOnSurfaceCreated = doit;
			}
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		sensorManager.unregisterListener(this);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (gameService != null) {
			gameService.drawTetrisEnvironment(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			if (event.getY() > getHeight() - 200) {
				gameService.registerSpeedUp();
			} else if (event.getY() < 200) {
				gameService.registerRotate();
			} else {
				if (event.getX() < getWidth() / 2.0) {
					gameService.registerMoveLeft();
				}
				if (event.getX() > getWidth() / 2.0) {
					gameService.registerMoveRight();
				}
			}
		}
		if (event.getAction() == MotionEvent.ACTION_UP) {
			if (event.getY() > getHeight() - 200) {
				gameService.registerSlowDown();
			}
		}
		return true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			getAccelerometer(event);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private final static float THRESHOLD = 10.0f; // (dt. schwellwert)
	private final static long DEADPHASE = 250; // milliseconds

	private void getAccelerometer(SensorEvent event) {
		float[] values = event.values;
		// Movement
		float z = values[2];
		float y = values[1];
		float diffZ = lastZ > z ? lastZ - z : z - lastZ;
		float diffY = lastY > y ? lastY - y : y - lastY;
		float diff = diffZ + diffY;
		lastZ = z;
		lastY = y;
		long actualTime = System.currentTimeMillis();
		if (diff >= THRESHOLD) {
			if (actualTime - lastUpdate < DEADPHASE) {
				return;
			}
			lastUpdate = actualTime;
			gameService.registerRotate();
		}
	}

	/**
	 * Calls tick-Method in the GameService, if not game Over
	 * 
	 * */
	public void tick() {
		if (gameService != null) {
			if (!gameService.getGameOver()) {
				gameService.tick();
			} else {
				((TetrisActivity) context).onGameOver();
			}
		}
	}

	public void render(Canvas canvas) {
		onDraw(canvas);
	}
}
