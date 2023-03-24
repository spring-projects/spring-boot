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

package org.springframework.boot.actuate.autoconfigure.metrics.jdbc;

import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourcePoolMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Tommy Ludwig
 */
class DataSourcePoolMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true")
		.with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(DataSourcePoolMetricsAutoConfiguration.class))
		.withUserConfiguration(BaseConfiguration.class);

	@Test
	void autoConfiguredDataSourceIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection().getMetaData();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("jdbc.connections.max").tags("name", "dataSource").meter();
			});
	}

	@Test
	void dataSourceInstrumentationCanBeDisabled() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withPropertyValues("management.metrics.enable.jdbc=false")
			.run((context) -> {
				context.getBean(DataSource.class).getConnection().getMetaData();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("jdbc.connections.max").tags("name", "dataSource").meter()).isNull();
			});
	}

	@Test
	void allDataSourcesCanBeInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withUserConfiguration(TwoDataSourcesConfiguration.class)
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection().getMetaData();
				context.getBean("secondOne", DataSource.class).getConnection().getMetaData();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("jdbc.connections.max").tags("name", "first").meter();
				registry.get("jdbc.connections.max").tags("name", "secondOne").meter();
			});
	}

	@Test
	void allDataSourcesCanBeInstrumentedWithLazyInitialization() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withInitializer(
					(context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
			.withUserConfiguration(TwoDataSourcesConfiguration.class)
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection().getMetaData();
				context.getBean("secondOne", DataSource.class).getConnection().getMetaData();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("jdbc.connections.max").tags("name", "first").meter();
				registry.get("jdbc.connections.max").tags("name", "secondOne").meter();
			});
	}

	@Test
	void autoConfiguredHikariDataSourceIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hikaricp.connections").meter();
			});
	}

	@Test
	void autoConfiguredHikariDataSourceIsInstrumentedWhenUsingDataSourceInitialization() {
		this.contextRunner.withPropertyValues("spring.sql.init.schema:db/create-custom-schema.sql")
			.withConfiguration(
					AutoConfigurations.of(DataSourceAutoConfiguration.class, SqlInitializationAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hikaricp.connections").meter();
			});
	}

	@Test
	void hikariCanBeInstrumentedAfterThePoolHasBeenSealed() {
		this.contextRunner.withUserConfiguration(HikariSealingConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hikaricp.connections").meter()).isNotNull();
			});
	}

	@Test
	void hikariDataSourceInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.hikaricp=false")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("hikaricp.connections").meter()).isNull();
			});
	}

	@Test
	void allHikariDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(TwoHikariDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection();
				context.getBean("secondOne", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hikaricp.connections").tags("pool", "firstDataSource").meter();
				registry.get("hikaricp.connections").tags("pool", "secondOne").meter();
			});
	}

	@Test
	void someHikariDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(MixedDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection();
				context.getBean("secondOne", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.get("hikaricp.connections").meter().getId().getTags())
					.containsExactly(Tag.of("pool", "firstDataSource"));
			});
	}

	@Test
	void allHikariDataSourcesCanBeInstrumentedWhenUsingLazyInitialization() {
		this.contextRunner.withUserConfiguration(TwoHikariDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withInitializer(
					(context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection();
				context.getBean("secondOne", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hikaricp.connections").tags("pool", "firstDataSource").meter();
				registry.get("hikaricp.connections").tags("pool", "secondOne").meter();
			});
	}

	@Test
	void hikariProxiedDataSourceCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(ProxiedHikariDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("proxiedDataSource", DataSource.class).getConnection();
				context.getBean("delegateDataSource", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get("hikaricp.connections").tags("pool", "firstDataSource").meter();
				registry.get("hikaricp.connections").tags("pool", "secondOne").meter();
			});
	}

	@Test
	void hikariDataSourceIsInstrumentedWithoutMetadataProvider() {
		this.contextRunner.withUserConfiguration(OneHikariDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DataSourcePoolMetadataProvider.class);
			context.getBean("hikariDataSource", DataSource.class).getConnection();
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.get("hikaricp.connections").meter().getId().getTags())
				.containsExactly(Tag.of("pool", "hikariDataSource"));
		});
	}

	private static HikariDataSource createHikariDataSource(String poolName) {
		String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
		HikariDataSource hikariDataSource = DataSourceBuilder.create().url(url).type(HikariDataSource.class).build();
		hikariDataSource.setPoolName(poolName);
		return hikariDataSource;
	}

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		SimpleMeterRegistry simpleMeterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return createDataSource();
		}

		@Bean
		DataSource secondOne() {
			return createDataSource();
		}

		private DataSource createDataSource() {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TwoHikariDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return createHikariDataSource("firstDataSource");
		}

		@Bean
		DataSource secondOne() {
			return createHikariDataSource("secondOne");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ProxiedHikariDataSourcesConfiguration {

		@Bean
		DataSource proxiedDataSource() {
			return (DataSource) new ProxyFactory(createHikariDataSource("firstDataSource")).getProxy();
		}

		@Bean
		DataSource delegateDataSource() {
			return new DelegatingDataSource(createHikariDataSource("secondOne"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OneHikariDataSourceConfiguration {

		@Bean
		DataSource hikariDataSource() {
			return createHikariDataSource("hikariDataSource");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MixedDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return createHikariDataSource("firstDataSource");
		}

		@Bean
		DataSource secondOne() {
			return createTomcatDataSource();
		}

		private HikariDataSource createHikariDataSource(String poolName) {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			HikariDataSource hikariDataSource = DataSourceBuilder.create()
				.url(url)
				.type(HikariDataSource.class)
				.build();
			hikariDataSource.setPoolName(poolName);
			return hikariDataSource;
		}

		private org.apache.tomcat.jdbc.pool.DataSource createTomcatDataSource() {
			String url = "jdbc:hsqldb:mem:test-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class HikariSealingConfiguration {

		@Bean
		static HikariSealer hikariSealer() {
			return new HikariSealer();
		}

		static class HikariSealer implements BeanPostProcessor, PriorityOrdered {

			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof HikariDataSource dataSource) {
					try {
						dataSource.getConnection().close();
					}
					catch (SQLException ex) {
						throw new IllegalStateException(ex);
					}
				}
				return bean;
			}

		}

	}

}
