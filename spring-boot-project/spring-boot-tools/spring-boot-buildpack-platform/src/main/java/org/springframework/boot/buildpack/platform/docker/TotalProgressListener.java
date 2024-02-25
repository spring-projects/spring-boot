/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;

/**
 * {@link UpdateListener} that calculates the total progress of the entire image operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @param <E> the type of {@link ImageProgressUpdateEvent}
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.4.0
 */
public abstract class TotalProgressListener<E extends ImageProgressUpdateEvent> implements UpdateListener<E> {

	private final Map<String, Layer> layers = new ConcurrentHashMap<>();

	private final Consumer<TotalProgressEvent> consumer;

	private final String[] trackedStatusKeys;

	private boolean progressStarted;

	/**
	 * Create a new {@link TotalProgressListener} that sends {@link TotalProgressEvent
	 * events} to the given consumer.
	 * @param consumer the consumer that receives {@link TotalProgressEvent progress
	 * events}
	 * @param trackedStatusKeys a list of status event keys to track the progress of
	 */
	protected TotalProgressListener(Consumer<TotalProgressEvent> consumer, String[] trackedStatusKeys) {
		this.consumer = consumer;
		this.trackedStatusKeys = trackedStatusKeys;
	}

	/**
     * Called when the progress listener is started.
     */
    @Override
	public void onStart() {
	}

	/**
     * This method is called when an event is updated.
     * 
     * @param event the event to be updated
     */
    @Override
	public void onUpdate(E event) {
		if (event.getId() != null) {
			this.layers.computeIfAbsent(event.getId(), (value) -> new Layer(this.trackedStatusKeys)).update(event);
		}
		this.progressStarted = this.progressStarted || event.getProgress() != null;
		if (this.progressStarted) {
			publish(0);
		}
	}

	/**
     * Called when the progress has reached its maximum value and is finished.
     * This method iterates through all the layers and calls the finish method on each layer.
     * It then publishes the progress value as 100.
     */
    @Override
	public void onFinish() {
		this.layers.values().forEach(Layer::finish);
		publish(100);
	}

	/**
     * Publishes the total progress of all layers.
     * 
     * @param fallback the fallback value to use if there are no layers
     */
    private void publish(int fallback) {
		int count = 0;
		int total = 0;
		for (Layer layer : this.layers.values()) {
			count++;
			total += layer.getProgress();
		}
		TotalProgressEvent event = new TotalProgressEvent(
				(count != 0) ? withinPercentageBounds(total / count) : fallback);
		this.consumer.accept(event);
	}

	/**
     * Returns the value within the percentage bounds of 0 and 100.
     *
     * @param value the value to be checked
     * @return the value within the percentage bounds
     */
    private static int withinPercentageBounds(int value) {
		if (value < 0) {
			return 0;
		}
		return Math.min(value, 100);
	}

	/**
	 * Progress for an individual layer.
	 */
	private static class Layer {

		private final Map<String, Integer> progressByStatus = new HashMap<>();

		/**
         * Initializes a new instance of the Layer class with the specified tracked status keys.
         * 
         * @param trackedStatusKeys an array of strings representing the tracked status keys
         */
        Layer(String[] trackedStatusKeys) {
			Arrays.stream(trackedStatusKeys).forEach((status) -> this.progressByStatus.put(status, 0));
		}

		/**
         * Updates the progress of an image based on the given event.
         * 
         * @param event the ImageProgressUpdateEvent containing the progress update information
         */
        void update(ImageProgressUpdateEvent event) {
			String status = event.getStatus();
			if (event.getProgressDetail() != null && this.progressByStatus.containsKey(status)) {
				int current = this.progressByStatus.get(status);
				this.progressByStatus.put(status, updateProgress(current, event.getProgressDetail()));
			}
		}

		/**
         * Updates the progress based on the current progress detail.
         * 
         * @param current the current progress value
         * @param detail the progress detail object containing the current and total progress values
         * @return the updated progress value
         */
        private int updateProgress(int current, ProgressDetail detail) {
			int result = withinPercentageBounds((int) ((100.0 / detail.getTotal()) * detail.getCurrent()));
			return Math.max(result, current);
		}

		/**
         * Sets the progress of all statuses to 100, indicating that the layer has been finished.
         */
        void finish() {
			this.progressByStatus.keySet().forEach((key) -> this.progressByStatus.put(key, 100));
		}

		/**
         * Returns the average progress of all statuses in the layer.
         * 
         * @return the average progress as an integer value within the percentage bounds
         */
        int getProgress() {
			return withinPercentageBounds((this.progressByStatus.values().stream().mapToInt(Integer::valueOf).sum())
					/ this.progressByStatus.size());
		}

	}

}
