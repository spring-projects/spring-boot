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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.Random;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
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
 * @since 1.4.0
 */
public class JdbcTemplateAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EmbeddedDatabaseConnection.override = null;
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void restore() {
		EmbeddedDatabaseConnection.override = null;
		this.context.close();
	}

	@Test
	public void testJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate).isNotNull();
		assertThat(jdbcTemplate.getDataSource()).isNotNull();
	}

	@Test
	public void testJdbcTemplateExistsWithCustomDataSource() throws Exception {
		this.context.register(TestDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		JdbcTemplate jdbcTemplate = this.context.getBean(JdbcTemplate.class);
		assertThat(jdbcTemplate).isNotNull();
		assertThat(jdbcTemplate.getDataSource() instanceof BasicDataSource).isTrue();
	}

	@Test
	public void testNamedParameterJdbcTemplateExists() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(NamedParameterJdbcOperations.class)).isNotNull();
	}

	@Test
	public void testMultiDataSource() throws Exception {
		this.context.register(MultiDataSourceConfiguration.class,
				DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBeansOfType(JdbcOperations.class)).isEmpty();
		assertThat(this.context.getBeansOfType(NamedParameterJdbcOperations.class))
				.isEmpty();
	}

	@Test
	public void testMultiDataSourceUsingPrimary() throws Exception {
		this.context.register(MultiDataSourceUsingPrimaryConfiguration.class,
				DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JdbcOperations.class)).isNotNull();
		assertThat(this.context.getBean(NamedParameterJdbcOperations.class)).isNotNull();
	}

	@Test
	public void testExistingCustomJdbcTemplate() throws Exception {
		this.context.register(CustomConfiguration.class,
				DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(JdbcOperations.class))
				.isEqualTo(this.context.getBean("customJdbcOperations"));
	}

	@Test
	public void testExistingCustomNamedParameterJdbcTemplate() throws Exception {
		this.context.register(CustomConfiguration.class,
				DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(NamedParameterJdbcOperations.class))
				.isEqualTo(this.context.getBean("customNamedParameterJdbcOperations"));
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
		public DataSource dataSource() {
			return new TestDataSource("overridedb");
		}

	}

}
