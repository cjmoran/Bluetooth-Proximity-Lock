package com.javadog.bluetoothproximitylock;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * This Service runs a background thread which periodically updates the signal strength.
 */
public class SignalReaderService extends Service {
	final static String BT_SIGNAL_STRENGTH_ACTION = "com.javadog.bluetoothproximitylock.UPDATE_BT_SS";
	private final static long REFRESH_INTERVAL_MS = 2000;    //TODO: Allow user to specify

	private static boolean iAmRunning;

	private SignalStrengthLoader loader;

	@Override
	public void onCreate() {
		loader = new SignalStrengthLoader();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		loader = new SignalStrengthLoader();
		loader.execute();

		Log.i(MainActivity.DEBUG_TAG, "Bluetooth proximity service started.");

		iAmRunning = true;

		//Keep the service in a "started" state even if killed for memory
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if(loader != null) {
			loader.plzStop();
			loader.cancel(true);
		}

		iAmRunning = false;

		Log.i(MainActivity.DEBUG_TAG, "Bluetooth proximity service stopped.");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static boolean isServiceRunning() {
		return iAmRunning;
	}

	/**
	 * Implementation of AsyncTask which periodically refreshes signal strength.
	 */
	class SignalStrengthLoader extends AsyncTask<Void, Integer, Void> { //TODO: Pass in chosen bluetooth device ID instead of picking the first one in BTManager
		private BluetoothManager manager;
		private int signalStrength;
		private boolean plzStop;

		protected void plzStop() {
			plzStop = true;
		}

		@Override
		protected void onPreExecute() {
			manager = new BluetoothManager(getApplicationContext());
			plzStop = false;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			while(manager != null && !plzStop) {
				//Read remote RSSI. If we have a request for it out already, don't request again.
				if(manager.canReadRssi()) {
					manager.getBtGatt().readRemoteRssi();
				}

				//Get signal strength from the BTManager
				signalStrength = manager.getSignalStrength();

				//Post the new value as "progress"
				publishProgress(signalStrength);

				Log.v(MainActivity.DEBUG_TAG, "Read signal strength: " + signalStrength);

				//Sleep this thread for the provided time
				try {
					Thread.sleep(REFRESH_INTERVAL_MS);
				} catch(InterruptedException e) {
					Log.w(MainActivity.DEBUG_TAG, "Signal strength thread interrupted. " +
							"User probably killed the service.");
				}
			}

			return null;
		}

		/**
		 * Signal strength represented in dBm (Short).
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			int updatedSignalStrength = values[0];

			//Locally broadcast the new signal strength value using an Intent extra
			LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
			Intent i = new Intent(BT_SIGNAL_STRENGTH_ACTION);
			i.putExtra("BTSignalStrength", updatedSignalStrength);
			broadcastManager.sendBroadcast(i);

			Log.d(MainActivity.DEBUG_TAG, "Local BT signal broadcast sent.");
		}
	}
}
