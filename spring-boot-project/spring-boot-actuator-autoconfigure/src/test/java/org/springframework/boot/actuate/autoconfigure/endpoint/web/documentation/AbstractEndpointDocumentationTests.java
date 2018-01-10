/*
 * Copyright 2012-2017 the original author or authors.
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.servlet.WebMvcEndpointManagementContextConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;

/**
 * Abstract base class for tests that generate endpoint documentation using Spring REST
 * Docs.
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.jackson.serialization.indent_output=true",
		"management.endpoints.web.expose=*" })
public abstract class AbstractEndpointDocumentationTests {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

	protected MockMvc mockMvc;

	@Autowired
	private WebApplicationContext applicationContext;

	@Before
	public void before() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.applicationContext)
				.apply(MockMvcRestDocumentation
						.documentationConfiguration(this.restDocumentation).uris())
				.build();
	}

	protected String describeEnumValues(Class<? extends Enum<?>> enumType) {
		return StringUtils
				.collectionToCommaDelimitedString(Stream.of(enumType.getEnumConstants())
						.map((constant) -> "`" + constant.name() + "`")
						.collect(Collectors.toList()));
	}

	protected OperationPreprocessor limit(String key) {
		return limit(key, (candidate) -> true);
	}

	@SuppressWarnings("unchecked")
	protected <T> OperationPreprocessor limit(String key, Predicate<T> filter) {
		return new ContentModifyingOperationPreprocessor((content, mediaType) -> {
			ObjectMapper objectMapper = new ObjectMapper()
					.enable(SerializationFeature.INDENT_OUTPUT);
			try {
				Map<String, Object> payload = objectMapper.readValue(content, Map.class);
				Object entry = payload.get(key);
				if (entry instanceof Map) {
					payload.put(key, select((Map<String, Object>) entry, filter));
				}
				else {
					payload.put(key, select((List<Object>) entry, filter));
				}
				return objectMapper.writeValueAsBytes(payload);
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		});
	}

	@SuppressWarnings("unchecked")
	private <T> Map<String, Object> select(Map<String, Object> candidates,
			Predicate<T> filter) {
		Map<String, Object> selected = new HashMap<String, Object>();
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
	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, EndpointAutoConfiguration.class,
			WebEndpointAutoConfiguration.class,
			WebMvcEndpointManagementContextConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class BaseDocumentationConfiguration {

	}

}
