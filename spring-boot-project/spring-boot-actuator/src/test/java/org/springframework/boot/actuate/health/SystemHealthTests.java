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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemHealth}.
 *
 * @author Phillip Webb
 */
class SystemHealthTests {

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthComponent> components = new LinkedHashMap<>();
		components.put("db1", Health.up().build());
		components.put("db2", Health.down().withDetail("a", "b").build());
		Set<String> groups = new LinkedHashSet<>(Arrays.asList("liveness", "readiness"));
		CompositeHealth health = new SystemHealth(ApiVersion.V3, Status.UP, components, groups);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(health);
		assertThat(json).isEqualTo("{\"status\":\"UP\",\"components\":{\"db1\":{\"status\":\"UP\"},"
				+ "\"db2\":{\"status\":\"DOWN\",\"details\":{\"a\":\"b\"}}},"
				+ "\"groups\":[\"liveness\",\"readiness\"]}");
	}

}
