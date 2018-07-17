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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcMetricsAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class WebMvcMetricsAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(WebMvcMetricsAutoConfiguration.class));

	@Test
	public void backsOffWhenMeterRegistryIsMissing() {
		this.contextRunner.run((context) -> assertThat(context)
				.doesNotHaveBean(WebMvcMetricsAutoConfiguration.class));
	}

	@Test
	public void definesTagsProviderAndFilterWhenMeterRegistryIsPresent() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(DefaultWebMvcTagsProvider.class);
					assertThat(context).hasSingleBean(FilterRegistrationBean.class);
					assertThat(context.getBean(FilterRegistrationBean.class).getFilter())
							.isInstanceOf(WebMvcMetricsFilter.class);
				});
	}

	@Test
	public void tagsProviderBacksOff() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class,
				TagsProviderConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(DefaultWebMvcTagsProvider.class);
					assertThat(context).hasSingleBean(TestWebMvcTagsProvider.class);
				});
	}

	@Test
	public void filterRegistrationHasExpectedDispatcherTypesAndOrder() {
		this.contextRunner.withUserConfiguration(MeterRegistryConfiguration.class)
				.run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat(registration).hasFieldOrPropertyWithValue(
							"dispatcherTypes",
							EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
					assertThat(registration.getOrder())
							.isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
				});
	}

	@Configuration
	static class MeterRegistryConfiguration {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration
	static class TagsProviderConfiguration {

		@Bean
		public TestWebMvcTagsProvider tagsProvider() {
			return new TestWebMvcTagsProvider();
		}

	}

	private static final class TestWebMvcTagsProvider implements WebMvcTagsProvider {

		@Override
		public Iterable<Tag> getTags(HttpServletRequest request,
				HttpServletResponse response, Object handler, Throwable exception) {
			return Collections.emptyList();
		}

		@Override
		public Iterable<Tag> getLongRequestTags(HttpServletRequest request,
				Object handler) {
			return Collections.emptyList();
		}

	}

}
