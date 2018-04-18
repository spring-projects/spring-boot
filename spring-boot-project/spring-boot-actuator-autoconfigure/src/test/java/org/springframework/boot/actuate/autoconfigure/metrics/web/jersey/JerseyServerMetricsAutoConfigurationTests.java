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

package org.springframework.boot.actuate.autoconfigure.metrics.web.jersey;

import io.micrometer.core.instrument.Tag;
import io.micrometer.jersey2.server.DefaultJerseyTagsProvider;
import io.micrometer.jersey2.server.JerseyTagsProvider;
import io.micrometer.jersey2.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyServerMetricsAutoConfiguration}.
 *
 * @author Michael Simons
 */
public class JerseyServerMetricsAutoConfigurationTests {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple())
			.withConfiguration(
					AutoConfigurations.of(JerseyServerMetricsAutoConfiguration.class)
			);

	private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					JerseyAutoConfiguration.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class,
					JerseyServerMetricsAutoConfiguration.class)
			)
			.withUserConfiguration(JerseyConfig.class);

	@Test
	public void shouldOnlyBeActiveInWebApplicationContext() {
		this.contextRunner.run(ctx -> assertThat(ctx).doesNotHaveBean(ResourceConfigCustomizer.class));
	}

	@Test
	public void shouldProvideALlNecessaryBeans() {
		this.webContextRunner.run(ctx -> assertThat(ctx)
				.hasSingleBean(DefaultJerseyTagsProvider.class)
				.hasSingleBean(ResourceConfigCustomizer.class)
		);
	}

	@Test
	public void shouldHonorExistingTagProvider() {
		this.webContextRunner
				.withUserConfiguration(JerseyServerMetricConfig.class)
				.run(ctx -> assertThat(ctx).hasSingleBean(ATagsProvider.class));
	}

	@Test
	public void doesNotFailWithoutJersey2Metrics() {
		this.webContextRunner
				.withClassLoader(new FilteredClassLoader(MetricsApplicationEventListener.class))
				.run(ctx -> assertThat(ctx).doesNotHaveBean(ResourceConfigCustomizer.class));
	}

	static class JerseyConfig {
		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}
	}

	static class JerseyServerMetricConfig {
		@Bean
		JerseyTagsProvider jerseyTagsProvider() {
			return new ATagsProvider();
		}
	}

	static class ATagsProvider implements JerseyTagsProvider {

		@Override
		public Iterable<Tag> httpRequestTags(RequestEvent requestEvent) {
			return null;
		}

		@Override
		public Iterable<Tag> httpLongRequestTags(RequestEvent requestEvent) {
			return null;
		}
	}
}
