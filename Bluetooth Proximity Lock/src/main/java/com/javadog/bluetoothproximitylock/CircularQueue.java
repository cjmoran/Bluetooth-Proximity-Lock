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

import android.util.Log;

import java.util.LinkedList;

/**
 * A queue (FIFO) which holds the last N elements. Used to keep an average of proximity samples.
 */
public class CircularQueue<Integer> extends LinkedList<Integer> {
	private final int limit;

	/**
	 * @param maxElements The maximum number of elements to hold.
	 */
	public CircularQueue(int maxElements) {
		limit = maxElements;
	}

	@Override
	public boolean add(Integer element) {
		super.add(element);
		while(size() > limit) {
			super.remove();
		}

		return true;
	}

	/**
	 * Note: Only works if the values stored in this CircularQueue are Integer objects.
	 *
	 * @return an average of all N values stored in this CircularQueue. If # samples < limit, returns a very small #.
	 */
	public float getAverageOfElements() {
		float result;

		if(size() < limit) {
			result = Float.MIN_VALUE;
		} else {
			Float avg = 0f;
			for(Integer element : this) {
				avg += (java.lang.Integer) element;
			}
			result = avg / limit;
		}

		Log.d(MainActivity.DEBUG_TAG, "Average of last five distance samples: " + result);

		return result;
	}
}
