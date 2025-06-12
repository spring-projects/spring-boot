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

package org.springframework.boot.actuate.autoconfigure.endpoint.jackson;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.boot.actuate.endpoint.jackson.EndpointObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.util.ClassUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Endpoint Jackson support.
 *
 * @author Phillip Webb
 * @since 3.0.0
 */
@AutoConfiguration
@SuppressWarnings("removal")
public class JacksonEndpointAutoConfiguration {

	private static final String CONTRIBUTED_HEALTH = "org.springframework.boot.health.contributor.ContributedHealth";

	@Bean
	@ConditionalOnBooleanProperty(name = "management.endpoints.jackson.isolated-object-mapper", matchIfMissing = true)
	@ConditionalOnClass({ ObjectMapper.class, Jackson2ObjectMapperBuilder.class })
	public EndpointObjectMapper endpointObjectMapper() {
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
			.featuresToEnable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
			.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
					SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
			.serializationInclusion(Include.NON_NULL)
			.build();
		Set<Class<?>> supportedTypes = new HashSet<>(EndpointObjectMapper.DEFAULT_SUPPORTED_TYPES);
		if (ClassUtils.isPresent(CONTRIBUTED_HEALTH, null)) {
			supportedTypes.add(ClassUtils.resolveClassName(CONTRIBUTED_HEALTH, null));
		}
		return new EndpointObjectMapper() {

			@Override
			public ObjectMapper get() {
				return objectMapper;
			}

			@Override
			public Set<Class<?>> getSupportedTypes() {
				return supportedTypes;
			}

		};

	}

}
