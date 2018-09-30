/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.web.documentation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.reactive.WebFluxEndpointManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.StringUtils;

import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

/**
 * Abstract base class for tests that generate endpoint documentation using Spring REST
 * Docs.
 *
 * @author Andy Wilkinson
 */
@TestPropertySource(properties = { "spring.jackson.serialization.indent_output=true",
		"management.endpoints.web.exposure.include=*",
		"spring.jackson.default-property-inclusion=non_null" })
public abstract class AbstractEndpointDocumentationTests {

	protected String describeEnumValues(Class<? extends Enum<?>> enumType) {
		return StringUtils
				.collectionToDelimitedString(Stream.of(enumType.getEnumConstants())
						.map((constant) -> "`" + constant.name() + "`")
						.collect(Collectors.toList()), ", ");
	}

	protected OperationPreprocessor limit(String... keys) {
		return limit((candidate) -> true, keys);
	}

	@SuppressWarnings("unchecked")
	protected <T> OperationPreprocessor limit(Predicate<T> filter, String... keys) {
		return new ContentModifyingOperationPreprocessor((content, mediaType) -> {
			ObjectMapper objectMapper = new ObjectMapper()
					.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Map<String, Object> payload = objectMapper.readValue(content, Map.class);
				Object target = payload;
				Map<Object, Object> parent = null;
				for (String key : keys) {
					if (target instanceof Map) {
						parent = (Map<Object, Object>) target;
						target = parent.get(key);
					}
					else {
						throw new IllegalStateException();
					}
				}
				if (target instanceof Map) {
					parent.put(keys[keys.length - 1],
							select((Map<String, Object>) target, filter));
				}
				else {
					parent.put(keys[keys.length - 1],
							select((List<Object>) target, filter));
				}
				return objectMapper.writeValueAsBytes(payload);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	protected FieldDescriptor parentIdField() {
		return fieldWithPath("contexts.*.parentId")
				.description("Id of the parent application context, if any.").optional()
				.type(JsonFieldType.STRING);
	}

	@SuppressWarnings("unchecked")
	private <T> Map<String, Object> select(Map<String, Object> candidates,
			Predicate<T> filter) {
		Map<String, Object> selected = new HashMap<>();
		candidates.entrySet().stream().filter((candidate) -> filter.test((T) candidate))
				.limit(3)
				.forEach((entry) -> selected.put(entry.getKey(), entry.getValue()));
		return selected;
	}

	@SuppressWarnings("unchecked")
	private <T> List<Object> select(List<Object> candidates, Predicate<T> filter) {
		return candidates.stream().filter((candidate) -> filter.test((T) candidate))
				.limit(3).collect(Collectors.toList());
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class,
			WebMvcEndpointManagementContextConfiguration.class,
			WebFluxEndpointManagementContextConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class, WebFluxAutoConfiguration.class,
			HttpHandlerAutoConfiguration.class })
	static class BaseDocumentationConfiguration {

	}

}
