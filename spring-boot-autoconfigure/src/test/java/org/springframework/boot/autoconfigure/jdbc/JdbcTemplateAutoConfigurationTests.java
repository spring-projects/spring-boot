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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.Random;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JdbcTemplateAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 */
public class JdbcTemplateAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void restore() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testJdbcTemplateExists() {
		load();
		assertThat(this.context.getBeansOfType(JdbcTemplate.class)).hasSize(1);
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate.getDataSource()).isEqualTo(
				this.context.getBean(DataSource.class));
	}

	@Test
	public void testJdbcTemplateExistsWithCustomDataSource() {
		load(TestDataSourceConfiguration.class);
		assertThat(this.context.getBeansOfType(JdbcTemplate.class)).hasSize(1);
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate).isNotNull();
		assertThat(jdbcTemplate.getDataSource()).isEqualTo(
				this.context.getBean("customDataSource"));
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() {
		load();
		assertThat(this.context.getBeansOfType(NamedParameterJdbcOperations.class))
				.hasSize(1);
	}

	@Test
	public void testMultiDataSource() {
		load(MultiDataSourceConfiguration.class);
		assertThat(this.context.getBeansOfType(JdbcOperations.class)).isEmpty();
		assertThat(this.context.getBeansOfType(NamedParameterJdbcOperations.class))
				.isEmpty();
	}

	@Test
	public void testMultiDataSourceUsingPrimary() {
		load(MultiDataSourceUsingPrimaryConfiguration.class);
		assertThat(this.context.getBeansOfType(JdbcOperations.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(NamedParameterJdbcOperations.class))
				.hasSize(1);
		assertThat(this.context.getBean(JdbcTemplate.class).getDataSource())
				.isEqualTo(this.context.getBean("test1DataSource"));
	}

	@Test
	public void testExistingCustomJdbcTemplate() {
		load(CustomConfiguration.class);
		assertThat(this.context.getBeansOfType(JdbcOperations.class)).hasSize(1);
		assertThat(this.context.getBean(JdbcOperations.class))
				.isEqualTo(this.context.getBean("customJdbcOperations"));
	}

	@Test
	public void testExistingCustomNamedParameterJdbcTemplate() {
		load(CustomConfiguration.class);
		assertThat(this.context.getBeansOfType(NamedParameterJdbcOperations.class))
				.hasSize(1);
		assertThat(this.context.getBean(NamedParameterJdbcOperations.class))
				.isEqualTo(this.context.getBean("customNamedParameterJdbcOperations"));
	}

	public void load(String... environment) {
		load(null, environment);
	}

	public void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("spring.datasource.initialize:false",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt())
				.applyTo(ctx);
		TestPropertyValues.of(environment).applyTo(ctx);
		if (config != null) {
			ctx.register(config);
		}
		ctx.register(DataSourceAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		public JdbcOperations customJdbcOperations(DataSource dataSource) {
			return new JdbcTemplate(dataSource);
		}

		@Bean
		public NamedParameterJdbcOperations customNamedParameterJdbcOperations(
				DataSource dataSource) {
			return new NamedParameterJdbcTemplate(dataSource);
		}

	}

	@Configuration
	static class TestDataSourceConfiguration {

		@Bean
		public DataSource customDataSource() {
			return new TestDataSource("overridedb");
		}

	}

}
