/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SystemHealthDescriptor}.
 *
 * @author Phillip Webb
 */
class SystemHealthDescriptorTests {

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("db1", new IndicatedHealthDescriptor(Health.up().build()));
		components.put("db2", new IndicatedHealthDescriptor(Health.down().withDetail("a", "b").build()));
		Set<String> groups = new LinkedHashSet<>(Arrays.asList("liveness", "readiness"));
		SystemHealthDescriptor descriptor = new SystemHealthDescriptor(ApiVersion.V3, Status.UP, components, groups);
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo(
				"""
						{"components":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"groups":["liveness","readiness"],"status":"UP"}""");
	}

	@Test
	void serializeWhenNoGroupsWithJacksonReturnsValidJson() throws Exception {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("db1", new IndicatedHealthDescriptor(Health.up().build()));
		components.put("db2", new IndicatedHealthDescriptor(Health.down().withDetail("a", "b").build()));
		SystemHealthDescriptor descriptor = new SystemHealthDescriptor(ApiVersion.V3, Status.UP, components, null);
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"components":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"status":"UP"}""");
	}

	@Test // gh-26797
	void serializeV2WithJacksonAndDisabledCanOverrideAccessModifiersReturnsValidJson() throws Exception {
		Map<String, HealthDescriptor> components = new LinkedHashMap<>();
		components.put("db1", new IndicatedHealthDescriptor(Health.up().build()));
		components.put("db2", new IndicatedHealthDescriptor(Health.down().withDetail("a", "b").build()));
		SystemHealthDescriptor descriptor = new SystemHealthDescriptor(ApiVersion.V2, Status.UP, components, null);
		JsonMapper mapper = JsonMapper.builder()
			.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
			.build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"details":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"status":"UP"}""");
	}

}
