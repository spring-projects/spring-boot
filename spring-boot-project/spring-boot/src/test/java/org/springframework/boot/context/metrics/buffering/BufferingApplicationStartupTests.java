/*
 * Copyright 2012-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.metrics.buffering.StartupTimeline.TimelineEvent;
import org.springframework.core.metrics.StartupStep;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BufferingApplicationStartup}.
 *
 * @author Brian Clozel
 * @author Phillip Webb
 */
class BufferingApplicationStartupTests {

	@Test
	void shouldNotRecordEventsWhenOverCapacity() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		applicationStartup.start("first").end();
		applicationStartup.start("second").end();
		applicationStartup.start("third").end();
		assertThat(applicationStartup.getBufferedTimeline().getEvents()).hasSize(2);
	}

	@Test
	void shouldNotRecordEventsWhenFiltered() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(5);
		applicationStartup.addFilter((step) -> step.getName().startsWith("spring"));
		applicationStartup.start("spring.first").end();
		StartupStep filtered = applicationStartup.start("filtered.second");
		applicationStartup.start("spring.third").end();
		filtered.end();
		List<TimelineEvent> events = applicationStartup.getBufferedTimeline().getEvents();
		assertThat(events).hasSize(2);
		StartupTimeline.TimelineEvent firstEvent = events.get(0);
		assertThat(firstEvent.getStartupStep().getId()).isEqualTo(0);
		assertThat(firstEvent.getStartupStep().getParentId()).isNull();
		StartupTimeline.TimelineEvent secondEvent = events.get(1);
		assertThat(secondEvent.getStartupStep().getId()).isEqualTo(2);
		assertThat(secondEvent.getStartupStep().getParentId()).isEqualTo(1);
	}

	@Test
	void bufferShouldNotBeEmptyWhenGettingSnapshot() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		applicationStartup.start("first").end();
		applicationStartup.start("second").end();
		assertThat(applicationStartup.getBufferedTimeline().getEvents()).hasSize(2);
		assertThat(applicationStartup.getBufferedTimeline().getEvents()).hasSize(2);
	}

	@Test
	void bufferShouldBeEmptyWhenDraining() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		applicationStartup.start("first").end();
		applicationStartup.start("second").end();
		assertThat(applicationStartup.drainBufferedTimeline().getEvents()).hasSize(2);
		assertThat(applicationStartup.getBufferedTimeline().getEvents()).isEmpty();
	}

	@Test
	void startRecordingShouldFailIfEventsWereRecorded() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		applicationStartup.start("first").end();
		assertThatThrownBy(applicationStartup::startRecording).isInstanceOf(IllegalStateException.class)
				.hasMessage("Cannot restart recording once steps have been buffered.");
	}

	@Test
	void taggingShouldFailWhenEventAlreadyRecorded() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		StartupStep step = applicationStartup.start("first");
		step.end();
		assertThatThrownBy(() -> step.tag("name", "value")).isInstanceOf(IllegalStateException.class)
				.hasMessage("StartupStep has already ended.");
	}

	@Test
	void taggingShouldFailWhenRemovingEntry() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(2);
		StartupStep step = applicationStartup.start("first");
		step.tag("name", "value");
		assertThatThrownBy(() -> step.getTags().iterator().remove()).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test // gh-25792
	void outOfOrderWithMultipleEndCallsShouldNotFail() {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(200);
		StartupStep one = applicationStartup.start("one");
		StartupStep two = applicationStartup.start("two");
		StartupStep three = applicationStartup.start("three");
		two.end();
		two.end();
		two.end();
		StartupStep four = applicationStartup.start("four");
		four.end();
		three.end();
		one.end();
	}

	@Test // gh-25792
	void multiThreadedAccessShouldWork() throws InterruptedException {
		BufferingApplicationStartup applicationStartup = new BufferingApplicationStartup(5000);
		Queue<Exception> errors = new ConcurrentLinkedQueue<>();
		List<Thread> threads = new ArrayList<>();
		for (int thread = 0; thread < 20; thread++) {
			String prefix = "thread-" + thread + "-";
			threads.add(new Thread(() -> {
				try {
					for (int i = 0; i < 100; i++) {
						StartupStep step = applicationStartup.start(prefix + i);
						try {
							Thread.sleep(1);
						}
						catch (InterruptedException ex) {
						}
						step.end();
					}
				}
				catch (Exception ex) {
					errors.add(ex);
				}
			}));
		}
		threads.forEach(Thread::start);
		for (Thread thread : threads) {
			thread.join();
		}
		assertThat(errors).isEmpty();
	}

}
