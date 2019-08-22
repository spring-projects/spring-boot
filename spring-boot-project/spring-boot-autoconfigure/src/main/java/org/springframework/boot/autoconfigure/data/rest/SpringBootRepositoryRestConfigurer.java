/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * A {@code RepositoryRestConfigurer} that applies configuration items from the
 * {@code spring.data.rest} namespace to Spring Data REST. Also, if a
 * {@link Jackson2ObjectMapperBuilder} is available, it is used to configure Spring Data
 * REST's {@link ObjectMapper ObjectMappers}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@Order(0)
class SpringBootRepositoryRestConfigurer implements RepositoryRestConfigurer {

	@Autowired(required = false)
	private Jackson2ObjectMapperBuilder objectMapperBuilder;

	@Autowired
	private RepositoryRestProperties properties;

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
		this.properties.applyTo(config);
	}

	@Override
	public void configureJacksonObjectMapper(ObjectMapper objectMapper) {
		if (this.objectMapperBuilder != null) {
			this.objectMapperBuilder.configure(objectMapper);
		}
	}

}
