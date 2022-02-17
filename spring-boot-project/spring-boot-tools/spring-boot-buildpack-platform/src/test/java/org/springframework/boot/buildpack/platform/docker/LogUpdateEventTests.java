/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogUpdateEvent}.
 *
 * @author Phillip Webb
 */
class LogUpdateEventTests {

	@Test
	void readAllWhenSimpleStreamReturnsEvents() throws Exception {
		List<LogUpdateEvent> events = readAll("log-update-event.stream");
		assertThat(events).hasSize(7);
		assertThat(events.get(0).toString())
				.isEqualTo("Analyzing image '307c032c4ceaa6330b6c02af945a1fe56a8c3c27c28268574b217c1d38b093cf'");
		assertThat(events.get(1).toString())
				.isEqualTo("Writing metadata for uncached layer 'org.cloudfoundry.openjdk:openjdk-jre'");
		assertThat(events.get(2).toString())
				.isEqualTo("Using cached launch layer 'org.cloudfoundry.jvmapplication:executable-jar'");
	}

	@Test
	void readAllWhenAnsiStreamReturnsEvents() throws Exception {
		List<LogUpdateEvent> events = readAll("log-update-event-ansi.stream");
		assertThat(events).hasSize(20);
		assertThat(events.get(0).toString()).isEqualTo("");
		assertThat(events.get(1).toString()).isEqualTo("Cloud Foundry OpenJDK Buildpack v1.0.64");
		assertThat(events.get(2).toString()).isEqualTo("  OpenJDK JRE 11.0.5: Reusing cached layer");
	}

	@Test
	void readSucceedsWhenStreamTypeIsInvalid() throws IOException {
		List<LogUpdateEvent> events = readAll("log-update-event-invalid-stream-type.stream");
		assertThat(events).hasSize(1);
		assertThat(events.get(0).toString()).isEqualTo("Stream type is out of bounds. Must be >= 0 and < 3, but was 3");
	}

	private List<LogUpdateEvent> readAll(String name) throws IOException {
		List<LogUpdateEvent> events = new ArrayList<>();
		try (InputStream inputStream = getClass().getResourceAsStream(name)) {
			LogUpdateEvent.readAll(inputStream, events::add);
		}
		return events;
	}

}
