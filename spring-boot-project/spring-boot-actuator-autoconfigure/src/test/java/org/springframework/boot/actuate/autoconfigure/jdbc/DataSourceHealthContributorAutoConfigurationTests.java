/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.jdbc;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration.RoutingDataSourceHealthContributor;
import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvidersConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DataSourceHealthContributorAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Julio Gomez
 * @author Safeer Ansari
 */
class DataSourceHealthContributorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					HealthContributorAutoConfiguration.class, DataSourceHealthContributorAutoConfiguration.class));

	@Test
	void runShouldCreateIndicator() {
		this.contextRunner.run((context) -> {
			context.getBean(DataSourceHealthIndicator.class);
			assertThat(context).hasSingleBean(DataSourceHealthIndicator.class);
		});
	}

	@Test
	void runWhenMultipleDataSourceBeansShouldCreateCompositeIndicator() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, DataSourceConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(CompositeHealthContributor.class);
					CompositeHealthContributor contributor = context.getBean(CompositeHealthContributor.class);
					String[] names = contributor.stream().map(NamedContributor::getName).toArray(String[]::new);
					assertThat(names).containsExactlyInAnyOrder("dataSource", "testDataSource");
				});
	}

	@Test
	void runWithRoutingAndEmbeddedDataSourceShouldIncludeRoutingDataSource() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, RoutingDataSourceConfig.class)
				.run((context) -> {
					CompositeHealthContributor composite = context.getBean(CompositeHealthContributor.class);
					assertThat(composite.getContributor("dataSource")).isInstanceOf(DataSourceHealthIndicator.class);
					assertThat(composite.getContributor("routingDataSource"))
							.isInstanceOf(RoutingDataSourceHealthContributor.class);
				});
	}

	@Test
	void runWithRoutingAndEmbeddedDataSourceShouldNotIncludeRoutingDataSourceWhenIgnored() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, RoutingDataSourceConfig.class)
				.withPropertyValues("management.health.db.ignore-routing-datasources:true").run((context) -> {
					assertThat(context).doesNotHaveBean(CompositeHealthContributor.class);
					assertThat(context).hasSingleBean(DataSourceHealthIndicator.class);
					assertThat(context).doesNotHaveBean(RoutingDataSourceHealthContributor.class);
				});
	}

	@Test
	void runWithOnlyRoutingDataSourceShouldIncludeRoutingDataSourceWithComposedIndicators() {
		this.contextRunner.withUserConfiguration(RoutingDataSourceConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(RoutingDataSourceHealthContributor.class);
			RoutingDataSourceHealthContributor routingHealthContributor = context
					.getBean(RoutingDataSourceHealthContributor.class);
			assertThat(routingHealthContributor.getContributor("one")).isInstanceOf(DataSourceHealthIndicator.class);
			assertThat(routingHealthContributor.getContributor("two")).isInstanceOf(DataSourceHealthIndicator.class);
			assertThat(routingHealthContributor.iterator()).toIterable().extracting("name")
					.containsExactlyInAnyOrder("one", "two");
		});
	}

	@Test
	void runWithOnlyRoutingDataSourceShouldCrashWhenIgnored() {
		this.contextRunner.withUserConfiguration(RoutingDataSourceConfig.class)
				.withPropertyValues("management.health.db.ignore-routing-datasources:true")
				.run((context) -> assertThat(context).hasFailed().getFailure()
						.hasRootCauseInstanceOf(IllegalArgumentException.class));
	}

	@Test
	void runWithValidationQueryPropertyShouldUseCustomQuery() {
		this.contextRunner
				.withUserConfiguration(DataSourceConfig.class, DataSourcePoolMetadataProvidersConfiguration.class)
				.withPropertyValues("spring.datasource.test.validation-query:SELECT from FOOBAR").run((context) -> {
					assertThat(context).hasSingleBean(DataSourceHealthIndicator.class);
					DataSourceHealthIndicator indicator = context.getBean(DataSourceHealthIndicator.class);
					assertThat(indicator.getQuery()).isEqualTo("SELECT from FOOBAR");
				});
	}

	@Test
	void runWhenDisabledShouldNotCreateIndicator() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("management.health.db.enabled:false")
				.run((context) -> assertThat(context).doesNotHaveBean(DataSourceHealthIndicator.class)
						.doesNotHaveBean(CompositeHealthContributor.class));
	}

	@Test
	void runWhenDataSourceHasNullRoutingKeyShouldProduceUnnamedComposedIndicator() {
		this.contextRunner.withUserConfiguration(NullKeyRoutingDataSourceConfig.class).run((context) -> {
			assertThat(context).hasSingleBean(RoutingDataSourceHealthContributor.class);
			RoutingDataSourceHealthContributor routingHealthContributor = context
					.getBean(RoutingDataSourceHealthContributor.class);
			assertThat(routingHealthContributor.getContributor("unnamed"))
					.isInstanceOf(DataSourceHealthIndicator.class);
			assertThat(routingHealthContributor.getContributor("one")).isInstanceOf(DataSourceHealthIndicator.class);
			assertThat(routingHealthContributor.iterator()).toIterable().extracting("name")
					.containsExactlyInAnyOrder("unnamed", "one");
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class DataSourceConfig {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.test")
		DataSource testDataSource() {
			return DataSourceBuilder.create().type(org.apache.tomcat.jdbc.pool.DataSource.class)
					.driverClassName("org.hsqldb.jdbc.JDBCDriver").url("jdbc:hsqldb:mem:test").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class RoutingDataSourceConfig {

		@Bean
		AbstractRoutingDataSource routingDataSource() {
			Map<Object, DataSource> dataSources = new HashMap<>();
			dataSources.put("one", mock(DataSource.class));
			dataSources.put("two", mock(DataSource.class));
			AbstractRoutingDataSource routingDataSource = mock(AbstractRoutingDataSource.class);
			given(routingDataSource.getResolvedDataSources()).willReturn(dataSources);
			return routingDataSource;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NullKeyRoutingDataSourceConfig {

		@Bean
		AbstractRoutingDataSource routingDataSource() {
			Map<Object, DataSource> dataSources = new HashMap<>();
			dataSources.put(null, mock(DataSource.class));
			dataSources.put("one", mock(DataSource.class));
			AbstractRoutingDataSource routingDataSource = mock(AbstractRoutingDataSource.class);
			given(routingDataSource.getResolvedDataSources()).willReturn(dataSources);
			return routingDataSource;
		}

	}

}
