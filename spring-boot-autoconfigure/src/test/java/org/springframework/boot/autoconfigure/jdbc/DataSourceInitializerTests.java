/*
 * Copyright 2013-2014 the original author or authors.
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
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DataSourceInitializer}.
 * 
 * @author Dave Syer
 */
public class DataSourceInitializerTests {

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
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDefaultDataSourceDoesNotExists() throws Exception {
		this.context.register(DataSourceInitializer.class,
				PropertyPlaceholderAutoConfiguration.class, DataSourceProperties.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(DataSource.class).length);
	}

	@Test
	public void testDataSourceInitialized() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:true");
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from BAR", Integer.class));
	}

	@Test
	public void testDataSourceInitializedWithExplicitScript() throws Exception {
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(
				this.context,
				"spring.datasource.initialize:true",
				"spring.datasource.schema:"
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"schema.sql"));
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from FOO", Integer.class));
	}

	@Test
	public void testDataSourceInitializedWithMultipleScripts() throws Exception {
		EnvironmentTestUtils.addEnvironment(
				this.context,
				"spring.datasource.initialize:true",
				"spring.datasource.schema:"
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"schema.sql")
						+ ","
						+ ClassUtils.addResourcePathToPackagePath(getClass(),
								"another.sql"));
		this.context.register(DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		DataSource dataSource = this.context.getBean(DataSource.class);
		assertTrue(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource);
		assertNotNull(dataSource);
		JdbcOperations template = new JdbcTemplate(dataSource);
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from FOO", Integer.class));
		assertEquals(new Integer(0),
				template.queryForObject("SELECT COUNT(*) from SPAM", Integer.class));
	}

}
