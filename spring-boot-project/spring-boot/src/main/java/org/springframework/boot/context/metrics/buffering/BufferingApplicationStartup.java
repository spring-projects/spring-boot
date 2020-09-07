/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.boot.context.metrics.buffering;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;
import org.springframework.util.Assert;

/**
 * {@link ApplicationStartup} implementation that buffers {@link StartupStep steps} and
 * records their timestamp as well as their processing time.
 * <p>
 * Once recording has been {@link #startRecording() started}, steps are buffered up until
 * the configured {@link #BufferingApplicationStartup(int) capacity}; after that, new
 * steps are not recorded.
 * <p>
 * There are several ways to keep the buffer size low:
 * <ul>
 * <li>configuring {@link #addFilter(Predicate) filters} to only record steps that are
 * relevant to us.
 * <li>{@link #drainBufferedTimeline() draining} the buffered steps.
 * </ul>
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
public class BufferingApplicationStartup implements ApplicationStartup {

	private Instant recordingStartTime;

	private long recordingStartNanos;

	private long currentSequenceId = 0;

	private final Deque<Long> currentSteps;

	private final BlockingQueue<BufferedStartupStep> recordedSteps;

	private Predicate<StartupStep> stepFilters = (step) -> true;

	/**
	 * Create a new buffered {@link ApplicationStartup} with a limited capacity and starts
	 * the recording of steps.
	 * @param capacity the configured capacity; once reached, new steps are not recorded.
	 */
	public BufferingApplicationStartup(int capacity) {
		this.currentSteps = new ArrayDeque<>();
		this.currentSteps.offerFirst(this.currentSequenceId);
		this.recordedSteps = new LinkedBlockingQueue<>(capacity);
		startRecording();
	}

	/**
	 * Start the recording of steps and mark the beginning of the {@link StartupTimeline}.
	 * The class constructor already implicitly calls this, but it is possible to reset it
	 * as long as steps have not been recorded already.
	 * @throws IllegalStateException if called and {@link StartupStep} have been recorded
	 * already.
	 */
	public void startRecording() {
		Assert.state(this.recordedSteps.isEmpty(), "Cannot restart recording once steps have been buffered.");
		this.recordingStartTime = Instant.now();
		this.recordingStartNanos = getCurrentTime();
	}

	/**
	 * Add a predicate filter to the list of existing ones.
	 * <p>
	 * A {@link StartupStep step} that doesn't match all filters will not be recorded.
	 * @param filter the predicate filter to add.
	 */
	public void addFilter(Predicate<StartupStep> filter) {
		this.stepFilters = this.stepFilters.and(filter);
	}

	/**
	 * Return the {@link StartupTimeline timeline} as a snapshot of currently buffered
	 * steps.
	 * <p>
	 * This will not remove steps from the buffer, see {@link #drainBufferedTimeline()}
	 * for its counterpart.
	 * @return a snapshot of currently buffered steps.
	 */
	public StartupTimeline getBufferedTimeline() {
		return new StartupTimeline(this.recordingStartTime, this.recordingStartNanos, this.recordedSteps);
	}

	/**
	 * Return the {@link StartupTimeline timeline} by pulling steps from the buffer.
	 * <p>
	 * This removes steps from the buffer, see {@link #getBufferedTimeline()} for its
	 * read-only counterpart.
	 * @return buffered steps drained from the buffer.
	 */
	public StartupTimeline drainBufferedTimeline() {
		List<BufferedStartupStep> steps = new ArrayList<>(this.recordedSteps.size());
		this.recordedSteps.drainTo(steps);
		return new StartupTimeline(this.recordingStartTime, this.recordingStartNanos, steps);
	}

	@Override
	public StartupStep start(String name) {
		BufferedStartupStep step = new BufferedStartupStep(++this.currentSequenceId, name,
				this.currentSteps.peekFirst(), this::record);
		step.recordStartTime(getCurrentTime());
		this.currentSteps.offerFirst(this.currentSequenceId);
		return step;
	}

	private void record(BufferedStartupStep step) {
		step.recordEndTime(getCurrentTime());
		if (this.stepFilters.test(step)) {
			this.recordedSteps.offer(step);
		}
		this.currentSteps.removeFirst();
	}

	private long getCurrentTime() {
		return System.nanoTime();
	}

}
