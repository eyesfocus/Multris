package de.medieninf.mobcomp.multris;

import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.medieninf.mobcomp.multris.R;
import de.medieninf.mobcomp.multris.network.BTMessage;
import de.medieninf.mobcomp.multris.network.helpers.DeviceContainer;

/**
 * @author tina schedlbauer
 * @author marcel bechtold
 * 
 *         Activity to set up a new game with more players. The user can choose between client and server roll. There
 *         can be only one player per game.
 */
public class ConnectionActivity extends Activity {
	private static final String TAG = ConnectionActivity.class.getSimpleName();

	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_START_GAME = 2;
	private static final int REQUEST_MAKE_DISCOVERABLE = 3;

	// Hack for confirm code of returning REQUEST_MAKE_DISCOVERABLE in onActivityResult
	private static final int RESULT_DISCOVERABLE_OK = 10;
	public static final int DISCOVERABLE_DURATION = 10;

	// Message types sent from the Handler
	public static final int MSG_STATE_CHANGE = 1;
	public static final int MSG_TOAST = 2;
	public static final int MSG_START_GAME = 3;
	public static final int MSG_NEW_DEVICE_FOUND = 4;
	public static final int MSG_DISCOVERY_FINISHED = 5;
	public static final int MSG_CONNECTION_TO_DEVICE_LOST = 6;
	public static final int MSG_SERVER_CONNECTED = 7;

	// Key names received from the handler
	public static final String TOAST = "toast";

	// Layout Views
	private Button createGameButton, searchGameButton, startGameButton;
	private ProgressBar progressBar;
	private TextView listTitle;
	private DeviceAdapter devicesAdapter;

	// Local Bluetooth adapter
	private BluetoothAdapter bluetoothAdapter = null;

	// Member objects for the bluetoothService
	private boolean bluetoothServiceBound;
	private BluetoothService bluetoothService;

	private ServiceConnection bluetoothConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			bluetoothService = ((BluetoothService.BluetoothServiceBinder) service).getService();
			bluetoothService.setConnectionHandler(connectionHandler);
			updateButtons(bluetoothService.getState());
			refreshDeviceAdapter();
		}

		public void onServiceDisconnected(ComponentName name) {

		}
	};

	// The Handler that gets information back from the BluetoothChatService
	private final Handler connectionHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_STATE_CHANGE:
				updateButtons(msg.arg1);
				break;
			case MSG_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			case MSG_START_GAME:
				int playerNo = msg.arg1;
				int playerCount = msg.arg2;
				goToGame(playerNo, playerCount);
				break;
			case MSG_NEW_DEVICE_FOUND:
				devicesAdapter.add((DeviceContainer) msg.obj);
				break;
			case MSG_CONNECTION_TO_DEVICE_LOST:
				devicesAdapter.remove((DeviceContainer) msg.obj);
				break;
			case MSG_DISCOVERY_FINISHED:
				progressBar.setVisibility(View.INVISIBLE);
				listTitle.setText("Verfuegbare Spiele");
				break;
			case MSG_SERVER_CONNECTED:
				devicesAdapter.setDeviceConnected((DeviceContainer) msg.obj, true);
				devicesAdapter.notifyDataSetChanged();
				break;
			}
		}
	};

	// Listener for Buttons
	private OnClickListener buttonListener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			int viewId = v.getId();
			switch (viewId) {
			case R.id.create_multiplayer_game:
				if (!(Boolean) v.getTag()) {
					ensureDiscoverable();
				} else {
					bluetoothService.stop(); // close every connection and go to state_none
				}
				break;
			case R.id.search_multiplayer_game:
				if (bluetoothService.getState() == BluetoothService.STATE_DISCOVERING) {
					bluetoothService.cancelDiscovery();
				} else {
					startDiscovery(); // set up client
				}
				break;
			case R.id.start_multiplayer_game:
				if (bluetoothService.isMyDeviceServer()) {
					bluetoothService.stopAccepting();
					bluetoothService.write(new BTMessage(BTMessage.START_GAME), true);
				}
				// 0 because this device always is server
				goToGame(0, 1);
				break;
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.v(TAG, "+++ ON CREATE +++");

		// Set up the window layout
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_connection);

		// init layout views
		devicesAdapter = new DeviceAdapter(this, R.layout.list_view_item);
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(devicesAdapter);

		bluetoothServiceBound = false;

		// Get local Bluetooth adapter
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// If the adapter is null, then Bluetooth is not supported
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		// init layout
		progressBar = (ProgressBar) findViewById(R.id.progress_bar_device_scan);
		listTitle = (TextView) findViewById(R.id.list_title);
		createGameButton = (Button) findViewById(R.id.create_multiplayer_game);
		searchGameButton = (Button) findViewById(R.id.search_multiplayer_game);
		startGameButton = (Button) findViewById(R.id.start_multiplayer_game);

		// add listeners
		createGameButton.setOnClickListener(buttonListener);
		searchGameButton.setOnClickListener(buttonListener);
		startGameButton.setOnClickListener(buttonListener);
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.v(TAG, "++ ON START ++");

		// If BT is not on, request that it be enabled.
		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			doStartBluetoothService();
			doBindBluetoothService();
		}
	}

	@Override
	public synchronized void onResume() {
		Log.v(TAG, "+ ON RESUME +");
		super.onResume();
	}

	@Override
	public synchronized void onPause() {
		Log.v(TAG, "- ON PAUSE -");
		super.onPause();
	}

	@Override
	public void onStop() {
		Log.v(TAG, "-- ON STOP --");
		super.onStop();
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "--- ON DESTROY ---");
		doUnbindBluetoothService();
		super.onDestroy();
	}

	/**
	 * Binds the BluetoothService to this Activity, so that its methods can be used in this Activity.
	 */
	private void doBindBluetoothService() {
		Intent intent = new Intent(this, BluetoothService.class);
		bindService(intent, bluetoothConnection, Context.BIND_AUTO_CREATE);
		bluetoothServiceBound = true;
	}

	/**
	 * Unbinds BluetootService
	 */
	private void doUnbindBluetoothService() {
		if (bluetoothServiceBound) {
			bluetoothServiceBound = false;
			unbindService(bluetoothConnection);
			bluetoothConnection = null;
		}
	}

	/**
	 * Starts BLuetoothService
	 * */
	private void doStartBluetoothService() {
		Intent intent = new Intent(this, BluetoothService.class);
		startService(intent);
	}

	/**
	 * Start device discovery in BluetoothService
	 */
	private void startDiscovery() {
		listTitle.setText("Suche nach Spielen...");
		bluetoothService.setMyDeviceAsServer(false);
		progressBar.setVisibility(View.VISIBLE);
		devicesAdapter.clear();
		bluetoothService.startDiscovery();
	}

	/**
	 * Makes an Intent to start TetrisActivity
	 * */
	private void goToGame(int playerNo, int playerCount) {
		Intent intent = new Intent(ConnectionActivity.this, TetrisActivity.class);
		intent.putExtra(TetrisActivity.EXTRA_MULTIPLAYER, true);
		intent.putExtra(TetrisActivity.EXTRA_PLAYER_NO, playerNo);
		intent.putExtra(TetrisActivity.EXTRA_PLAYER_COUNT, playerCount);
		startActivityForResult(intent, REQUEST_START_GAME);
	}

	/**
	 * Prepare BluetoothService setup as server
	 * */
	private void prepareServer() {
		bluetoothService.setMyDeviceAsServer(true);
		bluetoothService.start();
		devicesAdapter.clear();
	}

	/**
	 * Ensures Bluetooth Discovery
	 * */
	private void ensureDiscoverable() {
		if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
			startActivityForResult(discoverableIntent, REQUEST_MAKE_DISCOVERABLE);
		}
	}

	/**
	 * Updates the UI Components according to the state of the BluetoothService
	 * 
	 * */
	private void updateButtons(int state) {
		if (state == BluetoothService.STATE_NONE) {
			createGameButton.setTag(false);
			createGameButton.setEnabled(true);
			createGameButton.setText(R.string.button_create);
			searchGameButton.setEnabled(true);
			searchGameButton.setText(R.string.button_search);
			startGameButton.setEnabled(false);
			return;
		}

		boolean server = bluetoothService.isMyDeviceServer();
		if (server) {
			switch (state) {
			case BluetoothService.STATE_LISTEN:
				createGameButton.setTag(true);
				createGameButton.setText(R.string.button_close);
				searchGameButton.setEnabled(false);
				startGameButton.setEnabled(false);
				break;
			case BluetoothService.STATE_CONNECTED:
				createGameButton.setEnabled(true);
				createGameButton.setTag(true);
				createGameButton.setText(R.string.button_close);
				searchGameButton.setEnabled(false);
				startGameButton.setEnabled(true);
				break;
			}
		} else {
			switch (state) {
			case BluetoothService.STATE_DISCOVERING:
				createGameButton.setEnabled(false);
				searchGameButton.setEnabled(true);
				searchGameButton.setText(R.string.button_cancel_search);
				startGameButton.setEnabled(false);
				break;
			case BluetoothService.STATE_CONNECTED:
				createGameButton.setEnabled(false);
				searchGameButton.setEnabled(false);
				startGameButton.setEnabled(false);
				break;
			}
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) { // User enabled Bluetooth
				doStartBluetoothService();
				doBindBluetoothService();
			} else {// User did not enable Bluetooth or an error occured
				finish();
			}
		case REQUEST_START_GAME:
			// When the request to start the game returns
			break;
		case REQUEST_MAKE_DISCOVERABLE:
			// When the request to start the game returns
			if (resultCode == RESULT_DISCOVERABLE_OK) {
				prepareServer();
			}
			break;
		}
	}

	/**
	 * Refreshes the deviceAdapter depending on the devices that are saved in BluetoothService
	 */
	private void refreshDeviceAdapter() {
		devicesAdapter.clear();
		devicesAdapter.addAll(bluetoothService.getDiscoveredDevices());
	}

	/**
	 * Adapter to represent the discovered devices
	 * 
	 * */
	private class DeviceAdapter extends ArrayAdapter<DeviceContainer> {

		Context context;
		int layoutResource;

		public DeviceAdapter(Context context, int layoutResourceId) {
			super(context, layoutResourceId);
			this.layoutResource = layoutResourceId;
			this.context = context;
		}

		@Override
		public void add(DeviceContainer object) {
			super.add(object);
		}

		@Override
		public DeviceContainer getItem(int position) {
			return super.getItem(position);
		}

		public void addAll(ArrayList<DeviceContainer> newDevices) {
			for (DeviceContainer device : newDevices) {
				super.add(device);
			}
		}

		public void setDeviceConnected(DeviceContainer device, boolean connected) {
			device.setConnected(connected);
			int position = super.getPosition(device);
			getItem(position).setConnected(connected);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			DeviceHolder holder = null;

			if (row == null) {
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResource, parent, false);

				holder = new DeviceHolder();
				holder.txtName = (TextView) row.findViewById(R.id.device_name);
				holder.txtAddress = (TextView) row.findViewById(R.id.device_address);
				holder.buttonState = (Button) row.findViewById(R.id.server_state_button);
				row.setTag(holder);

				holder.buttonState.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View view) {
						// Cancel discovery because costly. and about to connect
						bluetoothService.cancelDiscovery();
						View v = (View) view.getParent(); // the row!

						// Get MAC address
						DeviceHolder holder = (DeviceHolder) v.getTag();
						String address = (String) (holder.txtAddress).getText();

						// Attempt to connect to the device
						bluetoothService.connect(address);
					}
				});
			} else {
				holder = (DeviceHolder) row.getTag();
			}

			DeviceContainer device = this.getItem(position);
			holder.txtName.setText(device.getName());
			holder.txtAddress.setText(device.getAddress());

			// show no button if i am server!
			if (bluetoothService.isMyDeviceServer()) {
				holder.buttonState.setVisibility(View.INVISIBLE);
			} else {
				holder.buttonState.setEnabled(!device.isConnected());
			}

			return row;
		}

		class DeviceHolder {
			TextView txtName;
			TextView txtAddress;
			Button buttonState;
		}
	}
}