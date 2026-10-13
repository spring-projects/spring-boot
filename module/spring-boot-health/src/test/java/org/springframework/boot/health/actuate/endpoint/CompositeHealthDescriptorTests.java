/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

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
	@SuppressWarnings("NullAway") // Test null check
	void createWhenApiVersionIsNullThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new CompositeHealthDescriptor(null, Status.UP, Collections.emptyMap()))
			.withMessage("'apiVersion' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
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
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
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
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"details":{"db1":{"status":"UP"},"db2":{"details":{"a":"b"},"status":"DOWN"}},"status":"UP"}""");
	}

}
