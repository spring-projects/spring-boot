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
import java.util.function.Function;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} without spring-jdbc on the classpath.
 *
 * @author Andy Wilkinson
 */
@ClassPathExclusions("spring-jdbc-*.jar")
class DataSourceAutoConfigurationWithoutSpringJdbcTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

	@Test
	void pooledDataSourceCanBeAutoConfigured() {
		this.contextRunner.run((context) -> {
			HikariDataSource dataSource = context.getBean(HikariDataSource.class);
			assertThat(dataSource.getJdbcUrl()).isNotNull();
			assertThat(dataSource.getDriverClassName()).isNotNull();
		});
	}

	@Test
	void withoutConnectionPoolsAutoConfigurationBacksOff() {
		this.contextRunner.with(hideConnectionPools())
			.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	void withUrlAndWithoutConnectionPoolsAutoConfigurationBacksOff() {
		this.contextRunner.with(hideConnectionPools())
			.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt())
			.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	private static Function<ApplicationContextRunner, ApplicationContextRunner> hideConnectionPools() {
		return (runner) -> runner.withClassLoader(new FilteredClassLoader("org.apache.tomcat", "com.zaxxer.hikari",
				"org.apache.commons.dbcp2", "oracle.ucp.jdbc", "com.mchange"));
	}

}
