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

package org.springframework.boot.autoconfigure.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.HikariCheckpointRestoreLifecycle;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} with Hikari.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class HikariDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.hikari.";

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
		.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName());

	@Test
	void testDataSourceExists() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBeansOfType(DataSource.class)).hasSize(1);
			assertThat(context.getBeansOfType(HikariDataSource.class)).hasSize(1);
		});
	}

	@Test
	void testDataSourcePropertiesOverridden() {
		this.contextRunner
			.withPropertyValues(PREFIX + "jdbc-url=jdbc:foo//bar/spam", "spring.datasource.hikari.max-lifetime=1234")
			.run((context) -> {
				HikariDataSource ds = context.getBean(HikariDataSource.class);
				assertThat(ds.getJdbcUrl()).isEqualTo("jdbc:foo//bar/spam");
				assertThat(ds.getMaxLifetime()).isEqualTo(1234);
			});
	}

	@Test
	void testDataSourceGenericPropertiesOverridden() {
		this.contextRunner
			.withPropertyValues(PREFIX + "data-source-properties.dataSourceClassName=org.h2.JDBCDataSource")
			.run((context) -> {
				HikariDataSource ds = context.getBean(HikariDataSource.class);
				assertThat(ds.getDataSourceProperties().getProperty("dataSourceClassName"))
					.isEqualTo("org.h2.JDBCDataSource");

			});
	}

	@Test
	void testDataSourceDefaultsPreserved() {
		this.contextRunner.run((context) -> {
			HikariDataSource ds = context.getBean(HikariDataSource.class);
			assertThat(ds.getMaxLifetime()).isEqualTo(1800000);
		});
	}

	@Test
	void nameIsAliasedToPoolName() {
		this.contextRunner.withPropertyValues("spring.datasource.name=myDS").run((context) -> {
			HikariDataSource ds = context.getBean(HikariDataSource.class);
			assertThat(ds.getPoolName()).isEqualTo("myDS");

		});
	}

	@Test
	void poolNameTakesPrecedenceOverName() {
		this.contextRunner.withPropertyValues("spring.datasource.name=myDS", PREFIX + "pool-name=myHikariDS")
			.run((context) -> {
				HikariDataSource ds = context.getBean(HikariDataSource.class);
				assertThat(ds.getPoolName()).isEqualTo("myHikariDS");
			});
	}

	@Test
	void usesCustomConnectionDetailsWhenDefined() {
		this.contextRunner.withBean(JdbcConnectionDetails.class, TestJdbcConnectionDetails::new)
			.withPropertyValues(PREFIX + "url=jdbc:broken", PREFIX + "username=alice", PREFIX + "password=secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcConnectionDetails.class)
					.doesNotHaveBean(PropertiesJdbcConnectionDetails.class);
				DataSource dataSource = context.getBean(DataSource.class);
				assertThat(dataSource).asInstanceOf(InstanceOfAssertFactories.type(HikariDataSource.class))
					.satisfies((hikari) -> {
						assertThat(hikari.getUsername()).isEqualTo("user-1");
						assertThat(hikari.getPassword()).isEqualTo("password-1");
						assertThat(hikari.getDriverClassName()).isEqualTo("org.postgresql.Driver");
						assertThat(hikari.getJdbcUrl())
							.isEqualTo("jdbc:customdb://customdb.example.com:12345/database-1");
					});
			});
	}

	@Test
	@ClassPathOverrides("org.crac:crac:1.3.0")
	void whenCheckpointRestoreIsAvailableHikariAutoConfigRegistersLifecycleBean() {
		this.contextRunner.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName())
			.run((context) -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycle.class));
	}

	@Test
	@ClassPathOverrides("org.crac:crac:1.3.0")
	void whenCheckpointRestoreIsAvailableAndDataSourceHasBeenWrappedHikariAutoConfigRegistersLifecycleBean() {
		this.contextRunner.withUserConfiguration(DataSourceWrapperConfiguration.class)
			.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName())
			.run((context) -> assertThat(context).hasSingleBean(HikariCheckpointRestoreLifecycle.class));
	}

	@Test
	void whenCheckpointRestoreIsNotAvailableHikariAutoConfigDoesNotRegisterLifecycleBean() {
		this.contextRunner.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName())
			.run((context) -> assertThat(context).doesNotHaveBean(HikariCheckpointRestoreLifecycle.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ConnectionDetailsConfiguration {

		@Bean
		JdbcConnectionDetails sqlConnectionDetails() {
			return new TestJdbcConnectionDetails();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceWrapperConfiguration {

		@Bean
		static BeanPostProcessor dataSourceWrapper() {
			return new BeanPostProcessor() {

				@Override
				public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
					if (bean instanceof DataSource dataSource) {
						return new DelegatingDataSource(dataSource);
					}
					return bean;
				}

			};
		}

	}

}
