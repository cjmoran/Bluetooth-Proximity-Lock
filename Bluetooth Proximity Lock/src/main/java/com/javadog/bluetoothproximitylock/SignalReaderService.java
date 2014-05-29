/*
	Copyright 2014 Cullin Moran

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

	    http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
 */

package com.javadog.bluetoothproximitylock;

import android.app.Service;
import android.content.Context;
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
	final static String BT_ENABLE_BUTTON_ACTION = "com.javadog.bluetoothproximitylock.BT_ENABLE_BUTTON";

	private static long refreshIntervalMs;
	private static boolean iAmRunning;

	private SignalStrengthLoader loader;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		//Refresh interval should have been passed in the intent
		refreshIntervalMs = intent.getLongExtra("btRefreshInterval", 2000);

		loader = new SignalStrengthLoader();
		loader.execute();

		iAmRunning = true;

		Log.i(MainActivity.DEBUG_TAG, "Bluetooth proximity service started.");

		//Service has started now, so we can enable the fragment button.
		enableFragmentButton();

		//Keep the service in a "started" state even if killed for memory
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		//Kill the AsyncTask
		if(loader != null) {
			loader.plzStop();
		}

		Log.i(MainActivity.DEBUG_TAG, "Bluetooth proximity service stopped.");

		iAmRunning = false;

		//The service has finished, so let's enable the BTFragment's button again
		enableFragmentButton();
	}

	/**
	 * Notifies the fragment that it can enable its button again.
	 */
	private void enableFragmentButton() {
		sendLocalBroadcast(getApplicationContext(), BT_ENABLE_BUTTON_ACTION, null);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	public static boolean isServiceRunning() {
		return iAmRunning;
	}

	/**
	 * Sends the specified String using the LocalBroadcastManager interface.
	 * Retrieve the message using intent.getStringExtra("message").
	 *
	 * @param context The application context.
	 * @param action The action to specify with the Intent.
	 * @param message The message to send.
	 */
	protected static void sendLocalBroadcast(Context context, String action, String message) {
		LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
		Intent i = new Intent(action);
		if(message != null) {
			i.putExtra("message", message);
		}

		broadcastManager.sendBroadcast(i);

		Log.d(MainActivity.DEBUG_TAG, "Sent local broadcast with action: " + action);
	}

	/**
	 * Sends the specified int using the LocalBroadcastManager interface.
	 * Retrieve the message using intent.getIntExtra("message").
	 *
	 * @param context The application context.
	 * @param action The action to specify with the Intent.
	 * @param message The message to send.
	 */
	protected static void sendLocalBroadcast(Context context, String action, int message) {
		LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(context);
		Intent i = new Intent(action);
		i.putExtra("message", message);
		broadcastManager.sendBroadcast(i);

		Log.d(MainActivity.DEBUG_TAG, "Sent local broadcast with action: " + action);
	}

	/**
	 * Implementation of AsyncTask which periodically refreshes signal strength.
	 */
	class SignalStrengthLoader extends AsyncTask<Void, Integer, Void> {
		private BluetoothManager bluetoothManager;
		private DeviceLockManager deviceLockManager;
		private int signalStrength;
		private boolean plzStop;    //Calling asynctask.cancel(true) prevents onPostExecute from running.

		public SignalStrengthLoader() {
			bluetoothManager = new BluetoothManager(getApplicationContext());
			deviceLockManager = new DeviceLockManager(getApplicationContext());
			plzStop = false;
		}

		void plzStop() {
			plzStop = true;
		}

		@Override
		protected Void doInBackground(Void... voids) {
			while(!plzStop) {
				//Read remote RSSI. If we have a request for it out already, don't request again.
				if(bluetoothManager.canReadRssi()) {
					bluetoothManager.getBtGatt().readRemoteRssi();
				}

				//Get signal strength from the BTManager
				signalStrength = bluetoothManager.getSignalStrength();

				//Post the new value as "progress"
				publishProgress(signalStrength);

				Log.d(MainActivity.DEBUG_TAG, "Read signal strength: " + signalStrength);
				Log.d(MainActivity.DEBUG_TAG, "Using device: " + bluetoothManager.getPairedDevice().getName());
				Log.d(MainActivity.DEBUG_TAG, "\twith address: " + bluetoothManager.getPairedDevice().getAddress());
				Log.d(MainActivity.DEBUG_TAG, "Refresh interval: " + refreshIntervalMs);

				//Decide whether the device should be locked/unlocked
				deviceLockManager.handleDeviceLock(signalStrength);

				//Sleep this thread for the provided time
				try {
					Thread.sleep(refreshIntervalMs);
				} catch(InterruptedException e) {
					Log.w(MainActivity.DEBUG_TAG, "Signal strength thread interrupted. " +
							"System probably killed the service.");
				}
			}
			return null;
		}

		/**
		 * Signal strength represented in dBm.
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			int updatedSignalStrength = values[0];

			sendLocalBroadcast(getApplicationContext(), BT_SIGNAL_STRENGTH_ACTION, updatedSignalStrength);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			cancel(true);
		}
	}
}
