package com.javadog.bluetoothproximitylock;

import android.app.ActivityManager;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A page for configuring Bluetooth options.
 */
public class BluetoothFragment extends Fragment implements View.OnClickListener {
	private SignalStrengthUpdateReceiver ssReceiver;
	private static TextView signalStrengthView;
	private boolean serviceRunning;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.bt_fragment, container, false);
	}

	@Override
	public void onResume() {
		super.onResume();

		setupClickListeners();

		//Get a fresh reference to the signal strength view
		signalStrengthView = (TextView) getView().findViewById(R.id.bt_signal_strength);

		//Check if the service is running, update our button depending
		serviceRunning = SignalReaderService.isServiceRunning();
		updateBtServiceUI(getView());

		//Instantiate the ssReceiver if it's not already, then register it
		if(ssReceiver == null) {
			ssReceiver = new SignalStrengthUpdateReceiver();
		}

		//Register this BroadcastReceiver with the system
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		IntentFilter filter = new IntentFilter(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION);
		manager.registerReceiver(ssReceiver, filter);
	}

	private void setupClickListeners() {
		Button serviceToggle = (Button) getView().findViewById(R.id.button_bt_service_start_stop);
		serviceToggle.setOnClickListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();

		//Unregister our BroadcastReceiver
		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getActivity().getApplicationContext());
		manager.unregisterReceiver(ssReceiver);
	}

	@Override
	public void onClick(View view) {
		switch(view.getId()) {
			case R.id.button_bt_service_start_stop:
				//Toggle the service
				toggleBtService();
				break;
		}
	}

	private void toggleBtService() {
		if(serviceRunning) {
			getActivity().stopService(new Intent(getActivity().getApplicationContext(), SignalReaderService.class));
		} else {
			getActivity().startService(new Intent(getActivity().getApplicationContext(), SignalReaderService.class));
		}

		serviceRunning = !serviceRunning;
		updateBtServiceUI(getView());
	}

	protected void updateSignalStrength(int newSignalStrength) {
		if(signalStrengthView != null) {
			signalStrengthView.setText(String.valueOf(newSignalStrength));
		}
	}

	private void updateBtServiceUI(View view) {
		//Update the button
		Button toggleButton = (Button) view.findViewById(R.id.button_bt_service_start_stop);
		toggleButton.setText(serviceRunning ? R.string.button_stop_service : R.string.button_start_service);

		//Update device name
		TextView deviceName = (TextView) view.findViewById(R.id.bt_device_name);
		BluetoothDevice pairedDevice = BluetoothManager.getPairedDevice(getActivity().getApplicationContext());
		String name = pairedDevice.getName();
		deviceName.setText(name == null ? "Error reading name." : name);

		//Update the status text
		TextView statusText = (TextView) view.findViewById(R.id.bt_service_status);
		statusText.setText(serviceRunning ? R.string.running : R.string.stopped);
		statusText.setTextColor(serviceRunning ? Color.GREEN : Color.RED);
	}

	/**
	 * Used in the main bluetooth fragment to receive on signal strength from SignalReaderService.
	 */
	private class SignalStrengthUpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			//If the broadcast was fired with this action, we know to update the UI with a new signal strength
			if(intent.getAction().equals(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION)) {
				Log.d(MainActivity.DEBUG_TAG, "Local BT signal broadcast received.");

				int newSignalStrength = intent.getIntExtra("BTSignalStrength", Short.MIN_VALUE);
				updateSignalStrength(newSignalStrength);
			}
		}
	}
}
