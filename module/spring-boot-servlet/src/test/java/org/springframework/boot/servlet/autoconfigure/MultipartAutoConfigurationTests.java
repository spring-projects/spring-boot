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

package org.springframework.boot.servlet.autoconfigure;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MultipartAutoConfiguration}. Tests an empty configuration, no
 * multipart configuration, and a multipart configuration (with both Jetty and Tomcat).
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Josh Long
 * @author Ivan Sopov
 * @author Toshiaki Maki
 * @author Yanming Zhou
 * @author Hans Hosea Schaefer
 */
@DirtiesUrlFactories
class MultipartAutoConfigurationTests {

	@Test
	void webServerWithMultipartConfigDisabled() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues.of("spring.servlet.multipart.enabled=false").applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeansOfType(MultipartConfigElement.class)).hasSize(0);
			assertThatException().isThrownBy(() -> context.getBean(MultipartProperties.class));
		}
	}

	@Test
	void webServerWithMultipartConfigEnabled() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues.of("spring.servlet.multipart.enabled=true").applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			assertThat(context.getBeansOfType(MultipartConfigElement.class)).hasSize(1);
			assertThat(context.getBean(MultipartProperties.class)).isNotNull();
		}
	}

	@Test
	void webServerWithMultipartConfigEnabledByDefault() {
		try (AnnotationConfigWebApplicationContext context1 = new AnnotationConfigWebApplicationContext()) {
			context1.register(MultipartAutoConfiguration.class);
			context1.refresh();
			assertThat(context1.getBeansOfType(MultipartConfigElement.class)).hasSize(1);
			assertThat(context1.getBean(MultipartProperties.class)).isNotNull();
		}
	}

	@Test
	void webServerWithCustomMultipartResolver() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			context.register(WebServerWithCustomMultipartResolver.class, MultipartAutoConfiguration.class);
			context.refresh();

			MultipartResolver multipartResolver = context.getBean(MultipartResolver.class);
			assertThat(multipartResolver).isNotInstanceOf(StandardServletMultipartResolver.class);
			assertThat(context.getBeansOfType(MultipartConfigElement.class)).hasSize(1);
		}
	}

	@Test
	void configureResolveLazily() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues.of("spring.servlet.multipart.resolve-lazily=true").applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			StandardServletMultipartResolver multipartResolver = context
				.getBean(StandardServletMultipartResolver.class);
			assertThat(multipartResolver).hasFieldOrPropertyWithValue("resolveLazily", true);
		}
	}

	@Test
	void configureStrictServletCompliance() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues.of("spring.servlet.multipart.strict-servlet-compliance=true").applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			StandardServletMultipartResolver multipartResolver = context
				.getBean(StandardServletMultipartResolver.class);
			assertThat(multipartResolver).hasFieldOrPropertyWithValue("strictServletCompliance", true);
		}
	}

	@Test
	void configureMultipartProperties() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues
				.of("spring.servlet.multipart.max-file-size=2048KB", "spring.servlet.multipart.max-request-size=15MB")
				.applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			final MultipartConfigElement multipartConfigElement = context.getBean(MultipartConfigElement.class);
			assertThat(multipartConfigElement.getMaxFileSize()).isEqualTo(2048 * 1024);
			assertThat(multipartConfigElement.getMaxRequestSize()).isEqualTo(15 * 1024 * 1024);
		}
	}

	@Test
	void configureMultipartPropertiesWithRawLongValues() {
		try (AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext()) {
			TestPropertyValues
				.of("spring.servlet.multipart.max-file-size=512", "spring.servlet.multipart.max-request-size=2048")
				.applyTo(context);
			context.register(MultipartAutoConfiguration.class);
			context.refresh();
			MultipartConfigElement multipartConfigElement = context.getBean(MultipartConfigElement.class);
			assertThat(multipartConfigElement.getMaxFileSize()).isEqualTo(512);
			assertThat(multipartConfigElement.getMaxRequestSize()).isEqualTo(2048);
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class WebServerWithCustomMultipartResolver {

		@Bean
		MultipartResolver multipartResolver() {
			return mock(MultipartResolver.class);
		}

	}

}
