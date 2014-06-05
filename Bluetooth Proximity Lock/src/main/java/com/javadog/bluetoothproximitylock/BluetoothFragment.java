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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
		CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {
	public final static String PREF_BT_DEVICE_ADDRESS = "btDeviceAddress";
	public final static String PREF_LOCK_DISTANCE = "lockDistance";
	public final static String PREF_REFRESH_INTERVAL = "refreshInterval";
	final static int REQUEST_CODE_ENABLE_ADMIN = 984;
	final static int REQUEST_CODE_ENABLE_BT = 873;

	protected SignalStrengthUpdateReceiver ssReceiver;
	protected static Switch serviceToggle;
	protected static TextView signalStrengthView;
	protected static Spinner deviceChooser;
	protected Set<BluetoothDevice> devicesSet;
	protected static Spinner lockDistance;
	protected static Spinner refreshIntervalSpinner;
	protected static long refreshInterval;    //TODO: Pretty sure a 5-second interval isn't practical. Reconsider this...
	protected boolean serviceRunning;
	protected static SharedPreferences userPrefs;
	protected BluetoothStateReceiver btStateReceiver;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bt_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		initialize();
		loadUserPreferences();
		setupClickListeners();
	}

	/**
	 * Initializes object references and performs some other set-up tasks.
	 */
	private void initialize() {
		//Get a reference to the user preferences editor
		userPrefs = getActivity().getPreferences(Context.MODE_PRIVATE);

		//Get a fresh reference to our views
		serviceToggle = (Switch) getView().findViewById(R.id.button_bt_service_start_stop);
		signalStrengthView = (TextView) getView().findViewById(R.id.bt_signal_strength);
		lockDistance = (Spinner) getView().findViewById(R.id.bt_lock_distances); //TODO: This doesn't actually do anything yet.
		refreshIntervalSpinner = (Spinner) getView().findViewById(R.id.bt_refresh_interval);

		//Get a reference to the local broadcast manager, and specify which intent actions we want to listen for
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		IntentFilter filter = new IntentFilter();
		filter.addAction(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION);
		filter.addAction(SignalReaderService.BT_ENABLE_BUTTON_ACTION);

		//Instantiate the ssReceiver if it's not already, then register it with the broadcast manager
		if(ssReceiver == null) {
			ssReceiver = new SignalStrengthUpdateReceiver();
		}
		manager.registerReceiver(ssReceiver, filter);

		//Check if the service is running, update our button depending
		serviceRunning = SignalReaderService.isServiceRunning();
		updateBtServiceUI();

		//Check whether device admin privileges are active, and show a dialog if not
		DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
		if(!dpm.isAdminActive(new ComponentName(getActivity().getApplicationContext(), DeviceLockManager.class))) {
			AdminDialogFragment adminDialogFragment = new AdminDialogFragment();
			adminDialogFragment.setCancelable(false);
			adminDialogFragment.show(getFragmentManager(), "needsAdmin");
		}

		deviceChooser = (Spinner) getView().findViewById(R.id.bt_device_chooser);

		populateBtDevices();

		//Start the device chooser in a disabled state if Bluetooth is disabled
		if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			deviceChooser.setEnabled(true);
		} else {
			deviceChooser.setEnabled(false);
		}

		//Register a listener with the system to get updates about changes to Bluetooth state
		if(btStateReceiver == null) {
			btStateReceiver = new BluetoothStateReceiver();
		}
		IntentFilter btFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		getActivity().registerReceiver(btStateReceiver, btFilter);
	}

	protected void populateBtDevices() {
		//Get a Set of all paired bluetooth devices, convert to array
		BluetoothManager.refreshBtDevices();
		devicesSet = BluetoothManager.getAllBtDevices();
		ArrayList<String> devices = new ArrayList<>();
		for(BluetoothDevice b : devicesSet) {
			devices.add(b.getName() + " (" + b.getAddress() + ")");
		}

		//Set the adapter for the device spinner
		deviceChooser.setAdapter(new ArrayAdapter<>(
				getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, devices));
	}

	/**
	 * Gets previously-set user prefs from Android and sets UI elements appropriately.
	 */
	private void loadUserPreferences() {
		//Retrieve chosen device by bluetooth address
		BluetoothDevice[] devices = devicesSet.toArray(new BluetoothDevice[devicesSet.size()]);
		if(!userPrefs.getString(PREF_BT_DEVICE_ADDRESS, "abc").equals("none")) {
			for(int i = 0; i < devices.length; i++) {
				if(devices[i].getAddress().equals(userPrefs.getString(PREF_BT_DEVICE_ADDRESS, "none"))) {
					deviceChooser.setSelection(i);
					break;
				}
			}
		}
		lockDistance.setSelection(userPrefs.getInt(PREF_LOCK_DISTANCE, 1));
		refreshIntervalSpinner.setSelection(userPrefs.getInt(PREF_REFRESH_INTERVAL, 1));

		//Update internal copy of refresh interval based on the value in the spinner
		refreshInterval = interpretRefreshSpinner(refreshIntervalSpinner.getSelectedItemPosition());
	}

	@Override
	public void onPause() {
		super.onPause();

		//Unregister our LocalBroadcastReceiver
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		manager.unregisterReceiver(ssReceiver);

		//Save user preferences
		SharedPreferences.Editor editor = userPrefs.edit();

		//Save chosen device by bluetooth address; order might change
		int selectedItem = deviceChooser.getSelectedItemPosition();
		if(selectedItem != Spinner.INVALID_POSITION) {
			editor.putString(PREF_BT_DEVICE_ADDRESS,
							devicesSet.toArray(new BluetoothDevice[devicesSet.size()])[selectedItem].getAddress());
		}
		editor.putInt(PREF_LOCK_DISTANCE, lockDistance.getSelectedItemPosition());
		editor.putInt(PREF_REFRESH_INTERVAL, refreshIntervalSpinner.getSelectedItemPosition());

		editor.apply();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		//Unregister the BluetoothStateReceiver
		getActivity().unregisterReceiver(btStateReceiver);
	}

	private void setupClickListeners() {
		//Service toggle switch
		serviceToggle.setOnCheckedChangeListener(this);

		Spinner[] spinners = {deviceChooser, lockDistance, refreshIntervalSpinner};
		for(Spinner spinner : spinners) {
			spinner.setOnItemSelectedListener(this);
		}
	}

	/**
	 * Disables the view(s) with the specified ID(s).
	 *
	 * @param views The views to be disabled.
	 */
	protected void disableUiElement(View... views) {
		for(View v : views) {
			v.setEnabled(false);
		}
	}

	/**
	 * Enables the view(s) with the specified ID(s).
	 *
	 * @param views The views to be enabled.
	 */
	protected void enableUiElement(View... views) {
		for(View v : views) {
			v.setEnabled(true);
		}
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
		disableUiElement(
				serviceToggle,
				deviceChooser,
				refreshIntervalSpinner);

		getActivity().stopService(new Intent(getActivity().getApplicationContext(), SignalReaderService.class));

		serviceRunning = false;
		updateBtServiceUI();
	}

	/**
	 * Handles starting of the service if all necessary conditions are met.
	 */
	protected void startBtService() {
		if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			//Disable the buttons so the user can't spam them, causing crashes. They get re-enabled by a broadcast.
			disableUiElement(
					serviceToggle,
					deviceChooser,
					refreshIntervalSpinner);

			Intent startIntent = new Intent(getActivity().getApplicationContext(), SignalReaderService.class);

			//Be sure to pass the device address and refresh interval with the intent
			startIntent.putExtra("btDeviceAddress", BluetoothManager.getSelectedDevice().getAddress());
			startIntent.putExtra("btRefreshInterval", refreshInterval);

			getActivity().startService(startIntent);

			serviceRunning = true;
			updateBtServiceUI();
		} else {
			new BluetoothDialogFragment().show(getFragmentManager(), "needsBluetooth");
			serviceToggle.setChecked(false);
		}
	}

	/**
	 * Updates the signal strength listed on the BluetoothFragment.
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

			case R.id.bt_lock_distances:

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
	 * Used to receive updates about Bluetooth state (enabled/disabled/etc).
	 */
	private class BluetoothStateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				switch(state) {
					case BluetoothAdapter.STATE_ON:
						enableUiElement(deviceChooser);
						Log.d(MainActivity.DEBUG_TAG, "Received broadcast: Bluetooth enabled");
						break;

					case BluetoothAdapter.STATE_OFF:
						disableUiElement(deviceChooser);
						Log.d(MainActivity.DEBUG_TAG, "Received broadcast: Bluetooth disabled");
						break;
				}
			}
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

			//When these broadcast are received, we want to enable the UI
			else if(action.equals(SignalReaderService.BT_ENABLE_BUTTON_ACTION)) {
				Log.d(MainActivity.DEBUG_TAG, "Received BT_ENABLE_BUTTON intent.");
				enableUiElement(
						serviceToggle,
						deviceChooser,
						refreshIntervalSpinner);
			}
		}
	}

	/**
	 * Used for alerting the user that they'll be redirected to Settings to enable device admin.
	 */
	public static class AdminDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.device_admin_dialog_title))
					.setMessage(getResources().getString(R.string.device_admin_dialog_text))
					.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							Intent activateAdminIntent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
							activateAdminIntent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
									new ComponentName(getActivity().getApplicationContext(), DeviceLockManager.class));
							activateAdminIntent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
									getResources().getString(R.string.device_admin_description));
							startActivityForResult(activateAdminIntent, REQUEST_CODE_ENABLE_ADMIN);
						}
					})
					.setNegativeButton(getResources().getString(R.string.nope), new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							getActivity().finish();
						}
					});
			return builder.create();
		}
	}

	/**
	 * Used to prompt the user to enable Bluetooth if they try to start the service without it.
	 */
	public static class BluetoothDialogFragment extends DialogFragment {
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getResources().getString(R.string.enable_bt_dialog_title))
					.setMessage(getResources().getString(R.string.enable_bt_dialog_text))
					.setPositiveButton(getResources().getString(R.string.bt_settings),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
							startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);
						}
					})
					.setNegativeButton(getResources().getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialogInterface, int i) {
							dialogInterface.cancel();
						}
					});
			return builder.create();
		}
	}
}
