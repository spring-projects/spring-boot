/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.UUID;

import javax.sql.DataSource;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class MetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(RegistryConfiguration.class)
			.withConfiguration(AutoConfigurations.of(MetricsAutoConfiguration.class));

	@Test
	public void autoConfiguredDataSourceIsInstrumented() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.metrics.use-global-registry=false")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("data.source.max.connections")
							.tags("name", "dataSource").meter()).isPresent();
				});
	}

	@Test
	public void autoConfiguredDataSourceWithCustomMetricName() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.metrics.jdbc.datasource-metric-name=custom.name",
						"spring.metrics.use-global-registry=false")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("custom.name.max.connections")
							.tags("name", "dataSource").meter()).isPresent();
				});
	}

	@Test
	public void dataSourceInstrumentationCanBeDisabled() {
		this.contextRunner
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"spring.metrics.jdbc.instrument-datasource=false",
						"spring.metrics.use-global-registry=false")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection().getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("custom.name.max.connections")
							.tags("name", "dataSource").meter()).isNotPresent();
				});
	}

	@Test
	public void allDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(TwoDataSourcesConfiguration.class)
				.withConfiguration(
						AutoConfigurations.of(DataSourceAutoConfiguration.class))
				.withPropertyValues("metrics.use-global-registry=false")
				.run((context) -> {
					context.getBean("firstDataSource", DataSource.class).getConnection()
							.getMetaData();
					context.getBean("secondOne", DataSource.class).getConnection()
							.getMetaData();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("data.source.max.connections")
							.tags("name", "first").meter()).isPresent();
					assertThat(registry.find("data.source.max.connections")
							.tags("name", "secondOne").meter()).isPresent();
				});
	}

	@Configuration
	static class RegistryConfiguration {

		@Bean
		public MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration
	static class TwoDataSourcesConfiguration {

		@Bean
		public DataSource firstDataSource() {
			return createDataSource();
		}

		@Bean
		public DataSource secondOne() {
			return createDataSource();
		}

		private DataSource createDataSource() {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).build();
		}

	}

}
