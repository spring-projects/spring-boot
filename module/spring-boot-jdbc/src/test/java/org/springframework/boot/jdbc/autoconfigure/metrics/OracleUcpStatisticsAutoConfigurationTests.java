/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.autoconfigure.metrics;

import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourcePoolMetricsAutoConfiguration}. Adapts for Oracle UCP the
 * same tests performed in {#link DataSourcePoolMetricsAutoConfigurationTests} for Hikari.
 *
 * @author Fabio Grassi
 * @since 4.1.0
 */
class OracleUcpStatisticsAutoConfigurationTests {

	private static final String ONE_UCP_METER_NAME = "oracleucp.connections.curr.count.open";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true",
				"management.metrics.use-global-registry=false")
		.withBean(SimpleMeterRegistry.class)
		.withConfiguration(
				AutoConfigurations.of(MetricsAutoConfiguration.class, DataSourcePoolMetricsAutoConfiguration.class));

	@AfterEach
	void destroyAllOracleConnectionPools() {
		try {
			final UniversalConnectionPoolManager mgr = UniversalConnectionPoolManagerImpl
				.getUniversalConnectionPoolManager();
			for (String poolName : mgr.getConnectionPoolNames()) {
				mgr.destroyConnectionPool(poolName);
			}
		}
		catch (UniversalConnectionPoolException e) {
			throw new InvalidDataAccessApiUsageException("Error while destroying Oracle connection pools", e);
		}
	}

	@Test
	@ClassPathExclusions({ "Hikari*.jar", "tomcat-jdbc*.jar", "commons-dbcp2*.jar" })
	void autoConfiguredPoolDataSourceIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get(ONE_UCP_METER_NAME).meter();
			});
	}

	@Test
	@ClassPathExclusions({ "Hikari*.jar", "tomcat-jdbc*.jar", "commons-dbcp2*.jar" })
	void autoConfiguredPoolDataSourceIsInstrumentedWhenUsingDataSourceInitialization() {
		this.contextRunner.withPropertyValues("spring.sql.init.schema:db/create-custom-schema.sql")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceInitializationAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get(ONE_UCP_METER_NAME).meter();
			});
	}

	@Test
	@ClassPathExclusions({ "Hikari*.jar", "tomcat-jdbc*.jar", "commons-dbcp2*.jar" })
	void poolCanBeInstrumentedAfterThePoolHasBeenSealed() {
		this.contextRunner.withUserConfiguration(OracleUCPSealingConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasNotFailed();
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find(ONE_UCP_METER_NAME).meter()).isNotNull();
			});
	}

	@Test
	@ClassPathExclusions({ "Hikari*.jar", "tomcat-jdbc*.jar", "commons-dbcp2*.jar" })
	void poolDataSourceInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.oracleucp=false")
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean(DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find(ONE_UCP_METER_NAME).meter()).isNull();
			});
	}

	@Test
	void allPoolDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(MultiplePoolDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("standardDataSource", DataSource.class).getConnection();
				context.getBean("nonDefault", DataSource.class).getConnection();
				context.getBean("nonAutowire", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find(ONE_UCP_METER_NAME).meters()).map((meter) -> meter.getId().getTag("pool"))
					.containsOnly("standardDataSource", "nonDefault");
			});
	}

	@Test
	void somePoolDataSourcesCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(MixedDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("firstDataSource", DataSource.class).getConnection();
				context.getBean("secondOne", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.get(ONE_UCP_METER_NAME).meter().getId().getTags())
					.containsExactly(Tag.of("pool", "firstDataSource"));
			});
	}

	@Test
	void allPoolDataSourcesCanBeInstrumentedWhenUsingLazyInitialization() {
		this.contextRunner.withUserConfiguration(MultiplePoolDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.withInitializer(
					(context) -> context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()))
			.run((context) -> {
				context.getBean("standardDataSource", DataSource.class).getConnection();
				context.getBean("nonDefault", DataSource.class).getConnection();
				context.getBean("nonAutowire", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find(ONE_UCP_METER_NAME).meters()).map((meter) -> meter.getId().getTag("pool"))
					.containsOnly("standardDataSource", "nonDefault");
			});
	}

	@Test
	void proxiedPoolDataSourceCanBeInstrumented() {
		this.contextRunner.withUserConfiguration(ProxiedPoolDataSourcesConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
			.run((context) -> {
				context.getBean("proxiedDataSource", DataSource.class).getConnection();
				context.getBean("delegateDataSource", DataSource.class).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				registry.get(ONE_UCP_METER_NAME).tags("pool", "firstDataSource").meter();
				registry.get(ONE_UCP_METER_NAME).tags("pool", "secondOne").meter();
			});
	}

	@Test
	void poolDataSourceIsInstrumentedWithoutMetadataProvider() {
		this.contextRunner.withUserConfiguration(OnePoolDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DataSourcePoolMetadataProvider.class);
			context.getBean("poolDataSource", DataSource.class).getConnection();
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			assertThat(registry.get(ONE_UCP_METER_NAME).meter().getId().getTags())
				.containsExactly(Tag.of("pool", "poolDataSource"));
		});
	}

	@Test
	void prototypeDataSourceIsIgnored() {
		this.contextRunner
			.withUserConfiguration(OnePoolDataSourceConfiguration.class, PrototypeDataSourceConfiguration.class)
			.run((context) -> {
				context.getBean("poolDataSource", DataSource.class).getConnection();
				((DataSource) context.getBean("prototypeDataSource", "", "")).getConnection();
				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.get(ONE_UCP_METER_NAME).meter().getId().getTags())
					.contains(Tag.of("pool", "poolDataSource"));
			});
	}

	private static PoolDataSourceImpl createPoolDataSource(String poolName) {
		final PoolDataSourceImpl poolDataSource = DataSourceBuilder.create()
			.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
			.type(PoolDataSourceImpl.class)
			.build();
		try {
			poolDataSource.setConnectionPoolName(poolName);
		}
		catch (SQLException e) {
			throw new InvalidDataAccessApiUsageException(
					"Cannot set Oracle UCP connection pool name '" + poolName + "'", e);
		}
		return poolDataSource;
	}

	@Configuration(proxyBeanMethods = false)
	static class MultiplePoolDataSourcesConfiguration {

		@Bean
		DataSource standardDataSource() {
			return createPoolDataSource("standardDataSource");
		}

		@Bean(defaultCandidate = false)
		DataSource nonDefault() {
			return createPoolDataSource("nonDefault");
		}

		@Bean(autowireCandidate = false)
		DataSource nonAutowire() {
			return createPoolDataSource("nonAutowire");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ProxiedPoolDataSourcesConfiguration {

		@Bean
		DataSource proxiedDataSource() {
			return (DataSource) new ProxyFactory(createPoolDataSource("firstDataSource")).getProxy();
		}

		@Bean
		DataSource delegateDataSource() {
			return new DelegatingDataSource(createPoolDataSource("secondOne"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OnePoolDataSourceConfiguration {

		@Bean
		DataSource poolDataSource() {
			return createPoolDataSource("poolDataSource");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MixedDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return createPoolDataSource("firstDataSource");
		}

		@Bean
		DataSource secondOne() {
			return createHikariDataSource("secondOne");
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

	}

	@Configuration(proxyBeanMethods = false)
	static class PrototypeDataSourceConfiguration {

		@Bean
		@Scope(BeanDefinition.SCOPE_PROTOTYPE)
		DataSource prototypeDataSource(String username, String password) {
			return createPoolDataSource(username, password);
		}

		private PoolDataSourceImpl createPoolDataSource(String username, String password) {
			return DataSourceBuilder.create()
				.url("jdbc:hsqldb:mem:test-" + UUID.randomUUID())
				.type(PoolDataSourceImpl.class)
				.username(username)
				.password(password)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class OracleUCPSealingConfiguration {

		@Bean
		static OracleUCPSealer oracleUCPSealer() {
			return new OracleUCPSealer();
		}

		static class OracleUCPSealer implements BeanPostProcessor, PriorityOrdered {

			@Override
			public int getOrder() {
				return Ordered.HIGHEST_PRECEDENCE;
			}

			@Override
			public Object postProcessAfterInitialization(Object bean, String beanName) {
				if (bean instanceof PoolDataSourceImpl dataSource) {
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
