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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.sql.DataSource;

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

/**
 * Tests for {@link JooqAutoConfiguration}.
 *
 * @author Andreas Ahlenstorf
 */
public class JooqAutoConfigurationTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.name:jooqtest");
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jooq.sql-dialect:H2");
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

		assertEquals(0,
				this.context.getBeanNamesForType(PlatformTransactionManager.class).length);
		assertEquals(0,
				this.context.getBeanNamesForType(SpringTransactionProvider.class).length);

		final DSLContext dsl = this.context.getBean(DSLContext.class);

		dsl.execute("create table jooqtest (name varchar(255) primary key);");

		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("0", (dsl.fetch("select count(*) as total from jooqtest;")
						.getValue(0, 0).toString()));
			}
		});
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				dsl.execute("insert into jooqtest (name) values ('foo');");
			}
		});
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("1", (dsl.fetch("select count(*) as total from jooqtest;")
						.getValue(0, 0).toString()));
			}
		});
		try {
			dsl.transaction(new TransactionalRunnable() {
				@Override
				public void run(org.jooq.Configuration configuration) throws Exception {
					dsl.execute("insert into jooqtest (name) values ('bar');");
					dsl.execute("insert into jooqtest (name) values ('foo');");
				}
			});

			fail("An org.springframework.dao.DataIntegrityViolationException should have been thrown.");
		}
		catch (org.springframework.dao.DataIntegrityViolationException e) {
			// ok
		}
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("2", (dsl.fetch("select count(*) as total from jooqtest;")
						.getValue(0, 0).toString()));
			}
		});
	}

	@Test
	public void jooqWithTx() throws Exception {
		registerAndRefresh(JooqDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TxManagerConfiguration.class,
				JooqAutoConfiguration.class);

		this.context.getBean(PlatformTransactionManager.class);

		final DSLContext dsl = this.context.getBean(DSLContext.class);
		assertEquals(SQLDialect.H2, dsl.configuration().dialect());

		dsl.execute("create table jooqtest_tx (name varchar(255) primary key);");

		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("0",
						(dsl.fetch("select count(*) as total from jooqtest_tx;")
								.getValue(0, 0).toString()));
			}
		});
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				dsl.execute("insert into jooqtest_tx (name) values ('foo');");
			}
		});
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("1",
						(dsl.fetch("select count(*) as total from jooqtest_tx;")
								.getValue(0, 0).toString()));
			}
		});
		try {
			dsl.transaction(new TransactionalRunnable() {
				@Override
				public void run(org.jooq.Configuration configuration) throws Exception {
					dsl.execute("insert into jooqtest_tx (name) values ('bar');");
					dsl.execute("insert into jooqtest_tx (name) values ('foo');");
				}
			});

			fail("A DataIntegrityViolationException should have been thrown.");
		}
		catch (DataIntegrityViolationException e) {
			// ok
		}
		dsl.transaction(new TransactionalRunnable() {
			@Override
			public void run(org.jooq.Configuration configuration) throws Exception {
				assertEquals("1",
						(dsl.fetch("select count(*) as total from jooqtest_tx;")
								.getValue(0, 0).toString()));
			}
		});
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
		assertEquals(1, dsl.configuration().recordListenerProviders().length);
		assertEquals(2, dsl.configuration().executeListenerProviders().length);
		assertEquals(1, dsl.configuration().visitListenerProviders().length);
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
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