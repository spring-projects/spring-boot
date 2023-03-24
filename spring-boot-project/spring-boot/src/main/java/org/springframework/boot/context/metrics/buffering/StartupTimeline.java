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

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.springframework.core.metrics.StartupStep;

/**
 * Represent the timeline of {@link StartupStep steps} recorded by
 * {@link BufferingApplicationStartup}. Each {@link TimelineEvent} has a start and end
 * time as well as a duration measured with nanosecond precision.
 *
 * @author Brian Clozel
 * @since 2.4.0
 */
public class StartupTimeline {

	private final Instant startTime;

	private final List<TimelineEvent> events;

	StartupTimeline(Instant startTime, List<TimelineEvent> events) {
		this.startTime = startTime;
		this.events = Collections.unmodifiableList(events);
	}

	/**
	 * Return the start time of this timeline.
	 * @return the start time
	 */
	public Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * Return the recorded events.
	 * @return the events
	 */
	public List<TimelineEvent> getEvents() {
		return this.events;
	}

	/**
	 * Event on the current {@link StartupTimeline}. Each event has a start/end time, a
	 * precise duration and the complete {@link StartupStep} information associated with
	 * it.
	 */
	public static class TimelineEvent {

		private final BufferedStartupStep step;

		private final Instant endTime;

		private final Duration duration;

		TimelineEvent(BufferedStartupStep step, Instant endTime) {
			this.step = step;
			this.endTime = endTime;
			this.duration = Duration.between(step.getStartTime(), endTime);
		}

		/**
		 * Return the start time of this event.
		 * @return the start time
		 */
		public Instant getStartTime() {
			return this.step.getStartTime();
		}

		/**
		 * Return the end time of this event.
		 * @return the end time
		 */
		public Instant getEndTime() {
			return this.endTime;
		}

		/**
		 * Return the duration of this event, i.e. the processing time of the associated
		 * {@link StartupStep} with nanoseconds precision.
		 * @return the event duration
		 */
		public Duration getDuration() {
			return this.duration;
		}

		/**
		 * Return the {@link StartupStep} information for this event.
		 * @return the step information.
		 */
		public StartupStep getStartupStep() {
			return this.step;
		}

	}

}
