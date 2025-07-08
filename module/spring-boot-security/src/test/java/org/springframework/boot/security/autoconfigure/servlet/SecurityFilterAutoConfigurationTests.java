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

package org.springframework.boot.security.autoconfigure.servlet;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfigurationEarlyInitializationTests.ConverterBean;
import org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfigurationEarlyInitializationTests.DeserializerBean;
import org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfigurationEarlyInitializationTests.ExampleController;
import org.springframework.boot.security.autoconfigure.servlet.SecurityFilterAutoConfigurationEarlyInitializationTests.JacksonModuleBean;
import org.springframework.boot.servlet.filter.OrderedRequestContextFilter;
import org.springframework.boot.web.context.servlet.AnnotationConfigServletWebApplicationContext;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SecurityFilterAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
class SecurityFilterAutoConfigurationTests {

	@Test
	void filterAutoConfigurationWorksWithoutSecurityAutoConfiguration() {
		try (AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext()) {
			context.setServletContext(new MockServletContext());
			context.register(Config.class);
			context.refresh();
		}
	}

	@Test
	void filterIsOrderedShortlyAfterRequestContextFilter() {
		try (AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext()) {
			context.setServletContext(new MockServletContext());
			context.register(SecurityAutoConfiguration.class);
			context.register(Config.class);
			context.refresh();
			int securityFilterOrder = context.getBean(DelegatingFilterProxyRegistrationBean.class).getOrder();
			int requestContextFilterOrder = new OrderedRequestContextFilter().getOrder();
			assertThat(securityFilterOrder).isGreaterThan(requestContextFilterOrder)
				.isCloseTo(requestContextFilterOrder, Assertions.within(5));
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ DeserializerBean.class, JacksonModuleBean.class, ExampleController.class, ConverterBean.class })
	@ImportAutoConfiguration({ WebMvcAutoConfiguration.class, JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			SecurityFilterAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	static class Config {

	}

}
