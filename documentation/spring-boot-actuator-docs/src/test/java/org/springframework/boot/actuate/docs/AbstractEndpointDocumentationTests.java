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

package org.springframework.boot.actuate.docs;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.jackson.JacksonEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.docs.AbstractEndpointDocumentationTests.BaseDocumentationConfiguration;
import org.springframework.boot.actuate.endpoint.jackson.EndpointJsonMapper;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.HttpHandlerAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.WebFluxAutoConfiguration;
import org.springframework.boot.webflux.autoconfigure.actuate.web.WebFluxEndpointManagementContextConfiguration;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.actuate.web.WebMvcEndpointManagementContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
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
@TestPropertySource(properties = { "management.endpoints.web.exposure.include=*" })
@Import(BaseDocumentationConfiguration.class)
public abstract class AbstractEndpointDocumentationTests {

	protected static String describeEnumValues(Class<? extends Enum<?>> enumType) {
		return StringUtils.collectionToDelimitedString(
				Stream.of(enumType.getEnumConstants()).map((constant) -> "`" + constant.name() + "`").toList(), ", ");
	}

	protected OperationPreprocessor limit(String... keys) {
		return limit((candidate) -> true, keys);
	}

	@SuppressWarnings("unchecked")
	protected <T> OperationPreprocessor limit(Predicate<T> filter, String... keys) {
		return new ContentModifyingOperationPreprocessor((content, mediaType) -> {
			JsonMapper jsonMapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
			Map<String, Object> payload = jsonMapper.readValue(content, Map.class);
			Object target = payload;
			Map<Object, Object> parent = null;
			for (String key : keys) {
				if (!(target instanceof Map)) {
					throw new IllegalStateException();
				}
				parent = (Map<Object, Object>) target;
				target = parent.get(key);
			}
			if (target instanceof Map) {
				parent.put(keys[keys.length - 1], select((Map<String, Object>) target, filter));
			}
			else {
				parent.put(keys[keys.length - 1], select((List<Object>) target, filter));
			}
			return jsonMapper.writeValueAsBytes(payload);
		});
	}

	protected FieldDescriptor parentIdField() {
		return fieldWithPath("contexts.*.parentId").description("Id of the parent application context, if any.")
			.optional()
			.type(JsonFieldType.STRING);
	}

	@SuppressWarnings("unchecked")
	private <T> Map<String, Object> select(Map<String, Object> candidates, Predicate<T> filter) {
		Map<String, Object> selected = new HashMap<>();
		candidates.entrySet()
			.stream()
			.filter((candidate) -> filter.test((T) candidate))
			.limit(3)
			.forEach((entry) -> selected.put(entry.getKey(), entry.getValue()));
		return selected;
	}

	@SuppressWarnings("unchecked")
	private <T> List<Object> select(List<Object> candidates, Predicate<T> filter) {
		return candidates.stream().filter((candidate) -> filter.test((T) candidate)).limit(3).toList();
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class, WebMvcEndpointManagementContextConfiguration.class,
			WebFluxEndpointManagementContextConfiguration.class, PropertyPlaceholderAutoConfiguration.class,
			WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
			JacksonEndpointAutoConfiguration.class })
	static class BaseDocumentationConfiguration {

		@Bean
		static BeanPostProcessor endpointJsonMapperBeanPostProcessor() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof EndpointJsonMapper) {
						return (EndpointJsonMapper) () -> ((EndpointJsonMapper) bean).get()
							.rebuild()
							.enable(SerializationFeature.INDENT_OUTPUT)
							.build();
					}
					return bean;
				}

			};
		}

	}

}
