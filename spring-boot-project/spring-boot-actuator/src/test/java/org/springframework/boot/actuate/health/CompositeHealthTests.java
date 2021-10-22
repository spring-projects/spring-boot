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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.ApiVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Test for {@link CompositeHealth}.
 *
 * @author Phillip Webb
 */
class CompositeHealthTests {

	@Test
	void createWhenStatusIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CompositeHealth(ApiVersion.V3, null, Collections.emptyMap()))
				.withMessage("Status must not be null");
	}

	@Test
	void getStatusReturnsStatus() {
		CompositeHealth health = new CompositeHealth(ApiVersion.V3, Status.UP, Collections.emptyMap());
		assertThat(health.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void getComponentReturnsComponents() {
		Map<String, HealthComponent> components = new LinkedHashMap<>();
		components.put("a", Health.up().build());
		CompositeHealth health = new CompositeHealth(ApiVersion.V3, Status.UP, components);
		assertThat(health.getComponents()).isEqualTo(components);
	}

	@Test
	void serializeV3WithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthComponent> components = new LinkedHashMap<>();
		components.put("db1", Health.up().build());
		components.put("db2", Health.down().withDetail("a", "b").build());
		CompositeHealth health = new CompositeHealth(ApiVersion.V3, Status.UP, components);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(health);
		assertThat(json).isEqualTo("{\"status\":\"UP\",\"components\":{\"db1\":{\"status\":\"UP\"},"
				+ "\"db2\":{\"status\":\"DOWN\",\"details\":{\"a\":\"b\"}}}}");
	}

	@Test
	void serializeV2WithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthComponent> components = new LinkedHashMap<>();
		components.put("db1", Health.up().build());
		components.put("db2", Health.down().withDetail("a", "b").build());
		CompositeHealth health = new CompositeHealth(ApiVersion.V2, Status.UP, components);
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(health);
		assertThat(json).isEqualTo("{\"status\":\"UP\",\"details\":{\"db1\":{\"status\":\"UP\"},"
				+ "\"db2\":{\"status\":\"DOWN\",\"details\":{\"a\":\"b\"}}}}");
	}

	@Test // gh-26797
	void serializeV2WithJacksonAndDisabledCanOverrideAccessModifiersReturnsValidJson() throws Exception {
		Map<String, HealthComponent> components = new LinkedHashMap<>();
		components.put("db1", Health.up().build());
		components.put("db2", Health.down().withDetail("a", "b").build());
		CompositeHealth health = new CompositeHealth(ApiVersion.V2, Status.UP, components);
		JsonMapper mapper = JsonMapper.builder().disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS).build();
		String json = mapper.writeValueAsString(health);
		assertThat(json).isEqualTo("{\"status\":\"UP\",\"details\":{\"db1\":{\"status\":\"UP\"},"
				+ "\"db2\":{\"status\":\"DOWN\",\"details\":{\"a\":\"b\"}}}}");
	}

}
