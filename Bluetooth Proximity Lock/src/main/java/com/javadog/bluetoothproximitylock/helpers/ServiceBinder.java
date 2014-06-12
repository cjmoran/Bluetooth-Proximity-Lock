package com.javadog.bluetoothproximitylock.helpers;

import android.os.Binder;

import java.lang.ref.WeakReference;

/**
 * Allows an activity to bind to a Service of type T.
 *
 * Implemented this way because of a bug in Android, see below:
 * http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android
 */
public class ServiceBinder<T> extends Binder {
	private WeakReference<T> weakService;

	/**
	 * @param service A reference to the service which will be bound.
	 */
	public ServiceBinder(T service) {
		weakService = new WeakReference<T>(service);
	}

	T getService() {
		return weakService.get();
	}
}
