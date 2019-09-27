/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.web.servlet;

import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.metrics.web.TestController;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.LongTaskTimingHandlerInterceptor;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Dmytro Nosan
 * @author Tadaya Tsuyukubo
 */
@ExtendWith(OutputCaptureExtension.class)
class WebMvcMetricsAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(WebMvcMetricsAutoConfiguration.class));

	@Test
	void backsOffWhenMeterRegistryIsMissing() {
		new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(WebMvcMetricsAutoConfiguration.class))
				.run((context) -> assertThat(context).doesNotHaveBean(WebMvcTagsProvider.class));
	}

	@Test
	void definesTagsProviderAndFilterWhenMeterRegistryIsPresent() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DefaultWebMvcTagsProvider.class);
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
					.isInstanceOf(WebMvcMetricsFilter.class);
		});
	}

	@Test
	void tagsProviderBacksOff() {
		this.contextRunner.withUserConfiguration(TagsProviderConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DefaultWebMvcTagsProvider.class);
			assertThat(context).hasSingleBean(TestWebMvcTagsProvider.class);
		});
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
	void afterMaxUrisReachedFurtherUrisAreDenied(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, WebMvcAutoConfiguration.class))
				.withPropertyValues("management.metrics.web.server.max-uri-tags=2").run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.server.requests").meters()).hasSize(2);
					assertThat(output).contains("Reached the maximum number of URI tags for 'http.server.requests'");
				});
	}

	@Test
	void shouldNotDenyNorLogIfMaxUrisIsNotReached(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(TestController.class)
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, WebMvcAutoConfiguration.class))
				.withPropertyValues("management.metrics.web.server.max-uri-tags=5").run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					assertThat(registry.get("http.server.requests").meters()).hasSize(3);
					assertThat(output)
							.doesNotContain("Reached the maximum number of URI tags for 'http.server.requests'");
				});
	}

	@Test
	void autoTimeRequestsCanBeConfigured() {
		this.contextRunner.withUserConfiguration(TestController.class)
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, WebMvcAutoConfiguration.class))
				.withPropertyValues("management.metrics.web.server.request.autotime.enabled=true",
						"management.metrics.web.server.request.autotime.percentiles=0.5,0.7",
						"management.metrics.web.server.request.autotime.percentiles-histogram=true")
				.run((context) -> {
					MeterRegistry registry = getInitializedMeterRegistry(context);
					Timer timer = registry.get("http.server.requests").timer();
					HistogramSnapshot snapshot = timer.takeSnapshot();
					assertThat(snapshot.percentileValues()).hasSize(2);
					assertThat(snapshot.percentileValues()[0].percentile()).isEqualTo(0.5);
					assertThat(snapshot.percentileValues()[1].percentile()).isEqualTo(0.7);
				});
	}

	@Test
	@SuppressWarnings("rawtypes")
	void longTaskTimingInterceptorIsRegistered() {
		this.contextRunner.withUserConfiguration(TestController.class)
				.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class, WebMvcAutoConfiguration.class))
				.run((context) -> assertThat(context.getBean(RequestMappingHandlerMapping.class))
						.extracting("interceptors").asList().extracting((item) -> (Class) item.getClass())
						.contains(LongTaskTimingHandlerInterceptor.class));
	}

	private MeterRegistry getInitializedMeterRegistry(AssertableWebApplicationContext context) throws Exception {
		assertThat(context).hasSingleBean(FilterRegistrationBean.class);
		Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
		assertThat(filter).isInstanceOf(WebMvcMetricsFilter.class);
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(MockMvcRequestBuilders.get("/test" + i)).andExpect(status().isOk());
		}
		return context.getBean(MeterRegistry.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class TagsProviderConfiguration {

		@Bean
		TestWebMvcTagsProvider tagsProvider() {
			return new TestWebMvcTagsProvider();
		}

	}

	private static final class TestWebMvcTagsProvider implements WebMvcTagsProvider {

		@Override
		public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler,
				Throwable exception) {
			return Collections.emptyList();
		}

		@Override
		public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
			return Collections.emptyList();
		}

	}

}
