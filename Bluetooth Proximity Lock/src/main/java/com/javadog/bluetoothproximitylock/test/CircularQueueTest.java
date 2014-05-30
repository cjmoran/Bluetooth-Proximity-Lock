package com.javadog.bluetoothproximitylock.test;

import com.javadog.bluetoothproximitylock.CircularQueue;

import junit.framework.TestCase;

/**
 * Unit tests for {@link com.javadog.bluetoothproximitylock.CircularQueue}
 */
@SuppressWarnings("ManualArrayToCollectionCopy")
public class CircularQueueTest extends TestCase {
	final Integer[] samples = {1, 2, 3, 4, 5, 6};
	CircularQueue<Integer> testQueue;

	/**
	 * Tests {@link com.javadog.bluetoothproximitylock.CircularQueue#add(Object)}
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
	 * Tests {@link com.javadog.bluetoothproximitylock.CircularQueue#getAverageOfElements()}
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
