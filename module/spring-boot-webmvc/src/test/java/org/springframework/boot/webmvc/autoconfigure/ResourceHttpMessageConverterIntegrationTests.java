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

package org.springframework.boot.webmvc.autoconfigure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceHttpMessageConverterIntegrationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class))
		.withUserConfiguration(ResourceControllerConfiguration.class);

	@Test
	void responseEntityResourceUsesResourceHttpMessageConverter() throws IOException {
		Path file = Files.createTempFile("spring-boot", ".bin");
		try {
			byte[] content = "test-content".getBytes(StandardCharsets.UTF_8);
			Files.write(file, content);
			this.contextRunner.withPropertyValues("test.file=" + file.toAbsolutePath()).run((context) -> {
				MockMvcTester mvc = MockMvcTester.from(context);
				MvcTestResult result = mvc.get().uri("/file").exchange();
				assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
				assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(content);
				assertThat(Files.readAllBytes(file)).isEqualTo(content);
			});
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	@Test
	void responseEntityResourcePrefersResourceConverterOverCustomJsonConverter() throws IOException {
		Path file = Files.createTempFile("spring-boot", ".bin");
		try {
			byte[] content = "custom-json-content".getBytes(StandardCharsets.UTF_8);
			Files.write(file, content);
			this.contextRunner.withUserConfiguration(CustomJsonConverterConfiguration.class)
				.withPropertyValues("test.file=" + file.toAbsolutePath())
				.run((context) -> {
					MockMvcTester mvc = MockMvcTester.from(context);
					MvcTestResult result = mvc.get().uri("/file").exchange();
					assertThat(result.getResponse().getContentType())
						.isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
					assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(content);
				});
		}
		finally {
			Files.deleteIfExists(file);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceControllerConfiguration {

		@Bean
		Resource fileResource(@Value("${test.file}") String file) {
			return new FileSystemResource(file);
		}

		@RestController
		static class FileController {

			private final Resource resource;

			FileController(Resource resource) {
				this.resource = resource;
			}

			@GetMapping(value = "/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
			ResponseEntity<Resource> file() {
				return ResponseEntity.ok()
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
					.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.bin\"")
					.body(this.resource);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJsonConverterConfiguration {

		@Bean
		JsonMapper jsonMapper() {
			return new JsonMapper();
		}

		@Bean
		JacksonJsonHttpMessageConverter customJsonHttpMessageConverter(JsonMapper jsonMapper) {
			JacksonJsonHttpMessageConverter converter = new JacksonJsonHttpMessageConverter(jsonMapper);
			converter.setSupportedMediaTypes(
					List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
			return converter;
		}

	}

}
