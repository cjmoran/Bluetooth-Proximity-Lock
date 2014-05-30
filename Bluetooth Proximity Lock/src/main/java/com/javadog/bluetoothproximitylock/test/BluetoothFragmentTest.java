package com.javadog.bluetoothproximitylock.test;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.javadog.bluetoothproximitylock.BluetoothFragment;
import com.javadog.bluetoothproximitylock.R;
import com.javadog.bluetoothproximitylock.SignalReaderService;

/**
 * Unit tests for BluetoothFragment.
 */
public class BluetoothFragmentTest extends ActivityInstrumentationTestCase2<PlaceholderFragmentActivity> {
	private PlaceholderFragmentActivity activity;

	public BluetoothFragmentTest() {
		super(PlaceholderFragmentActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		super.setUp();

		setActivityInitialTouchMode(true);

		//Get a reference to the activity
		activity = getActivity();
	}

	private Fragment addFragment(Fragment fragment) {
		//Add the BT Fragment to the placeholder activity
		FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
		transaction.add(R.id.placeholder_fragment_layout, fragment, "abc");
		transaction.commitAllowingStateLoss();
		getInstrumentation().waitForIdleSync();

		//Return a reference to the Fragment once it's inflated
		return activity.getFragmentManager().findFragmentByTag("abc");
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#onResume()}
	 */
	public void testOnResume() {
		//Anonymous class here lets us override methods for testing
		BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			public void onResume() {
				super.onResume();

				//Test that references aren't null
				assertNotNull("ssReceiver reference null check", ssReceiver);
				assertNotNull("serviceToggle reference null check", serviceToggle);
				assertNotNull("signalStrengthView reference null check", signalStrengthView);
				assertNotNull("refreshIntervalSpinner reference null check", refreshIntervalSpinner);

				//TODO: Ensure values set match saved user preferences
				// (code for this has yet to be written)
			}
		};

		addFragment(fragment);
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#populateBtDevices()}
	 */
	public void testPopulateBtDevices() {
		//TODO: Ensure bluetooth devices in spinner are actually paired and connected
		// (code for this has yet to be written)
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#disableButton(int...)}
	 */
	public void testDisableButton() {
		BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			protected void disableButton(int... buttonIds) {
				buttonIds = new int[] {
					R.id.button_bt_service_start_stop,
					R.id.bt_device_chooser
				};

				Switch startButton = (Switch) getView().findViewById(buttonIds[0]);
				Spinner deviceSpinner = (Spinner) getView().findViewById(buttonIds[1]);

				//Set them to enabled first
				startButton.setEnabled(true);
				deviceSpinner.setEnabled(true);

				super.disableButton(buttonIds);

				assertFalse("Two UI elements should be disabled",
						startButton.isEnabled() &&
						deviceSpinner.isEnabled());
			}
		};

		addFragment(fragment);
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#enableButton(int...)}
	 */
	public void testEnableButton() {
		BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			protected void enableButton(int... buttonIds) {
				buttonIds = new int[] {
						R.id.button_bt_service_start_stop,
						R.id.bt_device_chooser
				};

				Switch startButton = (Switch) getView().findViewById(buttonIds[0]);
				Spinner deviceSpinner = (Spinner) getView().findViewById(buttonIds[1]);

				//Set them to disabled first
				startButton.setEnabled(false);
				deviceSpinner.setEnabled(false);

				super.enableButton(buttonIds);

				assertTrue("Two UI elements should be enabled",
						startButton.isEnabled() &&
						deviceSpinner.isEnabled());
			}
		};

		addFragment(fragment);
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#stopBtService()}
	 *
	 * This will only test the state of the boolean serviceRunning and the state
	 * of the related UI elements. Can't get a reference to the service directly.
	 */
	public void testStopService() {
		BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			protected void stopBtService() {
				Switch toggle = (Switch) getView().findViewById(R.id.button_bt_service_start_stop);
				toggle.setChecked(true);

				serviceRunning = true;

				super.stopBtService();

				assertFalse("Switch should be in disabled state", toggle.isChecked());
				assertFalse("serviceRunning should be false", serviceRunning);
			}
		};

		addFragment(fragment);
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#startBtService()}
	 *
	 * This will only test the state of the boolean serviceRunning and the state
	 * of the related UI elements. Can't get a reference to the service directly.
	 */
	public void testStartService() {
		BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			protected void startBtService() {
				Switch toggle = (Switch) getView().findViewById(R.id.button_bt_service_start_stop);
				toggle.setChecked(false);

				serviceRunning = false;

				super.startBtService();

				assertTrue("Switch should be in enabled state", toggle.isChecked());
				assertTrue("serviceRunning should be true", serviceRunning);
			}
		};

		addFragment(fragment);
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment#updateSignalStrength(int)}
	 */
	public void testUpdateSignalStrength() {
		/*BluetoothFragment fragment = new BluetoothFragment() {
			@Override
			protected void updateSignalStrength(int newSignalStrength) {
				TextView signalView = (TextView) getView().findViewById(R.id.bt_signal_strength);

				newSignalStrength = Integer.MIN_VALUE;
				super.updateSignalStrength(newSignalStrength);
				assertEquals("Signal strength at INT.MIN should be 'Loading...'",
						getResources().getString(R.string.loading),
						signalView.getText());

				newSignalStrength = 5;
				super.updateSignalStrength(newSignalStrength);
				assertEquals("Signal strength at INT.MIN should be 5", "5", signalView.getText());
			}
		};

		addFragment(fragment);*/
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.BluetoothFragment.SignalStrengthUpdateReceiver#onReceive(android.content.Context, android.content.Intent)}
	 */
	public void testOnReceive() {
		ReceiverTest receiver = new ReceiverTest();
		addFragment(receiver);

		LocalBroadcastManager manager = LocalBroadcastManager.getInstance(getInstrumentation().getTargetContext());
		IntentFilter filter = new IntentFilter();
		filter.addAction(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION);
		filter.addAction(SignalReaderService.BT_ENABLE_BUTTON_ACTION);
		manager.registerReceiver(receiver.getReceiver(), filter);

		Intent testIntent = new Intent(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION);
		testIntent.putExtra("message", 6969);
		manager.sendBroadcast(testIntent);

		Intent testIntent2 = new Intent(SignalReaderService.BT_ENABLE_BUTTON_ACTION);
		testIntent2.putExtra("message", "lol");
		manager.sendBroadcast(testIntent2);
	}

	public static class ReceiverTest extends BluetoothFragment {
		private BluetoothFragment.SignalStrengthUpdateReceiver receiver = new SignalStrengthUpdateReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(SignalReaderService.BT_SIGNAL_STRENGTH_ACTION)) {
					assertEquals("Test whether signal strength message was received successfully", 6969,
							intent.getIntExtra("message", 0));
				} else if(intent.getAction().equals(SignalReaderService.BT_ENABLE_BUTTON_ACTION)) {
					assertEquals("Test whether String message was received successfully", "lol",
							intent.getStringExtra("message"));
				}
			}
		};
		public BluetoothFragment.SignalStrengthUpdateReceiver getReceiver() {
			return receiver;
		}
		@Override
		protected void updateSignalStrength(int newSignalStrength) {
			//Do nothing
		}
	}
}
