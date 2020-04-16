/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link Health}.
 *
 * @author Phillip Webb
 * @author Michael Pratt
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class HealthTests {

	@Test
	void statusMustNotBeNull() {
		assertThatIllegalArgumentException().isThrownBy(() -> new Health.Builder(null, null))
				.withMessageContaining("Status must not be null");
	}

	@Test
	void createWithStatus() {
		Health health = Health.status(Status.UP).build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void createWithDetails() {
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).contains(entry("a", "b"));
		assertThat(health.getDetails()).containsKey(Builder.DURATION_LABEL);
	}

	@Test
	void equalsAndHashCode() {
		Health h1 = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).build();
		Health h2 = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).build();
		Health h3 = new Health.Builder(Status.UP).build();
		assertThat(h1).isEqualTo(h1);
		assertThat(h1).isEqualTo(h2);
		assertThat(h1).isNotEqualTo(h3);
		assertThat(h1.hashCode()).isEqualTo(h1.hashCode());
		assertThat(h1.hashCode()).isEqualTo(h2.hashCode());
		assertThat(h1.hashCode()).isNotEqualTo(h3.hashCode());
	}

	@Test
	void withException() {
		RuntimeException ex = new RuntimeException("bang");
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).withException(ex).build();
		assertThat(health.getDetails()).contains(entry("a", "b"), entry("error", "java.lang.RuntimeException: bang"));
		assertThat(health.getDetails()).containsOnlyKeys("a", "error", Builder.DURATION_LABEL);
	}

	@Test
	void withDetails() {
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).withDetail("c", "d").build();
		assertThat(health.getDetails()).contains(entry("a", "b"), entry("c", "d"));
		assertThat(health.getDetails()).containsOnlyKeys("a", "c", Builder.DURATION_LABEL);
	}

	@Test
	void withoutDetails() {
		Health health = new Health.Builder(Status.UP, Collections.singletonMap("a", "b")).build();
		assertThat(health.withoutDetails().getDetails()).doesNotContainKeys("a", Builder.DURATION_LABEL);
	}

	@Test
	void withDetailsMap() {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("a", "b");
		details.put("c", "d");
		Health health = Health.up().withDetails(details).build();
		assertThat(health.getDetails()).contains(entry("a", "b"), entry("c", "d"));
		assertThat(health.getDetails()).containsOnlyKeys("a", "c", Builder.DURATION_LABEL);
	}

	@Test
	void withDetailsMapDuplicateKeys() {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("c", "d");
		details.put("a", "e");
		Health health = Health.up().withDetail("a", "b").withDetails(details).build();
		assertThat(health.getDetails()).contains(entry("a", "e"), entry("c", "d"));
		assertThat(health.getDetails()).containsOnlyKeys("a", "c", Builder.DURATION_LABEL);
	}

	@Test
	void withDetailsMultipleMaps() {
		Map<String, Object> details1 = new LinkedHashMap<>();
		details1.put("a", "b");
		details1.put("c", "d");
		Map<String, Object> details2 = new LinkedHashMap<>();
		details1.put("a", "e");
		details1.put("1", "2");
		Health health = Health.up().withDetails(details1).withDetails(details2).build();
		assertThat(health.getDetails()).contains(entry("a", "e"), entry("c", "d"), entry("1", "2"));
		assertThat(health.getDetails()).containsOnlyKeys("a", "c", "1", Builder.DURATION_LABEL);
	}

	@Test
	void unknownWithDetails() {
		Health health = new Health.Builder().unknown().withDetail("a", "b").build();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails()).contains(entry("a", "b"));
		assertThat(health.getDetails()).containsOnlyKeys("a", Builder.DURATION_LABEL);
	}

	@Test
	void unknown() {
		Health health = new Health.Builder().unknown().build();
		assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void upWithDetails() {
		Health health = new Health.Builder().up().withDetail("a", "b").build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).contains(entry("a", "b"));
		assertThat(health.getDetails()).containsOnlyKeys("a", Builder.DURATION_LABEL);
	}

	@Test
	void up() {
		Health health = new Health.Builder().up().build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void downWithException() {
		RuntimeException ex = new RuntimeException("bang");
		Health health = Health.down(ex).build();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).contains(entry("error", "java.lang.RuntimeException: bang"));
		assertThat(health.getDetails()).containsOnlyKeys("error", Builder.DURATION_LABEL);
	}

	@Test
	void down() {
		Health health = Health.down().build();
		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void outOfService() {
		Health health = Health.outOfService().build();
		assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void statusCode() {
		Health health = Health.status("UP").build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void status() {
		Health health = Health.status(Status.UP).build();
		assertThat(health.getStatus()).isEqualTo(Status.UP);
		assertThat(health.getDetails()).containsOnlyKeys(Builder.DURATION_LABEL);
	}

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		Health health = Health.down().withDetail("a", "b").build();
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(health);
		assertThat(json)
				.containsPattern("\\{\"status\":\"DOWN\",\"details\":\\{\"a\":\"b\",\"durationNanos\":[0-9].*}}");
	}

}
