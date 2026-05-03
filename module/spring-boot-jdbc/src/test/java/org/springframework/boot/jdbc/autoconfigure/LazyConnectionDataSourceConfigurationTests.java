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

package org.springframework.boot.jdbc.autoconfigure;

import java.util.Random;
import java.util.UUID;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LazyConnectionDataSourceConfiguration}.
 *
 * @author Stephane Nicoll
 */
class LazyConnectionDataSourceConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class))
		.withPropertyValues("spring.datasource.url:jdbc:h2:mem:test-" + new Random().nextInt());

	@Test
	void autoConfigurationConfiguresLazyProxyWhenEnabled() {
		this.contextRunner
			.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.connection-fetch=lazy")
			.run((context) -> {
				assertThat(context).hasSingleBean(DataSource.class).hasSingleBean(LazyConnectionDataSourceProxy.class);
				DataSource dataSource = context.getBean(LazyConnectionDataSourceProxy.class);
				HikariDataSource actualDataSource = dataSource.unwrap(HikariDataSource.class);
				assertThat(actualDataSource.getJdbcUrl()).startsWith("jdbc:h2:mem:test-");
			});
	}

	@ParameterizedTest
	@ValueSource(strings = { "eager", "lazy" })
	void autoConfigurationExposeDataSourceMBeanWhenEnabled(String connectionFetchStrategy) {
		String uniqueDomain = UUID.randomUUID().toString();
		this.contextRunner.withConfiguration(AutoConfigurations.of(JmxAutoConfiguration.class))
			.withPropertyValues("spring.jmx.enabled=true", "spring.jmx.default-domain=" + uniqueDomain,
					"spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.connection-fetch=" + connectionFetchStrategy)
			.run((context) -> {
				MBeanServer mBeanServer = context.getBean(MBeanServer.class);
				ObjectName objectName = new ObjectName(
						"%s:type=%s,name=dataSource".formatted(uniqueDomain, HikariDataSource.class.getSimpleName()));
				assertThat(mBeanServer.isRegistered(objectName)).isTrue();
			});
	}

	@Test
	void autoConfigurationBacksOffWhenPropertyIsNotSet() {
		this.contextRunner.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName())
			.run((context) -> assertThat(context).hasSingleBean(DataSource.class)
				.hasSingleBean(HikariDataSource.class)
				.doesNotHaveBean(LazyConnectionDataSourceProxy.class));
	}

	@Test
	void autoConfigurationDoesNotConfigureLazyProxyWhenEager() {
		this.contextRunner
			.withPropertyValues("spring.datasource.type=" + HikariDataSource.class.getName(),
					"spring.datasource.connection-fetch=eager")
			.run((context) -> assertThat(context).hasSingleBean(DataSource.class)
				.hasSingleBean(HikariDataSource.class)
				.doesNotHaveBean(LazyConnectionDataSourceProxy.class));
	}

	@Test
	void autoConfigurationBacksOffWhenUserProvidesDataSource() {
		DataSource dataSource = mock(DataSource.class);
		this.contextRunner.withBean(DataSource.class, () -> dataSource)
			.withPropertyValues("spring.datasource.connection-fetch=lazy")
			.run((context) -> {
				assertThat(context).hasSingleBean(DataSource.class)
					.doesNotHaveBean(LazyConnectionDataSourceProxy.class);
				assertThat(context.getBean(DataSource.class)).isSameAs(dataSource);
			});
	}

}
