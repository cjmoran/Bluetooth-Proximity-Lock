package com.javadog.bluetoothproximitylock.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Switch;

/**
 * Implementation of Android's Switch which can differentiate between user and programmatic changes to onChecked state.
 */
public class BetterSwitch extends Switch {
	private OnCheckedChangeListener listener = null;
	private Switch mSwitch;

	@SuppressWarnings("unused") //Used by android tools and IDE complains about it being removed...
	public BetterSwitch(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BetterSwitch(Context context, Switch switchArg) {
		super(context);
		mSwitch = switchArg;
	}

	@Override
	public void setOnCheckedChangeListener(OnCheckedChangeListener listenerArg) {
		if(listener == null || (!listener.equals(listenerArg) && listenerArg != null)) {
			listener = listenerArg;
		}
		mSwitch.setOnCheckedChangeListener(listenerArg);
	}

	/**
	 * Sets the checked state of this Switch without notifying the onCheckedChangedListener.
	 *
	 * @param checked Checked?
	 */
	public void silentlySetChecked(boolean checked) {
		toggleListener(false);
		mSwitch.setChecked(checked);
		toggleListener(true);
	}

	/**
	 * Enables/disables the onCheckedChangedListener.
	 *
	 * @param on On?
	 */
	private void toggleListener(boolean on) {
		setOnCheckedChangeListener(on ? listener : null);
	}
}
