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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HikariDataSourceMetricsAutoConfiguration}.
 *
 * @author Tommy Ludwig
 */
public class HikariDataSourceMetricsAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.with(MetricsRun.simple())
			.withConfiguration(
					AutoConfigurations.of(HikariDataSourceMetricsAutoConfiguration.class,
							DataSourceAutoConfiguration.class))
			.withUserConfiguration(BaseConfiguration.class);

	@Test
	public void autoConfiguredHikariDataSourceIsInstrumented() {
		this.contextRunner
				.withPropertyValues("spring.datasource.generate-unique-name=true")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("hikaricp.connections").meter();
				});
	}

	@Test
	public void hikariDataSourceInstrumentationCanBeDisabled() {
		this.contextRunner
				.withPropertyValues("spring.datasource.generate-unique-name=true",
						"management.metrics.enable.hikaricp=false")
				.run((context) -> {
					context.getBean(DataSource.class).getConnection();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("hikaricp.connections").meter())
							.isNull();
				});
	}

	@Test
	public void allHikariDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(TwoHikariDataSourcesConfiguration.class)
				.run((context) -> {
					context.getBean("firstDataSource", DataSource.class).getConnection();
					context.getBean("secondOne", DataSource.class).getConnection();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					registry.get("hikaricp.connections").tags("pool", "firstDataSource")
							.meter();
					registry.get("hikaricp.connections").tags("pool", "secondOne")
							.meter();
				});
	}

	@Test
	public void someHikariDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(MixedDataSourcesConfiguration.class)
				.run((context) -> {
					context.getBean("firstDataSource", DataSource.class).getConnection();
					context.getBean("secondOne", DataSource.class).getConnection();
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.get("hikaricp.connections").meter().getId()
							.getTags())
							.containsExactly(Tag.of("pool", "firstDataSource"));
				});
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration
	static class TwoHikariDataSourcesConfiguration {

		@Bean
		public DataSource firstDataSource() {
			return createHikariDataSource("firstDataSource");
		}

		@Bean
		public DataSource secondOne() {
			return createHikariDataSource("secondOne");
		}

		private HikariDataSource createHikariDataSource(String poolName) {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			HikariDataSource hikariDataSource = DataSourceBuilder.create().url(url)
					.type(HikariDataSource.class).build();
			hikariDataSource.setPoolName(poolName);
			return hikariDataSource;
		}

	}

	@Configuration
	static class MixedDataSourcesConfiguration {

		@Bean
		public DataSource firstDataSource() {
			return createHikariDataSource("firstDataSource");
		}

		@Bean
		public DataSource secondOne() {
			return createTomcatDataSource();
		}

		private HikariDataSource createHikariDataSource(String poolName) {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			HikariDataSource hikariDataSource = DataSourceBuilder.create().url(url)
					.type(HikariDataSource.class).build();
			hikariDataSource.setPoolName(poolName);
			return hikariDataSource;
		}

		private org.apache.tomcat.jdbc.pool.DataSource createTomcatDataSource() {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			return DataSourceBuilder
					.create().url(url)
					.type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

	}

}
