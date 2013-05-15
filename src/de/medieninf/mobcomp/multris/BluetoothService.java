package de.medieninf.mobcomp.multris;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import de.medieninf.mobcomp.multris.game.GameState;
import de.medieninf.mobcomp.multris.game.data.Shape;
import de.medieninf.mobcomp.multris.game.data.Wall;
import de.medieninf.mobcomp.multris.network.BTMessage;
import de.medieninf.mobcomp.multris.network.ByteBufferPool;
import de.medieninf.mobcomp.multris.network.converters.IntegerConverter;
import de.medieninf.mobcomp.multris.network.converters.MessageConverter;
import de.medieninf.mobcomp.multris.network.helpers.DeviceContainer;

/**
 * 
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 * **/
public class BluetoothService extends Service implements Observer {

	private static final String TAG = BluetoothService.class.getSimpleName();

	// Name for the SDP record when creating server socket
	private static final String NAME = "Tetris";
	// Unique UUID for this application
	private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // do nothing
	public static final int STATE_LISTEN = 1; // listening for incoming
	public static final int STATE_CONNECTING = 2; // initiating an outgoing
	public static final int STATE_CONNECTED = 3; // connected to a remote
	public static final int STATE_DISCOVERING = 4; // initiating an outgoing

	// Member fields
	private BluetoothAdapter bluetoothAdapter;

	private Handler connectionHandler;

	// Bluetooth Threads
	private AcceptThread acceptThread;
	private ConnectThread connectThread;
	private DeserializeThread deserializeThread;
	private ArrayList<ConnectedThread> connections = new ArrayList<ConnectedThread>();
	private LinkedBlockingQueue<byte[]> queue;
	private ByteBufferPool byteBufferPool = new ByteBufferPool();

	private int connectionState;
	private String personalID;
	// private BluetoothDevice connectedDevice;

	private ArrayList<DeviceContainer> discoveredDevices = new ArrayList<DeviceContainer>();

	// Game Connection
	private GameService gameService;
	private boolean gameServiceBound;

	private int pauseCounter;

	private IBinder binder = new BluetoothServiceBinder();

	// BroadcastReceiver
	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				BluetoothDevice btdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (btdevice.getBondState() == BluetoothDevice.BOND_BONDED) {
					DeviceContainer device = new DeviceContainer(btdevice.getName(), btdevice.getAddress());
					discoveredDevices.add(device);
					connectionHandler.obtainMessage(ConnectionActivity.MSG_NEW_DEVICE_FOUND, device).sendToTarget();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				connectionHandler.obtainMessage(ConnectionActivity.MSG_DISCOVERY_FINISHED).sendToTarget();
				setState(STATE_NONE);
			}
		}
	};

	private ServiceConnection gameServiceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			gameService = ((GameService.GameServiceBinder) service).getService();
			gameService.setPlayerID(personalID);
			gameService.registerChangeObjectListener(BluetoothService.this);
			gameServiceBound = true;
		}

		public void onServiceDisconnected(ComponentName name) {
			gameService.deregisterChangeObjectListener();
			gameServiceBound = false;
		}
	};

	/**
	 * Class used for the client Binder.
	 */
	public class BluetoothServiceBinder extends Binder {
		public BluetoothService getService() {
			return BluetoothService.this;
		}
	}

	/**
	 * Binds GameService
	 * */
	private void doBindGameService() {
		Intent intent = new Intent(this, GameService.class);
		bindService(intent, gameServiceConnection, Context.BIND_AUTO_CREATE);
	}

	/**
	 * Unbinds GameService
	 * */
	private void doUnbindGameService() {
		if (gameServiceBound) {
			gameServiceBound = false;
			gameService.deregisterChangeObjectListener();
			unbindService(gameServiceConnection);
		}
	}

	@Override
	public void onCreate() {
		Log.v(TAG, "+++ ON CREATE +++");
		super.onCreate();

		// Register for broadcast reciever
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(broadcastReceiver, filter);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		personalID = bluetoothAdapter.getAddress(); // initial value - means no

		connectionState = STATE_NONE;
		doBindGameService();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v(TAG, "++ ON START COMMAND STICKY ++");
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.v(TAG, "+ ON BIND +");
		return binder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		Log.v(TAG, "- ON UNBIND  -");
		return super.onUnbind(intent);
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "--- ON DESTROY ---");
		super.onDestroy();
		doUnbindGameService();
	}

	/**
	 * Start Bluetooth Discovery
	 */
	public void startDiscovery() {
		if (bluetoothAdapter.isDiscovering()) {
			bluetoothAdapter.cancelDiscovery();
		}
		discoveredDevices.clear();
		bluetoothAdapter.startDiscovery();
		setState(STATE_DISCOVERING);
	}

	/**
	 * Cancels Bluetooth Discovery
	 * */
	public void cancelDiscovery() {
		bluetoothAdapter.cancelDiscovery();
	}

	/**
	 * Set the current state of the chat connection
	 * 
	 * @param state
	 *            An integer defining the current connection state
	 */
	private synchronized void setState(int state) {
		// Give the new state to the Handler so the UI Activity can update
		connectionState = state;
		connectionHandler.obtainMessage(ConnectionActivity.MSG_STATE_CHANGE, state, -1).sendToTarget();
	}

	/**
	 * Return the current connection state.
	 */
	public synchronized int getState() {
		return connectionState;
	}

	/**
	 * Tells GameService to handle GameState progress in the roll of the server
	 * 
	 * @param isServer
	 *            true if server, false if not
	 * */
	public void setMyDeviceAsServer(boolean isServer) {
		gameService.setServer(isServer);
	}

	/**
	 * @return true if server, false if not
	 * */
	public boolean isMyDeviceServer() {
		return gameService.isServer();
	}

	/**
	 * Start accepting incoming connections. Specifically start AcceptThread to begin a session in listening (server)
	 * mode. Called when the Server wants to create a game
	 * */
	public synchronized void start() {
		// Cancel any thread attempting to make a connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Cancel any thread that is holding a connection
		for (ConnectedThread connectedThread : connections) {
			if (connectedThread != null) {
				connectedThread.cancel();
				connectedThread = null;
			}
		}
		connections.clear();

		// Start the thread to listen on a BluetoothServerSocket
		if (acceptThread == null) {
			acceptThread = new AcceptThread();
			acceptThread.start();
		}
		setState(STATE_LISTEN);
	}

	/**
	 * Start the ConnectThread to initiate a connection to a remote device. Called when a Client wants to join a game
	 * 
	 * @param device
	 *            The BluetoothDevice to connect
	 */
	public synchronized void connect(String address) {
		BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
		// Cancel any thread attempting to make a connection
		if (connectionState == STATE_CONNECTING) {
			if (connectThread != null) {
				connectThread.cancel();
				connectThread = null;
			}
		}

		// Start the thread to connect with the given device
		connectThread = new ConnectThread(device);
		connectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * Start the ConnectedThread to begin managing a Bluetooth connection
	 * 
	 * @param socket
	 *            The BluetoothSocket on which the connection was made
	 * @param device
	 *            The BluetoothDevice that has been connected
	 */
	public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {

		// Cancel the thread that completed the connection
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		ConnectedThread connectedThread = new ConnectedThread(socket);
		synchronized (connections) {
			connections.add(connectedThread);
		}
		connectedThread.start();

		DeviceContainer foundDevice = new DeviceContainer(device.getName(), device.getAddress());

		if (isMyDeviceServer()) {
			discoveredDevices.add(foundDevice);
			// Send the name of the connected device back to the UI Activity
			connectionHandler.obtainMessage(ConnectionActivity.MSG_NEW_DEVICE_FOUND, foundDevice).sendToTarget();
		} else {
			connectionHandler.obtainMessage(ConnectionActivity.MSG_SERVER_CONNECTED, foundDevice).sendToTarget();
		}

		setState(STATE_CONNECTED);
	}

	/**
	 * Write to the ConnectedThread in an unsynchronized manner
	 * 
	 * @param out
	 *            The bytes to write
	 * @see ConnectedThread#write(byte[])
	 */
	public void write(BTMessage btmsg, boolean broadcast) {
		ConnectedThread r; // Create temporary object
		byte[] byteMsg = MessageConverter.serializeObject(btmsg);
		int playerCount = connections.size() + 1;

		for (ConnectedThread connectedThread : connections) {
			if (btmsg.getmID() == BTMessage.START_GAME) {
				int playerNo = connections.indexOf(connectedThread) + 1;
				btmsg.setData(new Integer[] { playerNo, playerCount });
				byteMsg = MessageConverter.serializeObject(btmsg);
			}

			if (broadcast || !btmsg.getpID().equals(connectedThread.getDeviceAddress())) {
				synchronized (this) { // Synchronize a copy of the
										// ConnectedThread
					if (connectionState != STATE_CONNECTED) {
						return;
					}
					r = connectedThread;
				}
				r.write(byteMsg);
			}
		}
	}

	/**
	 * Stop AcceptThread so that no further incoming connections are possible
	 * */
	public synchronized void stopAccepting() {
		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}
	}

	/**
	 * Stop all threads
	 */
	public synchronized void stop() {
		if (connectThread != null) {
			connectThread.cancel();
			connectThread = null;
		}

		for (ConnectedThread connectedThread : connections) {
			if (connectedThread != null) {
				connectedThread.cancel();
				connectedThread = null;
			}
		}
		connections.clear();

		if (deserializeThread != null) {
			deserializeThread.cancel();
			deserializeThread = null;
		}

		if (acceptThread != null) {
			acceptThread.cancel();
			acceptThread = null;
		}
		setState(STATE_NONE);
	}

	/**
	 * Indicate that the connection attempt failed and notify the UI Activity.
	 */
	private void connectionFailed() {
		setState(STATE_NONE);

		// Send a failure message back to the Activity
		Message msg = connectionHandler.obtainMessage(ConnectionActivity.MSG_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(ConnectionActivity.TOAST, "CONNECTION FAILED");
		msg.setData(bundle);
		connectionHandler.sendMessage(msg);
	}

	/**
	 * Indicate that the connection was lost and notify the UI Activity.
	 */
	private void connectionLost(ConnectedThread connectedThread) {
		gameService.setPaused(false);
		String connectedDeviceAddress = connectedThread.getDeviceAddress();

		// find connected device in discoveredDevices lists
		for (DeviceContainer device : discoveredDevices) {
			if (device.getAddress().equals(connectedDeviceAddress)) {
				DeviceContainer disconnectedDevice = device;
				if (discoveredDevices.remove(disconnectedDevice)) {
					connectionHandler.obtainMessage(ConnectionActivity.MSG_CONNECTION_TO_DEVICE_LOST, disconnectedDevice).sendToTarget();
				}
				break;
			}
		}

		stop(); // stop every thread

		// Send a failure message back to the Activity
		Message msg = connectionHandler.obtainMessage(ConnectionActivity.MSG_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(ConnectionActivity.TOAST, "Connection was lost");
		msg.setData(bundle);
		connectionHandler.sendMessage(msg);
	}

	/**
	 * This thread runs while listening for incoming connections. It behaves like a server-side client. It runs until
	 * cancelled.
	 */
	private class AcceptThread extends Thread {
		// The local server socket
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			// Create a new listening server socket
			try {
				tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {

			}
			mmServerSocket = tmp;
		}

		@Override
		public void run() {
			setName("AcceptThread");
			BluetoothSocket socket = null;

			while (true) {
				try {
					// This is a blocking call and will only return on a
					// successful connection or an exception
					socket = mmServerSocket.accept();

				} catch (IOException e) {

					break;
				}

				if (socket != null) { // If a connection was accepted
					synchronized (BluetoothService.this) {
						switch (connectionState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// Situation normal. Start the connected thread.
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// connect if there's no connection yet
							if (!alreadyConnected(socket)) {
								connected(socket, socket.getRemoteDevice());
								break;
							}
							try {
								socket.close();
							} catch (IOException e) {

							}
							break;
						}
					}
				}
			}
		}

		/**
		 * Checks if there's already an open connection to this socket.
		 * */
		private boolean alreadyConnected(BluetoothSocket socket) {
			for (ConnectedThread connectedThread : connections) {
				if (socket.getRemoteDevice().equals(connectedThread.getDevice())) {
					return true;
				}
			}
			return false;
		}

		public void cancel() {
			try {
				mmServerSocket.close();
			} catch (IOException e) {

			}
		}
	}

	/**
	 * This thread runs while attempting to make an outgoing connection with a device. It runs straight through; the
	 * connection either succeeds or fails.
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// Get BluetoothSocket for connection with the given BluetoothDevice
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {

			}
			mmSocket = tmp;
		}

		@Override
		public void run() {
			setName("ConnectThread");

			// Always cancel discovery because it will slow down a connection
			bluetoothAdapter.cancelDiscovery();

			// Make a connection to the BluetoothSocket
			try {
				// This is a blocking call and will only return on a
				// successful connection or an exception
				mmSocket.connect();
			} catch (IOException e) {
				connectionFailed();
				// Close the socket
				try {
					mmSocket.close();
				} catch (IOException e2) {

				}
				return;
			}

			// Reset the ConnectThread because we're done
			synchronized (BluetoothService.this) {
				connectThread = null;
			}

			// Start the connected thread
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {

			}
		}
	}

	/**
	 * This thread runs during a connection with a remote device. It handles all incoming and outgoing transmissions.
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket connectedSocket;
		private final InputStream inStream;
		private final OutputStream outStream;

		public ConnectedThread(BluetoothSocket socket) {
			setName("ConnectedThread");
			connectedSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {

			}

			inStream = tmpIn;
			outStream = tmpOut;

			if (deserializeThread == null) { //
				queue = new LinkedBlockingQueue<byte[]>();
				deserializeThread = new DeserializeThread();
				deserializeThread.start();
			}
		}

		/**
		 * Reads a buffer with a specific number of bytes to read.
		 * 
		 * @param buffer
		 *            buffer that is read
		 * @param sizeBytesToRead
		 *            number of bytes to be read
		 * @throws IOException
		 *             if size of actual read bytes > the size of bytes to read
		 * */
		private void readBytes(byte[] buffer, int sizeBytesToRead) throws IOException {
			int alreadyRead = 0;
			while (sizeBytesToRead - alreadyRead > 0) {
				int actualRead;
				actualRead = inStream.read(buffer, alreadyRead, sizeBytesToRead - alreadyRead);
				alreadyRead += actualRead;
				if (actualRead == -1) {
					throw new IOException("readBytes: actualRead == -1. not possible");
				}
			}
		}

		@Override
		public void run() {
			final int SIZE_IN_BYTES = 4;
			byte[] sizeBuffer = new byte[SIZE_IN_BYTES]; // buffer to store
															// datasize

			try {
				// Keep listening to the InputStream while connected
				while (true) {
					readBytes(sizeBuffer, SIZE_IN_BYTES); // Read dataSize from
															// ips
					int dataSize = IntegerConverter.bytearrayToInt(sizeBuffer);
					// buffer to store and dispatch data
					byte[] dataBuffer = byteBufferPool.get(dataSize);
					readBytes(dataBuffer, dataSize); // Read data from ips
					queue.put(dataBuffer);
				}
			} catch (IOException ioe) {
				connectionLost(this);
			} catch (InterruptedException ioe) {
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				outStream.write(buffer);
			} catch (IOException e) {

			}
		}

		public BluetoothDevice getDevice() {
			return connectedSocket.getRemoteDevice();
		}

		public String getDeviceAddress() {
			return getDevice().getAddress();
		}

		public void cancel() {
			try {
				connectedSocket.close();
			} catch (IOException e) {

			}
		}
	}

	/**
	 * This Thread runs as long the ConnectedThread runs and works as a Cusumer. It contains a queue that stores
	 * messages that have been received from the ConnectedThread. As long as messages are stored in this queue they will
	 * be deserialized and processed to the application
	 */
	private class DeserializeThread extends Thread {

		public DeserializeThread() {
			setName("DeserializeThread");
		}

		@Override
		public void run() {
			try {
				while (true) {
					byte[] buffer = queue.take();
					BTMessage btmsg = (BTMessage) MessageConverter.deserializeObject(buffer);
					byteBufferPool.recycle(buffer);
					process(btmsg);
				}
			} catch (InterruptedException e) {

			}
		}

		private void process(BTMessage btmsg) throws InterruptedException {

			int mId = btmsg.getmID();

			switch (mId) {
			case BTMessage.PAUSE_GAME:
				if (isMyDeviceServer()) {
					pauseCounter += 1;
				}
				gameService.handleIncomingPause();
				break;
			case BTMessage.RESUME_GAME:
				if (isMyDeviceServer()) {
					pauseCounter -= 1;
				}
				gameService.handleIncomingResume();
				break;
			case BTMessage.START_GAME:
				// Send "ready to start" to UI Activity
				Integer[] gameInfo = ((Integer[]) btmsg.getData());
				int playerNo = gameInfo[0];
				int playerCount = gameInfo[1];
				Message msg = connectionHandler.obtainMessage(ConnectionActivity.MSG_START_GAME, playerNo, playerCount);
				connectionHandler.sendMessage(msg);
				break;
			default:
				gameService.setIncomingData(btmsg);
			}

			if (isMyDeviceServer()) {
				if (mId == BTMessage.RESUME_GAME) {
					if (pauseCounter == 0) {
						BluetoothService.this.write(btmsg, false); // an alle anderen
						// weiterleiten
					}
				} else {
					BluetoothService.this.write(btmsg, false); // an alle anderen
					// weiterleiten
				}

			}

		}

		public void cancel() {
			this.interrupt();
		}
	}

	public void setConnectionHandler(final Handler connectionHandler) {
		this.connectionHandler = connectionHandler;
	}

	public ArrayList<DeviceContainer> getDiscoveredDevices() {
		return discoveredDevices;
	}

	@Override
	public void update(Observable observable, Object data) {
		if (observable instanceof Shape) { // both sides to this: client and
											// server
			String pid = (String) data;
			Shape shape = (Shape) observable;
			this.write(new BTMessage(BTMessage.SHAPE, pid, shape), false);
		} else if (observable instanceof Wall) { // only server does this
			String pid = (String) data;
			Wall wall = (Wall) observable;
			this.write(new BTMessage(BTMessage.WALL, pid, wall), true);
		} else if (observable instanceof GameState) {
			if (data instanceof String) {
				String pid = (String) data;
				GameState gameState = (GameState) observable;
				if (gameState.getGameOver()) { // also only server
					this.write(new BTMessage(BTMessage.GAME_OVER, pid, gameState.getGameOver()), true);
				} else if (gameState.isScoreChanged()) { // also only server
					this.write(new BTMessage(BTMessage.POINTS, pid, gameState.getPoints()), true);
				} else if (gameState.isPaused()) { // also clients
					if (isMyDeviceServer()) {
						pauseCounter += 1;
					}
					this.write(new BTMessage(BTMessage.PAUSE_GAME, pid), false);
				} else if (!gameState.isPaused()) { // also clients
					if (isMyDeviceServer()) {
						pauseCounter -= 1;
					}
					this.write(new BTMessage(BTMessage.RESUME_GAME, pid), false);
				}
			}
		}
	}

}