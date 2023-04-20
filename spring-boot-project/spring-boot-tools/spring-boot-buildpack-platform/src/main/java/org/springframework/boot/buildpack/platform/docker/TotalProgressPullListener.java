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

import java.util.function.Consumer;

/**
 * {@link UpdateListener} that calculates the total progress of the entire pull operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 2.3.0
 */
public class TotalProgressPullListener extends TotalProgressListener<PullImageUpdateEvent> {

	private static final String[] TRACKED_STATUS_KEYS = { "Downloading", "Extracting" };

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
		super(consumer, TRACKED_STATUS_KEYS);
	}

}
