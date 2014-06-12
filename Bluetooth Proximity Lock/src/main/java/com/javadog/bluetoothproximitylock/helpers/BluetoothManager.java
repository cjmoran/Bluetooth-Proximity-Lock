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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.javadog.bluetoothproximitylock.MainActivity;

import java.util.Set;

/**
 * A class used to read the signal strength of a connected BT device
 * and perform some other related tasks.
 */
public class BluetoothManager extends BluetoothGattCallback {
	private int signalStrength;
	private static boolean rssiRequested;
	private static BluetoothGatt btGatt;

	private static BluetoothDevice selectedDevice;

	//Keep track of all BT devices locally
	private static Set<BluetoothDevice> allBtDevices;

	/**
	 * @param context Application context.
	 */
	public BluetoothManager(final Context context) {
		//Refresh all bluetooth devices
		refreshBtDevices();

		rssiRequested = false;

		final BluetoothDevice device = getPairedDevice();

		//Samsung devices require connectGatt to be run on the UI thread...
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				btGatt = device.connectGatt(context, false, BluetoothManager.this);
			}
		});
	}

	/**
	 * This method simply returns the first bluetooth device the system returns, unless "selectedDevice" is set.
	 *
	 * If selectedDevice is not set, it will set it to the system-selected device.
	 * To request a particular device by BT address, use:
	 * {@link BluetoothManager#getPairedDevice(String)}.
	 *
	 * @return The first bluetooth device the system feels like returning, or the selected device (if set).
	 */
	public static BluetoothDevice getPairedDevice() {
		return selectedDevice == null ?
				selectedDevice = BluetoothAdapter.getDefaultAdapter().getBondedDevices().iterator().next() :
				selectedDevice;
	}

	/**
	 * Returns the specific, bonded BT device identified by the provided deviceAddress.
	 *
	 * @param deviceAddress Which device I should return.
	 * @return The requested device.
	 */
	public static BluetoothDevice getPairedDevice(String deviceAddress) throws Resources.NotFoundException {
		for(BluetoothDevice device : allBtDevices) {
			if(device.getAddress().equalsIgnoreCase(deviceAddress)) {
				return device;
			}
		}

		throw new Resources.NotFoundException();
	}

	@Override
	public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
		super.onReadRemoteRssi(gatt, rssi, status);

		if(status == BluetoothGatt.GATT_SUCCESS) {
			signalStrength = rssi;
			Log.d(MainActivity.DEBUG_TAG, "Got RSSI value of " + rssi);
		} else {
			Log.w(MainActivity.DEBUG_TAG, "Error getting RSSI value.");
		}

		rssiRequested = false;
	}

	/**
	 * If new services have been discovered, we should update the BT devices list
	 */
	@Override
	public void onServicesDiscovered(BluetoothGatt gatt, int status) {
		super.onServicesDiscovered(gatt, status);

		Log.v(MainActivity.DEBUG_TAG, "BTManager discovered new services; refreshing BT device list.");
		refreshBtDevices();
	}

	public static void refreshBtDevices() {
		allBtDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
	}

	public static void setSelectedDevice(BluetoothDevice selectedDevice) {
		BluetoothManager.selectedDevice = selectedDevice;
	}

	public int getSignalStrength() {
		return signalStrength;
	}

	public static Set<BluetoothDevice> getAllBtDevices() {
		return allBtDevices;
	}

	public BluetoothGatt getBtGatt() {
		return btGatt;
	}

	public boolean canReadRssi() {
		return !rssiRequested;
	}
}
