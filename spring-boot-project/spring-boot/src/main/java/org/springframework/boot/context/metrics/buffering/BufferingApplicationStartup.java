/*
 * Copyright 2002-2021 the original author or authors.
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

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.springframework.boot.context.metrics.buffering.StartupTimeline.TimelineEvent;
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
 * @author Phillip Webb
 * @since 2.4.0
 */
public class BufferingApplicationStartup implements ApplicationStartup {

	private final int capacity;

	private final Clock clock;

	private Instant startTime;

	private final AtomicInteger idSeq = new AtomicInteger();

	private Predicate<StartupStep> filter = (step) -> true;

	private final AtomicReference<BufferedStartupStep> current = new AtomicReference<>();

	private final AtomicInteger estimatedSize = new AtomicInteger();

	private final ConcurrentLinkedQueue<TimelineEvent> events = new ConcurrentLinkedQueue<>();

	/**
	 * Create a new buffered {@link ApplicationStartup} with a limited capacity and starts
	 * the recording of steps.
	 * @param capacity the configured capacity; once reached, new steps are not recorded.
	 */
	public BufferingApplicationStartup(int capacity) {
		this(capacity, Clock.systemDefaultZone());
	}

	BufferingApplicationStartup(int capacity, Clock clock) {
		this.capacity = capacity;
		this.clock = clock;
		this.startTime = clock.instant();
	}

	/**
	 * Start the recording of steps and mark the beginning of the {@link StartupTimeline}.
	 * The class constructor already implicitly calls this, but it is possible to reset it
	 * as long as steps have not been recorded already.
	 * @throws IllegalStateException if called and {@link StartupStep} have been recorded
	 * already.
	 */
	public void startRecording() {
		Assert.state(this.events.isEmpty(), "Cannot restart recording once steps have been buffered.");
		this.startTime = this.clock.instant();
	}

	/**
	 * Add a predicate filter to the list of existing ones.
	 * <p>
	 * A {@link StartupStep step} that doesn't match all filters will not be recorded.
	 * @param filter the predicate filter to add.
	 */
	public void addFilter(Predicate<StartupStep> filter) {
		this.filter = this.filter.and(filter);
	}

	@Override
	public StartupStep start(String name) {
		int id = this.idSeq.getAndIncrement();
		Instant start = this.clock.instant();
		while (true) {
			BufferedStartupStep current = this.current.get();
			BufferedStartupStep parent = getLatestActive(current);
			BufferedStartupStep next = new BufferedStartupStep(parent, name, id, start, this::record);
			if (this.current.compareAndSet(current, next)) {
				return next;
			}
		}
	}

	private void record(BufferedStartupStep step) {
		if (this.filter.test(step) && this.estimatedSize.get() < this.capacity) {
			this.estimatedSize.incrementAndGet();
			this.events.add(new TimelineEvent(step, this.clock.instant()));
		}
		while (true) {
			BufferedStartupStep current = this.current.get();
			BufferedStartupStep next = getLatestActive(current);
			if (this.current.compareAndSet(current, next)) {
				return;
			}
		}
	}

	private BufferedStartupStep getLatestActive(BufferedStartupStep step) {
		while (step != null && step.isEnded()) {
			step = step.getParent();
		}
		return step;
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
		return new StartupTimeline(this.startTime, new ArrayList<>(this.events));
	}

	/**
	 * Return the {@link StartupTimeline timeline} by pulling steps from the buffer.
	 * <p>
	 * This removes steps from the buffer, see {@link #getBufferedTimeline()} for its
	 * read-only counterpart.
	 * @return buffered steps drained from the buffer.
	 */
	public StartupTimeline drainBufferedTimeline() {
		List<TimelineEvent> events = new ArrayList<>();
		Iterator<TimelineEvent> iterator = this.events.iterator();
		while (iterator.hasNext()) {
			events.add(iterator.next());
			iterator.remove();
		}
		this.estimatedSize.set(0);
		return new StartupTimeline(this.startTime, events);
	}

}
