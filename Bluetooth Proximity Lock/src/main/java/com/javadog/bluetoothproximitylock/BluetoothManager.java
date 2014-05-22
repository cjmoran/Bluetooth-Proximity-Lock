package com.javadog.bluetoothproximitylock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Iterator;

/**
 * A class used to read the signal strength of a connected BT device
 * and perform some other related tasks.
 */
public class BluetoothManager extends BluetoothGattCallback {
	private int signalStrength;
	private boolean rssiRequested;
	private BluetoothGatt btGatt;

	public BluetoothManager(Context context) {
		rssiRequested = false;
		BluetoothDevice device = getPairedDevice(context);
		btGatt = device.connectGatt(context, false, this);
	}

	public int getSignalStrength() {
		return signalStrength;
	}

	public BluetoothGatt getBtGatt() {
		return btGatt;
	}

	public boolean canReadRssi() {
		return !rssiRequested;
	}

	protected static BluetoothDevice getPairedDevice(Context context) {   //TODO: allow user to choose device
		//Get a Set of paired Bluetooth devices from the system Bluetooth service
		Iterator<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices().iterator();

		bondedDevices.next();
		return bondedDevices.next();
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
}
