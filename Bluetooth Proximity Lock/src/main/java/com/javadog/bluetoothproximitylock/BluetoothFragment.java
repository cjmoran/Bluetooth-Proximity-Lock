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

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Set;

/**
 * A page for configuring Bluetooth options.
 */
public class BluetoothFragment extends Fragment implements
		CompoundButton.OnCheckedChangeListener,AdapterView.OnItemSelectedListener {
	protected SignalStrengthUpdateReceiver ssReceiver;
	protected static Switch serviceToggle;
	protected static TextView signalStrengthView;
	protected static Spinner refreshIntervalSpinner;
	protected static long refreshInterval;    //TODO: Pretty sure a 5-second interval isn't practical. Reconsider this...
	protected boolean serviceRunning;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bt_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		populateBtDevices();

		//Get a fresh reference to our views
		serviceToggle = (Switch) getView().findViewById(R.id.button_bt_service_start_stop);
		signalStrengthView = (TextView) getView().findViewById(R.id.bt_signal_strength);
		refreshIntervalSpinner = (Spinner) getView().findViewById(R.id.bt_refresh_interval);

		setupClickListeners();

		//Update refresh interval based on the value in the spinner
		refreshInterval = interpretRefreshSpinner(refreshIntervalSpinner.getSelectedItemPosition());

		//Check if the service is running, update our button depending
		serviceRunning = SignalReaderService.isServiceRunning();
		updateBtServiceUI();

		//Get a reference to the local broadcast manager, and specify which intent actions we want to listen for
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		IntentFilter filter = new IntentFilter();
		filter.addAction(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION);
		filter.addAction(SignalReaderService.BT_ENABLE_BUTTON_ACTION);

		//Instantiate the ssReceiver if it's not already, then register it with the LBM
		if(ssReceiver == null) {
			ssReceiver = new SignalStrengthUpdateReceiver();
		}
		manager.registerReceiver(ssReceiver, filter);
	}

	protected void populateBtDevices() {
		//Get a Set of all paired bluetooth devices, convert to array
		BluetoothManager.refreshBtDevices();
		Set<BluetoothDevice> devicesSet = BluetoothManager.getAllBtDevices();
		ArrayList<String> devices = new ArrayList<>();
		for(BluetoothDevice b : devicesSet) {
			devices.add(b.getName() + " (" + b.getAddress() + ")");
		}

		//Set the adapter for the device spinner
		Spinner deviceChooser = (Spinner) getView().findViewById(R.id.bt_device_chooser);
		deviceChooser.setAdapter(new ArrayAdapter<>(
				getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, devices));

		//Set the onItemSelectedListener to this fragment
		deviceChooser.setOnItemSelectedListener(this);
	}

	private void setupClickListeners() {
		//Service toggle switch
		serviceToggle.setOnCheckedChangeListener(this);

		//Refresh interval spinner
		Spinner refreshInts = (Spinner) getView().findViewById(R.id.bt_refresh_interval);
		refreshInts.setOnItemSelectedListener(this);
	}

	/**
	 * Disables the button(s) with the specified buttonId(s).
	 *
	 * @param buttonIds The buttons to be disabled.
	 */
	protected void disableButton(int... buttonIds) {
		for(int id : buttonIds) {
			getView().findViewById(id).setEnabled(false);
		}
	}

	/**
	 * Enables the button(s) with the specified buttonId(s).
	 *
	 * @param buttonIds The buttons to be enabled.
	 */
	protected void enableButton(int... buttonIds) {
		for(int id : buttonIds) {
			getView().findViewById(id).setEnabled(true);
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		//Unregister our BroadcastReceiver
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		manager.unregisterReceiver(ssReceiver);
	}

	private void toggleBtService() {
		if(serviceRunning) {
			stopBtService();
		} else {
			startBtService();
		}
	}

	protected void stopBtService() {
		//Disable the buttons so the user can't spam them, causing crashes. They get re-enabled by a broadcast.
		disableButton(
				R.id.button_bt_service_start_stop,
				R.id.bt_device_chooser,
				R.id.bt_refresh_interval);

		getActivity().stopService(new Intent(getActivity().getApplicationContext(), SignalReaderService.class));

		serviceRunning = false;
		updateBtServiceUI();
	}

	protected void startBtService() {
		//Disable the buttons so the user can't spam them, causing crashes. They get re-enabled by a broadcast.
		disableButton(
				R.id.button_bt_service_start_stop,
				R.id.bt_device_chooser,
				R.id.bt_refresh_interval);

		Intent startIntent = new Intent(getActivity().getApplicationContext(), SignalReaderService.class);

		//Be sure to pass the device address and refresh interval with the intent
		startIntent.putExtra("btDeviceAddress", BluetoothManager.getSelectedDevice().getAddress());
		startIntent.putExtra("btRefreshInterval", refreshInterval);

		getActivity().startService(startIntent);

		serviceRunning = true;
		updateBtServiceUI();
	}

	/**
	 * Updates the signal stre
	 *
	 * @param newSignalStrength The updated signal strength value.
	 */
	protected void updateSignalStrength(int newSignalStrength) {
		if(signalStrengthView != null && newSignalStrength != Integer.MIN_VALUE) {
			signalStrengthView.setText(String.valueOf(newSignalStrength));
		} else if(newSignalStrength == Integer.MIN_VALUE) {
			signalStrengthView.setText(getResources().getString(R.string.loading));
		}
	}

	private void updateBtServiceUI() {
		//Update the switch
		serviceToggle.setChecked(serviceRunning);
	}

	/**
	 * Returns the millisecond value corresponding to the value selected in the refresh interval spinner.
	 *
	 * @param position The position of the selected item in the spinner.
	 * @return The {@link java.lang.Long} millisecond value the passed position represents.
	 */
	private long interpretRefreshSpinner(int position) {
		return new long[]{1000, 2000, 5000}[position];
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId) {
		boolean wasRunning;
		switch(adapterView.getId()) {
			case R.id.bt_device_chooser:
				//First stop the service if it's running
				wasRunning = SignalReaderService.isServiceRunning();
				if(wasRunning) {
					stopBtService();
				}

				//Now feed the selected device's address to BluetoothManager
				Set<BluetoothDevice> devicesSet = BluetoothManager.getAllBtDevices();
				BluetoothDevice chosenDevice = (BluetoothDevice)devicesSet.toArray()[position];
				BluetoothManager.setSelectedDevice(chosenDevice);

				//And restart the service if it had been running
				if(wasRunning) {
					startBtService();
				}
				break;

			case R.id.bt_refresh_interval:
				//Stop the service if it was running
				wasRunning = SignalReaderService.isServiceRunning();
				if(wasRunning) {
					stopBtService();
				}

				//Now save the spinner value into our instance variable here, and restart the service
				refreshInterval = interpretRefreshSpinner(refreshIntervalSpinner.getSelectedItemPosition());

				//And restart the service if it had been running
				if(wasRunning) {
					startBtService();
				}
				break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> adapterView) {
		//Nothing
	}

	@Override
	public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
		switch(compoundButton.getId()) {
			case R.id.button_bt_service_start_stop:
				toggleBtService();
				break;
		}
	}

	/**
	 * Used in the main bluetooth fragment to receive on signal strength from SignalReaderService.
	 */
	public class SignalStrengthUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			//If the broadcast was fired with this action, we know to update the UI
			if(action.equals(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION)) {
				Log.d(MainActivity.DEBUG_TAG, "Local BT signal broadcast received.");

				//Update signal strength
				int newSignalStrength = intent.getIntExtra("message", Integer.MIN_VALUE);
				updateSignalStrength(newSignalStrength);
			}

			//When these broadcast are received, we want to enable the Start/Stop switch.
			else if(action.equals(SignalReaderService.BT_ENABLE_BUTTON_ACTION)) {
				Log.d(MainActivity.DEBUG_TAG, "Received BT_ENABLE_BUTTON intent.");
				enableButton(
						R.id.button_bt_service_start_stop,
						R.id.bt_device_chooser,
						R.id.bt_refresh_interval);
			}
		}
	}
}
