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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import org.junit.jupiter.api.Test;
import zipkin2.reporter.urlconnection.URLConnectionSender;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.observation.web.client.HttpClientObservationsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.ApplicationContextAssertProvider;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ZipkinAutoConfiguration} and other related
 * auto-configurations.
 *
 * @author Andy Wilkinson
 */
class ZipkinAutoConfigurationIntegrationTests {

	@Test
	void zipkinsUseOfRestTemplateDoesNotCauseACycle() {
		configure(new WebApplicationContextRunner())
			.withConfiguration(AutoConfigurations.of(RestTemplateAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void zipkinsUseOfWebClientDoesNotCauseACycle() {
		configure(new ReactiveWebApplicationContextRunner())
			.withConfiguration(AutoConfigurations.of(WebClientAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	<SELF extends AbstractApplicationContextRunner<SELF, C, A>, C extends ConfigurableApplicationContext, A extends ApplicationContextAssertProvider<C>> AbstractApplicationContextRunner<SELF, C, A> configure(
			AbstractApplicationContextRunner<SELF, ?, ?> runner) {
		return runner
			.withConfiguration(AutoConfigurations.of(MicrometerTracingAutoConfiguration.class,
					ObservationAutoConfiguration.class, BraveAutoConfiguration.class, ZipkinAutoConfiguration.class,
					HttpClientObservationsAutoConfiguration.class, MetricsAutoConfiguration.class,
					SimpleMetricsExportAutoConfiguration.class))
			.withClassLoader(new FilteredClassLoader(URLConnectionSender.class));
	}

}
