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
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
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

import com.javadog.bluetoothproximitylock.helpers.BetterSwitch;
import com.javadog.bluetoothproximitylock.helpers.BluetoothManager;
import com.javadog.bluetoothproximitylock.helpers.DeviceLockManager;

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

	protected static LocalBroadcastReceiver ssReceiver;
	protected static BetterSwitch serviceToggle;
	protected static TextView signalStrengthView;
	protected static Spinner deviceChooser;
	protected Set<BluetoothDevice> devicesSet;
	protected static Spinner lockDistance;
	protected static Spinner refreshIntervalSpinner;
	protected static long refreshInterval;    //TODO: Pretty sure a 5-second interval isn't practical. Reconsider this...
	protected boolean serviceBound;
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
		bindToService();
	}

	/**
	 * Initializes object references and performs some other set-up tasks.
	 */
	private void initialize() {
		//Get a reference to the user preferences editor
		userPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

		//Get fresh references to our views
		serviceToggle = new BetterSwitch(getActivity(),
				(Switch) getView().findViewById(R.id.button_bt_service_start_stop));
		signalStrengthView = (TextView) getView().findViewById(R.id.bt_signal_strength);
		deviceChooser = (Spinner) getView().findViewById(R.id.bt_device_chooser);
		lockDistance = (Spinner) getView().findViewById(R.id.bt_lock_distances); //TODO: This doesn't actually do anything yet.
		refreshIntervalSpinner = (Spinner) getView().findViewById(R.id.bt_refresh_interval);

		//Get a reference to the local broadcast manager, and specify which intent actions we want to listen for
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		IntentFilter filter = new IntentFilter();
		filter.addAction(SignalReaderService.ACTION_SIGNAL_STRENGTH_UPDATE);
		filter.addAction(SignalReaderService.ACTION_UNBIND_SERVICE);

		//Instantiate the ssReceiver if it's not already, then register it with the broadcast manager
		if(ssReceiver == null) {
			ssReceiver = new LocalBroadcastReceiver();
		}
		manager.registerReceiver(ssReceiver, filter);

		//Check whether device admin privileges are active, and show a dialog if not
		DevicePolicyManager dpm = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
		if(!dpm.isAdminActive(new ComponentName(getActivity().getApplicationContext(), DeviceLockManager.class))) {
			AdminDialogFragment adminDialogFragment = new AdminDialogFragment();
			adminDialogFragment.setCancelable(false);
			adminDialogFragment.show(getFragmentManager(), "needsAdmin");
		}

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

	/**
	 * Binds to the service if it's running. If it's not, then we'll still get a callback when it's started.
	 */
	private void bindToService() {
		Intent bindService = new Intent(getActivity().getApplicationContext(), SignalReaderService.class);

		//Passing 0 as the flag will prevent the service from being started if it hasn't been already.
		getActivity().bindService(bindService, serviceConnection, 0);
	}

	/**
	 * Unbinds from the service.
	 */
	public void unbindFromService() {
		if(serviceBound) {
			getActivity().unbindService(serviceConnection);
			serviceBound = false;
		}
	}

	/**
	 * Attempts to bind to the SignalReaderService, if it's running. Also handles disconnect.
	 */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			serviceBound = true;
			updateBtServiceUI();
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			//This should never be called because our service resides in the same process.
		}
	};

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
					BluetoothManager.setSelectedDevice(devices[i]);
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

		//Unregister the BluetoothStateReceiver
		getActivity().unregisterReceiver(btStateReceiver);

		unbindFromService();
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

	/**
	 * Enables/disables the bluetooth service.
	 *
	 * @param on On?
	 */
	private void toggleBtService(boolean on) {
		if(on) {
			startBtService();
		} else {
			stopBtService();
		}
	}

	protected void stopBtService() {
		getActivity().getApplicationContext().
				stopService(new Intent(getActivity().getApplicationContext(), SignalReaderService.class));
	}

	/**
	 * Handles starting of the service if all necessary conditions are met.
	 */
	protected void startBtService() {
		if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			//Disable the buttons so the user can't spam them, causing crashes. They get re-enabled when binding.
//			disableUiElement(
//					serviceToggle,
//					deviceChooser,
//					lockDistance,
//					refreshIntervalSpinner);

			Intent startIntent = new Intent(getActivity().getApplicationContext(), SignalReaderService.class);
			getActivity().getApplicationContext().startService(startIntent);
		} else {
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, REQUEST_CODE_ENABLE_BT);
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
		serviceToggle.silentlySetChecked(serviceBound);
	}

	/**
	 * Returns the millisecond value corresponding to the value selected in the refresh interval spinner.
	 *
	 * @param position The position of the selected item in the spinner.
	 * @return The {@link java.lang.Long} millisecond value the passed position represents.
	 */
	public static long interpretRefreshSpinner(int position) {
		return new long[]{1000, 2000, 5000}[position];
	}

	@Override
	public void onItemSelected(AdapterView<?> adapterView, View view, int position, long rowId) {
		SharedPreferences.Editor prefsEditor = userPrefs.edit();
		switch(adapterView.getId()) {
			case R.id.bt_device_chooser:
				//Feed the selected device's address to BluetoothManager
				Set<BluetoothDevice> devicesSet = BluetoothManager.getAllBtDevices();
				BluetoothDevice chosenDevice = (BluetoothDevice)devicesSet.toArray()[position];
				BluetoothManager.setSelectedDevice(chosenDevice);

				//Update saved preference
				prefsEditor.putString(BluetoothFragment.PREF_BT_DEVICE_ADDRESS, chosenDevice.getAddress());
				break;

			case R.id.bt_lock_distances:
				//Update saved preference
				prefsEditor.putInt(PREF_LOCK_DISTANCE, lockDistance.getSelectedItemPosition());
				break;

			case R.id.bt_refresh_interval:
				//Save the spinner value into our instance variable here
				refreshInterval = interpretRefreshSpinner(refreshIntervalSpinner.getSelectedItemPosition());

				//Update saved preference
				prefsEditor.putInt(PREF_REFRESH_INTERVAL, refreshIntervalSpinner.getSelectedItemPosition());
				break;
		}
		prefsEditor.apply();

		//Call startService again (if the service is running) in order to update its settings based on new prefs.
		if(serviceBound) {
			startBtService();
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
				toggleBtService(isChecked);
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
	 * Used in the main bluetooth fragment to receive signal strength from SignalReaderService.
	 */
	public class LocalBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch(intent.getAction()) {
				case SignalReaderService.ACTION_SIGNAL_STRENGTH_UPDATE:
					//If the broadcast was fired with this action, we know to update the UI
					Log.d(MainActivity.DEBUG_TAG, "Local BT signal broadcast received.");

					//Update signal strength
					int newSignalStrength = intent.getIntExtra("message", Integer.MIN_VALUE);
					updateSignalStrength(newSignalStrength);
					break;

				case SignalReaderService.ACTION_UNBIND_SERVICE:
					//We need to unbind from the service so it can shut down
					Log.d(MainActivity.DEBUG_TAG, "Unbind broadcast received.");
					unbindFromService();
					break;
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
}
