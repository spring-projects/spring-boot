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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.boot.buildpack.platform.docker.ProgressUpdateEvent.ProgressDetail;

/**
 * {@link UpdateListener} that calculates the total progress of the entire pull operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class TotalProgressPullListener implements UpdateListener<PullImageUpdateEvent> {

	private final Map<String, Layer> layers = new ConcurrentHashMap<>();

	private final Consumer<TotalProgressEvent> consumer;

	private boolean progressStarted;

	/**
	 * Create a new {@link TotalProgressPullListener} that prints a progress bar to
	 * {@link System#out}.
	 * @param prefix the prefix to output
	 */
	public TotalProgressPullListener(String prefix) {
		this(new TotalProgressBar(prefix));
	}

	/**
	 * Create a new {@link TotalProgressPullListener} that sends {@link TotalProgressEvent
	 * events} to the given consumer.
	 * @param consumer the consumer that receives {@link TotalProgressEvent progress
	 * events}
	 */
	public TotalProgressPullListener(Consumer<TotalProgressEvent> consumer) {
		this.consumer = consumer;
	}

	@Override
	public void onStart() {
	}

	@Override
	public void onUpdate(PullImageUpdateEvent event) {
		if (event.getId() != null) {
			this.layers.computeIfAbsent(event.getId(), Layer::new).update(event);
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

		private int downloadProgress;

		private int extractProgress;

		Layer(String id) {
		}

		void update(PullImageUpdateEvent event) {
			if (event.getProgressDetail() != null) {
				ProgressDetail detail = event.getProgressDetail();
				if ("Downloading".equals(event.getStatus())) {
					this.downloadProgress = updateProgress(this.downloadProgress, detail);
				}
				if ("Extracting".equals(event.getStatus())) {
					this.extractProgress = updateProgress(this.extractProgress, detail);
				}
			}
		}

		private int updateProgress(int current, ProgressDetail detail) {
			int result = withinPercentageBounds((int) ((100.0 / detail.getTotal()) * detail.getCurrent()));
			return Math.max(result, current);
		}

		void finish() {
			this.downloadProgress = 100;
			this.extractProgress = 100;
		}

		int getProgress() {
			return withinPercentageBounds((this.downloadProgress + this.extractProgress) / 2);
		}

	}

}
