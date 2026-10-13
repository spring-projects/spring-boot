/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.health.contributor.Health;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IndicatedHealthDescriptor}.
 *
 * @author Phillip Webb
 */
class IndicatedHealthDescriptorTests {

	@Test
	void serializeWithJacksonReturnsValidJson() throws Exception {
		IndicatedHealthDescriptor descriptor = new IndicatedHealthDescriptor(
				Health.outOfService().withDetail("spring", "boot").build());
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"details":{"spring":"boot"},"status":"OUT_OF_SERVICE"}""");
	}

	@Test
	void serializeWithJacksonWhenEmptyDetailsReturnsValidJson() throws Exception {
		IndicatedHealthDescriptor descriptor = new IndicatedHealthDescriptor(Health.outOfService().build());
		JsonMapper mapper = JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
		String json = mapper.writeValueAsString(descriptor);
		assertThat(json).isEqualTo("""
				{"status":"OUT_OF_SERVICE"}""");
	}

}
