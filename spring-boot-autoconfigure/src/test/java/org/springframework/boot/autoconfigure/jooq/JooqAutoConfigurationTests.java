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

package org.springframework.boot.autoconfigure.jooq;

import javax.sql.DataSource;

import org.hamcrest.Matcher;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.Record;
import org.jooq.RecordListener;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.SQLDialect;
import org.jooq.TransactionalRunnable;
import org.jooq.VisitListener;
import org.jooq.VisitListenerProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link JooqAutoConfiguration}.
 *
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 */
public class JooqAutoConfigurationTests {

	private static final String[] NO_BEANS = {};

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:jooqtest");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.jooq.sql-dialect:H2");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noDataSource() throws Exception {
		registerAndRefresh(JooqAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		assertEquals(0, this.context.getBeanNamesForType(DSLContext.class).length);
	}

	@Test
	public void jooqWithoutTx() throws Exception {
		registerAndRefresh(JooqDataSourceConfiguration.class,
				JooqAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		assertThat(getBeanNames(PlatformTransactionManager.class), equalTo(NO_BEANS));
		assertThat(getBeanNames(SpringTransactionProvider.class), equalTo(NO_BEANS));
		DSLContext dsl = this.context.getBean(DSLContext.class);
		dsl.execute("create table jooqtest (name varchar(255) primary key);");
		dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;",
				equalTo("0")));
		dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest (name) values ('foo');"));
		dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;",
				equalTo("1")));
		try {
			dsl.transaction(new ExecuteSql(dsl,
					"insert into jooqtest (name) values ('bar');",
					"insert into jooqtest (name) values ('foo');"));
			fail("An DataIntegrityViolationException should have been thrown.");
		}
		catch (DataIntegrityViolationException ex) {
		}
		dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;",
				equalTo("2")));
	}

	@Test
	public void jooqWithTx() throws Exception {
		registerAndRefresh(JooqDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TxManagerConfiguration.class,
				JooqAutoConfiguration.class);
		this.context.getBean(PlatformTransactionManager.class);
		DSLContext dsl = this.context.getBean(DSLContext.class);
		assertEquals(SQLDialect.H2, dsl.configuration().dialect());
		dsl.execute("create table jooqtest_tx (name varchar(255) primary key);");
		dsl.transaction(new AssertFetch(dsl,
				"select count(*) as total from jooqtest_tx;", equalTo("0")));
		dsl.transaction(new ExecuteSql(dsl,
				"insert into jooqtest_tx (name) values ('foo');"));
		dsl.transaction(new AssertFetch(dsl,
				"select count(*) as total from jooqtest_tx;", equalTo("1")));
		try {
			dsl.transaction(new ExecuteSql(dsl,
					"insert into jooqtest (name) values ('bar');",
					"insert into jooqtest (name) values ('foo');"));
			fail("A DataIntegrityViolationException should have been thrown.");
		}
		catch (DataIntegrityViolationException ex) {
		}
		dsl.transaction(new AssertFetch(dsl,
				"select count(*) as total from jooqtest_tx;", equalTo("1")));
	}

	@Test
	public void customProvidersArePickedUp() {
		registerAndRefresh(JooqDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TxManagerConfiguration.class,
				TestRecordMapperProvider.class, TestRecordListenerProvider.class,
				TestExecuteListenerProvider.class, TestVisitListenerProvider.class,
				JooqAutoConfiguration.class);
		DSLContext dsl = this.context.getBean(DSLContext.class);
		assertEquals(TestRecordMapperProvider.class, dsl.configuration()
				.recordMapperProvider().getClass());
		assertThat(dsl.configuration().recordListenerProviders().length, equalTo(1));
		assertThat(dsl.configuration().executeListenerProviders().length, equalTo(2));
		assertThat(dsl.configuration().visitListenerProviders().length, equalTo(1));
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	private String[] getBeanNames(Class<?> type) {
		return this.context.getBeanNamesForType(type);
	}

	private static class AssertFetch implements TransactionalRunnable {

		private final DSLContext dsl;

		private final String sql;

		private final Matcher<? super String> matcher;

		public AssertFetch(DSLContext dsl, String sql, Matcher<? super String> matcher) {
			this.dsl = dsl;
			this.sql = sql;
			this.matcher = matcher;
		}

		@Override
		public void run(org.jooq.Configuration configuration) throws Exception {
			assertThat(this.dsl.fetch(this.sql).getValue(0, 0).toString(), this.matcher);
		}

	}

	private static class ExecuteSql implements TransactionalRunnable {

		private final DSLContext dsl;

		private final String[] sql;

		public ExecuteSql(DSLContext dsl, String... sql) {
			this.dsl = dsl;
			this.sql = sql;
		}

		@Override
		public void run(org.jooq.Configuration configuration) throws Exception {
			for (String statement : this.sql) {
				this.dsl.execute(statement);
			}
		}

	}

	@Configuration
	protected static class JooqDataSourceConfiguration {

		@Bean
		public DataSource jooqDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:jooqtest")
					.username("sa").build();
		}

	}

	@Configuration
	protected static class TxManagerConfiguration {

		@Bean
		public PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

	}

	protected static class TestRecordMapperProvider implements RecordMapperProvider {

		@Override
		public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType,
				Class<? extends E> aClass) {
			return null;
		}

	}

	protected static class TestRecordListenerProvider implements RecordListenerProvider {

		@Override
		public RecordListener provide() {
			return null;
		}

	}

	protected static class TestExecuteListenerProvider implements ExecuteListenerProvider {

		@Override
		public ExecuteListener provide() {
			return null;
		}

	}

	protected static class TestVisitListenerProvider implements VisitListenerProvider {

		@Override
		public VisitListener provide() {
			return null;
		}

	}

}
