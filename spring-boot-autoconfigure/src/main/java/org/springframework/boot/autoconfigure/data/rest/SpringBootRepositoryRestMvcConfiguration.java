/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A specialized {@link RepositoryRestMvcConfiguration} that applies configuration items
 * from the {@code spring.data.rest} namespace. Also configures Jackson if it's available
 * <p>
 * Favor an extension of this class instead of extending directly from
 * {@link RepositoryRestMvcConfiguration}.
 *
 * @author Stephane Nicoll
 * @since 1.2.2
 */
@Configuration
public class SpringBootRepositoryRestMvcConfiguration extends
		RepositoryRestMvcConfiguration {

	@Autowired(required = false)
	private Jackson2ObjectMapperBuilder objectMapperBuilder;

	@Bean
	@ConfigurationProperties(prefix = "spring.data.rest")
	@Override
	public RepositoryRestConfiguration config() {
		return super.config();
	}

	@Override
	protected void configureJacksonObjectMapper(ObjectMapper objectMapper) {
		if (this.objectMapperBuilder != null) {
			this.objectMapperBuilder.configure(objectMapper);
		}
	}

}
