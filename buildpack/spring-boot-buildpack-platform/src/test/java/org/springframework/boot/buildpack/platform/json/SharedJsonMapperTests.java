/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.json;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SharedJsonMapper}.
 *
 * @author Phillip Webb
 */
class SharedJsonMapperTests {

	@Test
	void getReturnsConfiguredJsonMapper() {
		JsonMapper mapper = SharedJsonMapper.get();
		assertThat(mapper).isNotNull();
		assertThat(
				SerializationFeature.INDENT_OUTPUT.enabledIn(mapper.serializationConfig().getSerializationFeatures()))
			.isTrue();
		assertThat(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
			.enabledIn(mapper.deserializationConfig().getDeserializationFeatures())).isFalse();
		assertThat(mapper.serializationConfig().getPropertyNamingStrategy())
			.isEqualTo(PropertyNamingStrategies.LOWER_CAMEL_CASE);
		assertThat(mapper.deserializationConfig().getPropertyNamingStrategy())
			.isEqualTo(PropertyNamingStrategies.LOWER_CAMEL_CASE);
	}

}
