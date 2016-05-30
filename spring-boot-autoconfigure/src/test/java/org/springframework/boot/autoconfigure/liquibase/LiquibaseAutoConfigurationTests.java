/*
 * Copyright 2012-2016 the original author or authors.
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

import java.io.File;
import java.util.Map;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.liquibase.CommonsLoggingLiquibaseLogger;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseAutoConfiguration}.
 *
 * @author Marcel Overdijk
 * @author Andy Wilkinson
 */
public class LiquibaseAutoConfigurationTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

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
		assertThat(this.context.getBeanNamesForType(SpringLiquibase.class).length)
				.isEqualTo(0);
	}

	@Test
	public void testDefaultSpringLiquibase() throws Exception {
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-master.yaml");
		assertThat(liquibase.getContexts()).isNull();
		assertThat(liquibase.getDefaultSchema()).isNull();
		assertThat(liquibase.isDropFirst()).isFalse();
	}

	@Test
	public void testXmlChangeLog() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.change-log:classpath:/db/changelog/db.changelog-override.xml");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.xml");
	}

	@Test
	public void testJsonChangeLog() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.change-log:classpath:/db/changelog/db.changelog-override.json");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.json");
	}

	@Test
	public void testSqlChangeLog() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.change-log:classpath:/db/changelog/db.changelog-override.sql");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.sql");
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
		assertThat(liquibase.getContexts()).isEqualTo("test, production");
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
		assertThat(liquibase.getDefaultSchema()).isEqualTo("public");
	}

	@Test
	public void testOverrideDropFirst() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context, "liquibase.drop-first:true");
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		assertThat(liquibase.isDropFirst()).isTrue();
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
		assertThat(liquibase.getDataSource().getConnection().getMetaData().getURL())
				.isEqualTo("jdbc:hsqldb:mem:liquibase");
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
		assertThat(log).isInstanceOf(CommonsLoggingLiquibaseLogger.class);
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
		assertThat(liquibase.getLabels()).isEqualTo("test, production");
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
		assertThat(parameters.containsKey("foo")).isTrue();
		assertThat(parameters.get("foo")).isEqualTo("bar");
	}

	@Test
	public void testRollbackFile() throws Exception {
		File file = this.temp.newFile("rollback-file.sql");
		EnvironmentTestUtils.addEnvironment(this.context,
				"liquibase.rollbackFile:" + file.getAbsolutePath());
		this.context.register(EmbeddedDataSourceConfiguration.class,
				LiquibaseAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		SpringLiquibase liquibase = this.context.getBean(SpringLiquibase.class);
		File actualFile = (File) ReflectionTestUtils.getField(liquibase, "rollbackFile");
		assertThat(actualFile).isEqualTo(file).exists();
		String content = new String(FileCopyUtils.copyToByteArray(file));
		assertThat(content).contains("DROP TABLE PUBLIC.customer;");
	}

}
