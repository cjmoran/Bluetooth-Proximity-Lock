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
