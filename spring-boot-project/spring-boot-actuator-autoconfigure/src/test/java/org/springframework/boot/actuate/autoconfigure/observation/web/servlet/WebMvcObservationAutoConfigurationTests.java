/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.observation.web.servlet;

import java.util.EnumSet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.metrics.web.TestController;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.server.observation.DefaultServerRequestObservationConvention;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.ServerHttpObservationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcObservationAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 * @author Tadaya Tsuyukubo
 * @author Madhura Bhave
 * @author Chanhyeong LEE
 */
@ExtendWith(OutputCaptureExtension.class)
class WebMvcObservationAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(ObservationAutoConfiguration.class))
		.withConfiguration(AutoConfigurations.of(WebMvcObservationAutoConfiguration.class));

	@Test
	void backsOffWhenMeterRegistryIsMissing() {
		new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(WebMvcObservationAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	void definesFilterWhenRegistryIsPresent() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
				.isInstanceOf(ServerHttpObservationFilter.class);
		});
	}

	@Test
	void customConventionWhenPresent() {
		this.contextRunner.withUserConfiguration(CustomConventionConfiguration.class)
			.run((context) -> assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
				.extracting("observationConvention")
				.isInstanceOf(CustomConvention.class));
	}

	@Test
	void filterRegistrationHasExpectedDispatcherTypesAndOrder() {
		this.contextRunner.run((context) -> {
			FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration).hasFieldOrPropertyWithValue("dispatcherTypes",
					EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
			assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
		});
	}

	@Test
	void filterRegistrationBacksOffWithAnotherServerHttpObservationFilterRegistration() {
		this.contextRunner.withUserConfiguration(TestServerHttpObservationFilterRegistrationConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(FilterRegistrationBean.class);
				assertThat(context.getBean(FilterRegistrationBean.class))
					.isSameAs(context.getBean("testServerHttpObservationFilter"));
			});
	}

	@Test
	void filterRegistrationBacksOffWithAnotherServerHttpObservationFilter() {
		this.contextRunner.withUserConfiguration(TestServerHttpObservationFilterConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class)
				.hasSingleBean(ServerHttpObservationFilter.class));
	}

	@Test
	void filterRegistrationDoesNotBackOffWithOtherFilterRegistration() {
		this.contextRunner.withUserConfiguration(TestFilterRegistrationConfiguration.class)
			.run((context) -> assertThat(context).hasBean("testFilter").hasBean("webMvcObservationFilter"));
	}

	@Test
	void filterRegistrationDoesNotBackOffWithOtherFilter() {
		this.contextRunner.withUserConfiguration(TestFilterConfiguration.class)
			.run((context) -> assertThat(context).hasBean("testFilter").hasBean("webMvcObservationFilter"));
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebMvcAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=2")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("http.server.requests").meters()).hasSizeLessThanOrEqualTo(2);
				assertThat(output).contains("Reached the maximum number of URI tags for 'http.server.requests'");
			});
	}

	@Test
	void afterMaxUrisReachedFurtherUrisAreDeniedWhenUsingCustomObservationName(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebMvcAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=2",
					"management.observations.http.server.requests.name=my.http.server.requests")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("my.http.server.requests").meters()).hasSizeLessThanOrEqualTo(2);
				assertThat(output).contains("Reached the maximum number of URI tags for 'my.http.server.requests'");
			});
	}

	@Test
	void shouldNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, ObservationAutoConfiguration.class,
					WebMvcAutoConfiguration.class))
			.withPropertyValues("management.metrics.web.server.max-uri-tags=5")
			.run((context) -> {
				MeterRegistry registry = getInitializedMeterRegistry(context);
				assertThat(registry.get("http.server.requests").meters()).hasSize(3);
				assertThat(output).doesNotContain("Reached the maximum number of URI tags for 'http.server.requests'");
			});
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableWebApplicationContext context) throws Exception {
		return getInitializedMeterRegistry(context, "/test0", "/test1", "/test2");
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableWebApplicationContext context, String... urls)
			throws Exception {
		assertThat(context).hasSingleBean(FilterRegistrationBean.class);
		Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
		assertThat(filter).isInstanceOf(ServerHttpObservationFilter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
		for (String url : urls) {
			mockMvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isOk());
		}
		return context.getBean(MeterRegistry.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TestServerHttpObservationFilterRegistrationConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		FilterRegistrationBean<ServerHttpObservationFilter> testServerHttpObservationFilter() {
			return mock(FilterRegistrationBean.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestServerHttpObservationFilterConfiguration {

		@Bean
		ServerHttpObservationFilter testServerHttpObservationFilter() {
			return new ServerHttpObservationFilter(TestObservationRegistry.create());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestFilterRegistrationConfiguration {

		@Bean
		@SuppressWarnings("unchecked")
		FilterRegistrationBean<Filter> testFilter() {
			return mock(FilterRegistrationBean.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestFilterConfiguration {

		@Bean
		Filter testFilter() {
			return mock(Filter.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConventionConfiguration {

		@Bean
		CustomConvention customConvention() {
			return new CustomConvention();
		}

	}

	static class CustomConvention extends DefaultServerRequestObservationConvention {

	}

}
