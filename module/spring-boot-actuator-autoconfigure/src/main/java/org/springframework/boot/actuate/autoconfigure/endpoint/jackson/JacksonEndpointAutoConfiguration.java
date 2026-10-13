/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Endpoint Jackson support.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@AutoConfiguration
@ConditionalOnClass(JsonMapper.class)
public final class JacksonEndpointAutoConfiguration {

	@Bean
	@ConditionalOnBooleanProperty(name = "management.endpoints.jackson.isolated-json-mapper", matchIfMissing = true)
	EndpointJsonMapper endpointJsonMapper() {
		JsonMapper jsonMapper = JsonMapper.builder()
			.changeDefaultPropertyInclusion(
					(value) -> value.withValueInclusion(Include.NON_NULL).withContentInclusion(Include.NON_NULL))
			.build();
		return () -> jsonMapper;
	}

}
