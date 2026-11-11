/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import java.util.function.Consumer;

/**
 * {@link UpdateListener} that calculates the total progress of the entire push operation
 * and publishes {@link TotalProgressEvent}.
 *
 * @author Scott Frederick
 * @since 2.4.0
 */
public class TotalProgressPushListener extends TotalProgressListener<PushImageUpdateEvent> {

	private static final String[] TRACKED_STATUS_KEYS = { "Pushing" };

	/**
	 * Create a new {@link TotalProgressPushListener} that prints a progress bar to
	 * {@link System#out}.
	 * @param prefix the prefix to output
	 */
	public TotalProgressPushListener(String prefix) {
		this(new TotalProgressBar(prefix));
	}

	/**
	 * Create a new {@link TotalProgressPushListener} that sends {@link TotalProgressEvent
	 * events} to the given consumer.
	 * @param consumer the consumer that receives {@link TotalProgressEvent progress
	 * events}
	 */
	public TotalProgressPushListener(Consumer<TotalProgressEvent> consumer) {
		super(consumer, TRACKED_STATUS_KEYS);
	}

}
