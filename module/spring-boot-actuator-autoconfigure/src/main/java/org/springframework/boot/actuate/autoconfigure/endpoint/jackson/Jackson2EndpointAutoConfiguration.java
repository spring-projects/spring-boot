/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.jackson;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Endpoint Jackson 2 support.
 *
 * @author Phillip Webb
 * @since 3.0.0
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3.
 */
@AutoConfiguration
@ConditionalOnClass({ ObjectMapper.class, org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.class })
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public final class Jackson2EndpointAutoConfiguration {

	@Bean
	@ConditionalOnBooleanProperty(name = "management.endpoints.jackson2.isolated-object-mapper", matchIfMissing = true)
	org.springframework.boot.actuate.endpoint.jackson.EndpointJackson2ObjectMapper jackson2EndpointJsonMapper() {
		ObjectMapper objectMapper = org.springframework.http.converter.json.Jackson2ObjectMapperBuilder.json()
			.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
					SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
			.serializationInclusion(Include.NON_NULL)
			.build();
		return () -> objectMapper;
	}

}
