/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
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
 */
public class TestDatabaseAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void replaceWithNoDataSourceAvailable() {
		load(null);
		assertThat(this.context.getBeansOfType(DataSource.class)).isEmpty();
	}

	@Test
	public void replaceWithUniqueDatabase() {
		load(ExistingDataSourceConfiguration.class);
		DataSource datasource = this.context.getBean(DataSource.class);
		JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
		jdbcTemplate.execute("create table example (id int, name varchar);");
		ConfigurableApplicationContext anotherContext = doLoad(
				ExistingDataSourceConfiguration.class);
		try {
			DataSource anotherDatasource = anotherContext.getBean(DataSource.class);
			JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(anotherDatasource);
			anotherJdbcTemplate.execute("create table example (id int, name varchar);");
		}
		finally {
			anotherContext.close();
		}
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private ConfigurableApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(TestDatabaseAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(ctx, environment);
		ctx.refresh();
		return ctx;
	}

	@Configuration
	static class ExistingDataSourceConfiguration {

		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder()
					.generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL);
			return builder.build();
		}

	}

}
