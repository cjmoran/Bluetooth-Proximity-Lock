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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.javadog.bluetoothproximitylock.helpers.BluetoothManager;
import com.javadog.bluetoothproximitylock.helpers.DeviceLockManager;
import com.javadog.bluetoothproximitylock.helpers.ServiceBinder;

/**
 * This Service runs a background thread which periodically updates the signal strength.
 */
public class SignalReaderService extends Service {
	public final static String ACTION_SIGNAL_STRENGTH_UPDATE = "com.javadog.bluetoothproximitylock.UPDATE_BT_SS";
	public final static String ACTION_UNBIND_SERVICE = "com.javadog.bluetoothproximitylock.UNBIND_PLZ";

	private final IBinder binder = new ServiceBinder<>(this);
	private static long refreshIntervalMs;
	private static boolean iAmRunning;

	private SignalStrengthLoader loader;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);

		//Refresh interval should be specified in Preferences
		SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		refreshIntervalMs = BluetoothFragment.interpretRefreshSpinner(
				userPrefs.getInt(BluetoothFragment.PREF_REFRESH_INTERVAL, 1));

		//onStartCommand can be run multiple times by calls to startService
		if(!isServiceRunning()) {
			Log.i(MainActivity.DEBUG_TAG, "SignalReaderService started.");
		}

		iAmRunning = true;

		loader = new SignalStrengthLoader();
		loader.execute();

		//Keep the service in a "started" state even if killed for memory
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		tearDown();

		Log.i(MainActivity.DEBUG_TAG, "SignalReaderService stopped.");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public static boolean isServiceRunning() {
		return iAmRunning;
	}

	private void tearDown() {
		//Kill the AsyncTask
		if(loader != null) {
			loader.plzStop();
		}

		//Tell the BTFragment to unbind this service so it can be destroyed
		sendLocalBroadcast(getApplicationContext(), ACTION_UNBIND_SERVICE, 1);

		iAmRunning = false;

		Toast.makeText(getApplicationContext(), "Bluetooth auto-lock disabled", Toast.LENGTH_LONG).show();
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
		protected void onPreExecute() {
			Log.d(MainActivity.DEBUG_TAG, "BT signal strength loader started");
		}

		@Override
		protected Void doInBackground(Void... voids) {
			while(!plzStop) {
				//Read remote RSSI. If we have a request for it out already, don't request again.
				//The btGatt is null checked because it's instantiated on the UI thread. Thanks Samsung.
				if(bluetoothManager.getBtGatt() != null && bluetoothManager.canReadRssi()) {
					bluetoothManager.getBtGatt().readRemoteRssi();

					//Get signal strength from the BTManager
					signalStrength = bluetoothManager.getSignalStrength();

					//Post the new value as "progress"
					publishProgress(signalStrength);

					Log.d(MainActivity.DEBUG_TAG, "Read signal strength: " + signalStrength);
					Log.d(MainActivity.DEBUG_TAG, "Using device: " + BluetoothManager.getPairedDevice().getName());
					Log.d(MainActivity.DEBUG_TAG, "\twith address: " + BluetoothManager.getPairedDevice().getAddress());
					Log.d(MainActivity.DEBUG_TAG, "Refresh interval: " + refreshIntervalMs);

					//Decide whether the device should be locked/unlocked
					deviceLockManager.handleDeviceLock(signalStrength);
				}

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
			sendLocalBroadcast(getApplicationContext(), ACTION_SIGNAL_STRENGTH_UPDATE, updatedSignalStrength);
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			Log.d(MainActivity.DEBUG_TAG, "BT signal strength loader stopped");
		}
	}
}
