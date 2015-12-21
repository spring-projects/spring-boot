/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.liquibase;

import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link LiquibaseAutoConfiguration}.
 *
 * @author Marcel Overdijk
 */
public class LiquibaseAutoConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:liquibasetest");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testNoDataSource() throws Exception {
		this.context.register(LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(SpringLiquibase.class).length);
	}

	@Test
	public void testDefaultSpringLiquibase() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("classpath:/db/changelog/db.changelog-master.yaml",
				liquibase.getChangeLog());
		assertNull(liquibase.getContexts());
		assertNull(liquibase.getDefaultSchema());
		assertFalse(liquibase.isDropFirst());
	}

	@Test
	public void testOverrideChangeLog() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.change-log:classpath:/db/changelog/db.changelog-override.xml");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("classpath:/db/changelog/db.changelog-override.xml",
				liquibase.getChangeLog());
	}

	@Test
	public void testOverrideContexts() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.contexts:test, production");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("test, production", liquibase.getContexts());
	}

	@Test
	public void testOverrideDefaultSchema() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.default-schema:public");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("public", liquibase.getDefaultSchema());
	}

	@Test
	public void testOverrideDropFirst() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "liquibase.drop-first:true");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertTrue(liquibase.isDropFirst());
	}

	@Test
	public void testOverrideDataSource() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.url:jdbc:hsqldb:mem:liquibase", "liquibase.user:sa");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("jdbc:hsqldb:mem:liquibase",
				liquibase.getDataSource().getConnection().getMetaData().getURL());
	}

	@Test(expected = BeanCreationException.class)
	public void testChangeLogDoesNotExist() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.change-log:classpath:/no-such-changelog.yaml");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void testLogger() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		Object log = ReflectionTestUtils.getField(liquibase, "log");
		assertThat(log, instanceOf(CommonsLoggingLiquibaseLogger.class));
	}

	@Test
	public void testOverrideLabels() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.labels:test, production");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertEquals("test, production", liquibase.getLabels());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOverrideParameters() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "liquibase.parameters.foo:bar");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		Map<String, String> parameters = (Map<String, String>) ReflectionTestUtils
				.getField(liquibase, "parameters");
		assertTrue(parameters.containsKey("foo"));
		assertEquals("bar", parameters.get("foo"));
	}

}
