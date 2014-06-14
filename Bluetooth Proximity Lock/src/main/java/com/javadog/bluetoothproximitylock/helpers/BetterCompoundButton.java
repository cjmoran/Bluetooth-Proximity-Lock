package com.javadog.bluetoothproximitylock.helpers;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CompoundButton;

/**
 * Implementation of Android's CompoundButton (like Switch, Checkbox) which can differentiate between user and
 * programmatic changes to checked state.
 */
public class BetterCompoundButton<T extends CompoundButton> extends CompoundButton {
	private OnCheckedChangeListener listener = null;
	private T mCompoundButton;

	@SuppressWarnings("unused") //Used by android tools and IDE complains about it being removed...
	public BetterCompoundButton(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BetterCompoundButton(Context context, T arg) {
		super(context);
		mCompoundButton = arg;
	}

	@Override
	public void setOnCheckedChangeListener(OnCheckedChangeListener listenerArg) {
		if(listener == null || (!listener.equals(listenerArg) && listenerArg != null)) {
			listener = listenerArg;
		}
		mCompoundButton.setOnCheckedChangeListener(listenerArg);
	}

	/**
	 * Sets the checked state of this Switch without notifying the onCheckedChangedListener.
	 *
	 * @param checked Checked?
	 */
	public void silentlySetChecked(boolean checked) {
		toggleListener(false);
		mCompoundButton.setChecked(checked);
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
