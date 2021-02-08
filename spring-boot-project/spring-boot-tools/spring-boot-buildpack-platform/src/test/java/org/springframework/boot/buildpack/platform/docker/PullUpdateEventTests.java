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

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.json.AbstractJsonTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PullImageUpdateEvent}.
 *
 * @author Phillip Webb
 */
class PullUpdateEventTests extends AbstractJsonTests {

	@Test
	void readValueWhenFullDeserializesJson() throws Exception {
		PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-update-full.json"),
				PullImageUpdateEvent.class);
		assertThat(event.getId()).isEqualTo("4f4fb700ef54");
		assertThat(event.getStatus()).isEqualTo("Extracting");
		assertThat(event.getProgressDetail().getCurrent()).isEqualTo(16);
		assertThat(event.getProgressDetail().getTotal()).isEqualTo(32);
		assertThat(event.getProgress()).isEqualTo("[==================================================>]      32B/32B");
	}

	@Test
	void readValueWhenMinimalDeserializesJson() throws Exception {
		PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-update-minimal.json"),
				PullImageUpdateEvent.class);
		assertThat(event.getId()).isNull();
		assertThat(event.getStatus()).isEqualTo("Status: Downloaded newer image for paketo-buildpacks/cnb:base");
		assertThat(event.getProgressDetail()).isNull();
		assertThat(event.getProgress()).isNull();
	}

	@Test
	void readValueWhenEmptyDetailsDeserializesJson() throws Exception {
		PullImageUpdateEvent event = getObjectMapper().readValue(getContent("pull-with-empty-details.json"),
				PullImageUpdateEvent.class);
		assertThat(event.getId()).isEqualTo("d837a2a1365e");
		assertThat(event.getStatus()).isEqualTo("Pulling fs layer");
		assertThat(event.getProgressDetail()).isNull();
		assertThat(event.getProgress()).isNull();
	}

}
