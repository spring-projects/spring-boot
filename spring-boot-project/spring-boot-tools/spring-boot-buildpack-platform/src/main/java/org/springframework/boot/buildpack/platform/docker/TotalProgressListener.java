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

	@Override
	public void onStart() {
	}

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

	@Override
	public void onFinish() {
		this.layers.values().forEach(Layer::finish);
		publish(100);
	}

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

		Layer(String[] trackedStatusKeys) {
			Arrays.stream(trackedStatusKeys).forEach((status) -> this.progressByStatus.put(status, 0));
		}

		void update(ImageProgressUpdateEvent event) {
			String status = event.getStatus();
			if (event.getProgressDetail() != null && this.progressByStatus.containsKey(status)) {
				int current = this.progressByStatus.get(status);
				this.progressByStatus.put(status, updateProgress(current, event.getProgressDetail()));
			}
		}

		private int updateProgress(int current, ProgressDetail detail) {
			int result = withinPercentageBounds((int) ((100.0 / detail.getTotal()) * detail.getCurrent()));
			return Math.max(result, current);
		}

		void finish() {
			this.progressByStatus.keySet().forEach((key) -> this.progressByStatus.put(key, 100));
		}

		int getProgress() {
			return withinPercentageBounds((this.progressByStatus.values().stream().mapToInt(Integer::valueOf).sum())
					/ this.progressByStatus.size());
		}

	}

}
