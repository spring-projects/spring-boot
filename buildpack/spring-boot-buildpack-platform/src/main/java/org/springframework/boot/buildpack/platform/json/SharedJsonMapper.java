/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.json;

import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Provides access to a shared pre-configured {@link JsonMapper}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class SharedJsonMapper {

	private static final JsonMapper INSTANCE;

	static {
		INSTANCE = JsonMapper.builder()
			.enable(SerializationFeature.INDENT_OUTPUT)
			.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
			.disable(JsonWriteFeature.ESCAPE_FORWARD_SLASHES)
			.propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
			.build();
	}

	private SharedJsonMapper() {
	}

	public static JsonMapper get() {
		return INSTANCE;
	}

}
