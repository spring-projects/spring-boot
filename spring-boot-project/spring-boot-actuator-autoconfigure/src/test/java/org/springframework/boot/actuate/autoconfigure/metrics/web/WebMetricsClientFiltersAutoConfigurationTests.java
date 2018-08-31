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

package org.springframework.boot.actuate.autoconfigure.metrics.web;

import io.micrometer.core.instrument.config.MeterFilter;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.RestTemplateMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.reactive.WebClientMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMetricsClientFiltersAutoConfiguration}.
 *
 * @author Dmytro Nosan
 */
public class WebMetricsClientFiltersAutoConfigurationTests {

	private static final String FILTER_NAME = "webMetricsClientUriTagFilter";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class,
					SimpleMetricsExportAutoConfiguration.class,
					RestTemplateAutoConfiguration.class, WebClientAutoConfiguration.class,
					RestTemplateMetricsAutoConfiguration.class,
					WebClientMetricsAutoConfiguration.class,
					WebMetricsClientFiltersAutoConfiguration.class));

	@Test
	public void filterShouldNotBeRegisteredIfWebClientAndRestTemplateAreNotAvailable() {
		FilteredClassLoader classLoader = new FilteredClassLoader(WebClient.class,
				RestTemplate.class);
		this.contextRunner.withClassLoader(classLoader).run(
				(context) -> assertThat(context.getBeanNamesForType(MeterFilter.class))
						.doesNotContain(FILTER_NAME));
	}

	@Test
	public void filterShouldBeRegisteredIfRestTemplateIsAvailable() {
		FilteredClassLoader classLoader = new FilteredClassLoader(WebClient.class);
		this.contextRunner.withClassLoader(classLoader).run(
				(context) -> assertThat(context.getBeanNamesForType(MeterFilter.class))
						.contains(FILTER_NAME));
	}

	@Test
	public void filterShouldBeRegisteredIfWebClientIsAvailable() {
		FilteredClassLoader classLoader = new FilteredClassLoader(RestTemplate.class);
		this.contextRunner.withClassLoader(classLoader).run(
				(context) -> assertThat(context.getBeanNamesForType(MeterFilter.class))
						.contains(FILTER_NAME));
	}

}
