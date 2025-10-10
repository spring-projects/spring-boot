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

package org.springframework.boot.jdbc.test.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.test.autoconfigure.TestDatabaseAutoConfiguration.EmbeddedDataSourceFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestDatabaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class TestDatabaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

	@Test
	void replaceWithNoDataSourceAvailable() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	void replaceWithUniqueDatabase() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(EmbeddedDataSourceFactoryBean.class);
			DataSource datasource = context.getBean(DataSource.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
			jdbcTemplate.execute("create table example (id int, name varchar);");
			this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((secondContext) -> {
				DataSource anotherDatasource = secondContext.getBean(DataSource.class);
				JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(anotherDatasource);
				anotherJdbcTemplate.execute("create table example (id int, name varchar);");
			});
		});
	}

	@Test
	void whenUsingAotGeneratedArtifactsEmbeddedDataSourceFactoryBeanIsNotDefined() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
			.withSystemProperties("spring.aot.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean(EmbeddedDataSourceFactoryBean.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ExistingDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
		}

	}

}
