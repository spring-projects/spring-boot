/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Factory for an {@code ObjectMapper} instance to be shared within Actuator
 * infrastructure.
 *
 * <p>
 * The goal is to have a Jackson configuration separate from the rest of the application
 * to keep a consistent serialization behavior between applications.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class ActuatorJsonMapperProvider {

	private ObjectMapper objectMapper;

	public ObjectMapper getInstance() {
		if (this.objectMapper == null) {
			Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
			builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
			builder.featuresToDisable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS);
			this.objectMapper = builder.build();
		}
		return this.objectMapper;
	}

}
