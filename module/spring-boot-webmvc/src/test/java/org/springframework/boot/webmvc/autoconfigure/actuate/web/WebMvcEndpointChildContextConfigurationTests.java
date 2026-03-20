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

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.boot.servlet.filter.OrderedRequestContextFilter;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletPath;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.web.filter.ServerHttpObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcEndpointChildContextConfiguration}.
 *
 * @author Madhura Bhave
 */
class WebMvcEndpointChildContextConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withAllowBeanDefinitionOverriding(true);

	@Test
	void contextShouldConfigureRequestContextFilter() {
		this.contextRunner.withUserConfiguration(WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(OrderedRequestContextFilter.class));
	}

	@Test
	void contextShouldNotConfigureRequestContextFilterWhenPresent() {
		this.contextRunner.withUserConfiguration(ExistingConfig.class, WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RequestContextFilter.class);
				assertThat(context).hasBean("testRequestContextFilter");
			});
	}

	@Test
	void contextShouldNotConfigureRequestContextFilterWhenRequestContextListenerPresent() {
		this.contextRunner
			.withUserConfiguration(RequestContextListenerConfig.class, WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(RequestContextListener.class);
				assertThat(context).doesNotHaveBean(OrderedRequestContextFilter.class);
			});
	}

	@Test
	void contextShouldConfigureDispatcherServletPathWithRootPath() {
		this.contextRunner.withUserConfiguration(WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> assertThat(context.getBean(DispatcherServletPath.class).getPath()).isEqualTo("/"));
	}

	@Test
	void contextShouldConfigureObservationFilterWhenObservationRegistryPresent() {
		this.contextRunner
			.withUserConfiguration(ObservationTestConfig.class, WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(FilterRegistrationBean.class);
				FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
				assertThat(registration.getFilter()).isInstanceOf(ServerHttpObservationFilter.class);
			});
	}

	@Test
	void contextShouldNotConfigureObservationFilterWhenObservationRegistryMissing() {
		this.contextRunner.withUserConfiguration(WebMvcEndpointChildContextConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ServerHttpObservationFilter.class);
			assertThat(context).doesNotHaveBean(FilterRegistrationBean.class);
		});
	}

	@Test
	void contextShouldNotConfigureObservationFilterWhenExistingFilterPresent() {
		this.contextRunner
			.withUserConfiguration(ObservationTestConfig.class, ExistingObservationFilterConfig.class,
					WebMvcEndpointChildContextConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ExistingConfig {

		@Bean
		RequestContextFilter testRequestContextFilter() {
			return new RequestContextFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RequestContextListenerConfig {

		@Bean
		RequestContextListener testRequestContextListener() {
			return new RequestContextListener();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ObservationTestConfig {

		@Bean
		ObservationRegistry observationRegistry() {
			return ObservationRegistry.create();
		}

		@Bean
		ObservationProperties observationProperties() {
			return new ObservationProperties();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ExistingObservationFilterConfig {

		@Bean
		ServerHttpObservationFilter testServerHttpObservationFilter() {
			return new ServerHttpObservationFilter(ObservationRegistry.create());
		}

	}

}
