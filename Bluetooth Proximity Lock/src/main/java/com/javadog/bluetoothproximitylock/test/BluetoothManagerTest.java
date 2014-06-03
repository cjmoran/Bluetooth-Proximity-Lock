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

package com.javadog.bluetoothproximitylock.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import com.javadog.bluetoothproximitylock.BluetoothManager;

import junit.framework.TestCase;

import java.util.Set;

/**
 * Unit tests for {@link com.javadog.bluetoothproximitylock.BluetoothManager}
 */
public class BluetoothManagerTest extends TestCase {
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothManager#getPairedDevice()} and
	 * {@link com.javadog.bluetoothproximitylock.BluetoothManager#getPairedDevice(String)}.
	 */
	public void testGetPairedDevice() {
		Set<BluetoothDevice> bondedDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();

		//If this condition is false, Bluetooth is disabled or no devices are paired. That is checked elsewhere.
		if(!bondedDevices.isEmpty()) {
			BluetoothDevice testDevice = bondedDevices.iterator().next();

			BluetoothManager.setSelectedDevice(testDevice);
			assertEquals("BTManager should return the selected device",
					testDevice.getAddress(),
					BluetoothManager.getPairedDevice().getAddress());
			assertEquals("BTManager should return the selected device (selected by address)",
					testDevice.getAddress(),
					BluetoothManager.getPairedDevice(testDevice.getAddress()).getAddress());
		}
	}
}
