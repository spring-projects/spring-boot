/*
 * Copyright 2012-present the original author or authors.
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
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link CompositeHealthDescriptor}.
 *
 * @author Phillip Webb
 */
class CompositeHealthDescriptorTests {

	@Test
	void createWhenApiVersionIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CompositeHealthDescriptor(null, Status.UP, Collections.emptyMap()))
			.withMessage("'apiVersion' must not be null");
	}

	@Test
	void createWhenStatusIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CompositeHealthDescriptor(ApiVersion.V3, null, Collections.emptyMap()))
			.withMessage("'status' must not be null");
	}

	@Test
	void getStatusReturnsStatus() {
		CompositeHealthDescriptor descriptor = new CompositeHealthDescriptor(ApiVersion.V3, Status.UP,
				Collections.emptyMap());
		assertThat(descriptor.getStatus()).isEqualTo(Status.UP);
	}

	@Test
	void getComponentReturnsComponents() {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("a", new IndicatedHealthDescriptor(Health.up().build()));
		CompositeHealthDescriptor descriptor = new CompositeHealthDescriptor(ApiVersion.V3, Status.UP, components);
		assertThat(descriptor.getComponents()).isEqualTo(components);
	}

	@Test
	void serializeV3WithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("db1", new IndicatedHealthDescriptor(Health.up().build()));
		components.put("db2", new IndicatedHealthDescriptor(Health.down().withDetail("a", "b").build()));
		CompositeHealthDescriptor descriptor = new CompositeHealthDescriptor(ApiVersion.V3, Status.UP, components);
		ObjectMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"components":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"status":"UP"}""");
	}

	@Test
	void serializeV2WithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("db1", new IndicatedHealthDescriptor(Health.up().build()));
		components.put("db2", new IndicatedHealthDescriptor(Health.down().withDetail("a", "b").build()));
		CompositeHealthDescriptor descriptor = new CompositeHealthDescriptor(ApiVersion.V2, Status.UP, components);
		ObjectMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"details":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"status":"UP"}""");
	}

}
