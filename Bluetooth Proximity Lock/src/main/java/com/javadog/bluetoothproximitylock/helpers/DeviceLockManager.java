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

package com.javadog.bluetoothproximitylock.helpers;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.javadog.bluetoothproximitylock.MainActivity;
import com.javadog.bluetoothproximitylock.helpers.CircularQueue;

/**
 * Handles locking/unlocking of the device when conditions are met.
 */
public class DeviceLockManager extends DeviceAdminReceiver {
	//TODO: temporary constants that work for my devices. Add a calibration screen.
	private final static int CLOSE_PROXIMITY = 0;
	private final static int MEDIUM_PROXIMITY = -4;
	private final static int FAR_PROXIMITY = -10;

	private static CircularQueue<Integer> fiveDistanceSamples = new CircularQueue<>(5);

	private boolean lockEnabled;

	private DevicePolicyManager dpm;

	@SuppressWarnings("unused") //This is called by the system
	public DeviceLockManager() {
		super();
	}

	public DeviceLockManager(Context context) {
		dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

		//Assume the device is locked already (surprisingly difficult to determine)
		lockEnabled = true;
	}

	/**
	 * Determines whether the device needs to be locked/unlocked, then locks/unlocks it accordingly.
	 *
	 * @param signalStrength The current signal strength as measured by SignalReaderService.
	 */
	public void handleDeviceLock(int signalStrength) {
		//TODO: Allow the user to specify distance tolerance. For now I will assume "medium" distance = lock.

		//Add the latest sample to our queue of 5 samples
		fiveDistanceSamples.add(signalStrength);

		//Only load resources to alter device lock if necessary
		boolean tempLockEnabled = fiveDistanceSamples.getAverageOfElements() < MEDIUM_PROXIMITY;

		//TODO: Allow user to toggle auto-lock even when screen is on.
		if(tempLockEnabled != lockEnabled) {
			lockEnabled = tempLockEnabled;

			String newPassword = lockEnabled ? "1234" : "";

			boolean lockSuccess = dpm.resetPassword(newPassword, 0);

			Log.d(MainActivity.DEBUG_TAG, "Device lock enabled set to: " + lockEnabled);
			Log.d(MainActivity.DEBUG_TAG, "Lock change successful: " + lockSuccess);
		}
	}

	/**
	 * When device administrator privileges have been enabled by the user.
	 */
	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
	}
}
