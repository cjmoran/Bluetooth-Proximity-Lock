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

package com.javadog.bluetoothproximitylock.test;

import com.javadog.bluetoothproximitylock.helpers.CircularQueue;

import junit.framework.TestCase;

/**
 * Unit tests for {@link com.javadog.bluetoothproximitylock.helpers.CircularQueue}
 */
@SuppressWarnings("ManualArrayToCollectionCopy")
public class CircularQueueTest extends TestCase {
	final Integer[] samples = {1, 2, 3, 4, 5, 6};
	CircularQueue<Integer> testQueue;

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.helpers.CircularQueue#add(Object)}
	 */
	public void testAdd() {
		testQueue = new CircularQueue<>(5);

		for(Integer i : samples) {
			testQueue.add(i);
		}

		assertFalse("Queue should have deleted 1", testQueue.contains(1));
		assertTrue("Queue should contain 2", testQueue.contains(2));
		assertTrue("Queue should contain 6", testQueue.contains(6));
	}

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.helpers.CircularQueue#getAverageOfElements()}
	 */
	public void testGetAverageOfElements() {
		testQueue = new CircularQueue<>(5);
		float expected = (samples[1] + samples[2] + samples[3] + samples[4] + samples[5]) / 5;
		for(Integer i : samples) {
			testQueue.add(i);
		}
		float result = testQueue.getAverageOfElements();
		assertEquals("(2+3+4+5+6)/5 should equal 4", expected, result);

		//Test using 0
		testQueue = new CircularQueue<>(0);
		for(Integer i : samples) {
			testQueue.add(i);
		}
		try {
			result = testQueue.getAverageOfElements();
			fail("Should have thrown a divide by zero ArithmeticException");
		} catch(ArithmeticException e) {
			//Expected result.
			assertTrue("Message should contain zero division error", e.getMessage().contains("Division by zero"));
		}
	}
}
