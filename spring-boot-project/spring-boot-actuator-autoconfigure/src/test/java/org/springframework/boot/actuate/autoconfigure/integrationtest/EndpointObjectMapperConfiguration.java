/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.integrationtest;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import org.springframework.boot.actuate.endpoint.jackson.EndpointObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * {@link Configuration @Configuration} that creates an {@link EndpointObjectMapper} that
 * reverses all strings.
 *
 * @author Phillip Webb
 */
@Configuration
class EndpointObjectMapperConfiguration {

	@Bean
	EndpointObjectMapper endpointObjectMapper() {
		SimpleModule module = new SimpleModule();
		module.addSerializer(String.class, new ReverseStringSerializer());
		ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().modules(module).build();
		return () -> objectMapper;
	}

	static class ReverseStringSerializer extends StdScalarSerializer<Object> {

		ReverseStringSerializer() {
			super(String.class, false);
		}

		@Override
		public boolean isEmpty(SerializerProvider prov, Object value) {
			return ((String) value).isEmpty();
		}

		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
			serialize(value, gen);
		}

		@Override
		public final void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider,
				TypeSerializer typeSer) throws IOException {
			serialize(value, gen);
		}

		private void serialize(Object value, JsonGenerator gen) throws IOException {
			StringBuilder builder = new StringBuilder((String) value);
			gen.writeString(builder.reverse().toString());
		}

	}

}
