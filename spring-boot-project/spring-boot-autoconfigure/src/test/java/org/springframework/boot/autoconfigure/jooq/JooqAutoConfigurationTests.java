/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.jooq;

import java.util.concurrent.Executor;

import javax.sql.DataSource;

import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.ExecutorProvider;
import org.jooq.Record;
import org.jooq.RecordListener;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapper;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordType;
import org.jooq.RecordUnmapper;
import org.jooq.RecordUnmapperProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionListener;
import org.jooq.TransactionListenerProvider;
import org.jooq.TransactionalRunnable;
import org.jooq.VisitListener;
import org.jooq.VisitListenerProvider;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link JooqAutoConfiguration}.
 *
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
class JooqAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JooqAutoConfiguration.class))
			.withPropertyValues("spring.datasource.name:jooqtest");

	@Test
	void noDataSource() {
		this.contextRunner.run((context) -> assertThat(context.getBeansOfType(DSLContext.class)).isEmpty());
	}

	@Test
	void jooqWithoutTx() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(PlatformTransactionManager.class);
			assertThat(context).doesNotHaveBean(SpringTransactionProvider.class);
			DSLContext dsl = context.getBean(DSLContext.class);
			dsl.execute("create table jooqtest (name varchar(255) primary key);");
			dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;", "0"));
			dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest (name) values ('foo');"));
			dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;", "1"));
			assertThatExceptionOfType(DataIntegrityViolationException.class)
					.isThrownBy(() -> dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest (name) values ('bar');",
							"insert into jooqtest (name) values ('foo');")));
			dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest;", "2"));
		});
	}

	@Test
	void jooqWithTx() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class, TxManagerConfiguration.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(PlatformTransactionManager.class);
					DSLContext dsl = context.getBean(DSLContext.class);
					assertThat(dsl.configuration().dialect()).isEqualTo(SQLDialect.HSQLDB);
					dsl.execute("create table jooqtest_tx (name varchar(255) primary key);");
					dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest_tx;", "0"));
					dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest_tx (name) values ('foo');"));
					dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest_tx;", "1"));
					assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(
							() -> dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest (name) values ('bar');",
									"insert into jooqtest (name) values ('foo');")));
					dsl.transaction(new AssertFetch(dsl, "select count(*) as total from jooqtest_tx;", "1"));
				});

	}

	@Test
	void customProvidersArePickedUp() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class, TxManagerConfiguration.class,
				TestRecordMapperProvider.class, TestRecordUnmapperProvider.class, TestRecordListenerProvider.class,
				TestExecuteListenerProvider.class, TestVisitListenerProvider.class,
				TestTransactionListenerProvider.class, TestExecutorProvider.class).run((context) -> {
					DSLContext dsl = context.getBean(DSLContext.class);
					assertThat(dsl.configuration().recordMapperProvider().getClass())
							.isEqualTo(TestRecordMapperProvider.class);
					assertThat(dsl.configuration().recordUnmapperProvider().getClass())
							.isEqualTo(TestRecordUnmapperProvider.class);
					assertThat(dsl.configuration().executorProvider().getClass()).isEqualTo(TestExecutorProvider.class);
					assertThat(dsl.configuration().recordListenerProviders()).hasSize(1);
					ExecuteListenerProvider[] executeListenerProviders = dsl.configuration().executeListenerProviders();
					assertThat(executeListenerProviders).hasSize(2);
					assertThat(executeListenerProviders[0]).isInstanceOf(DefaultExecuteListenerProvider.class);
					assertThat(executeListenerProviders[1]).isInstanceOf(TestExecuteListenerProvider.class);
					assertThat(dsl.configuration().visitListenerProviders()).hasSize(1);
					assertThat(dsl.configuration().transactionListenerProviders()).hasSize(1);
				});
	}

	@Test
	void relaxedBindingOfSqlDialect() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
				.withPropertyValues("spring.jooq.sql-dialect:PoSTGrES")
				.run((context) -> assertThat(context.getBean(org.jooq.Configuration.class).dialect())
						.isEqualTo(SQLDialect.POSTGRES));
	}

	static class AssertFetch implements TransactionalRunnable {

		private final DSLContext dsl;

		private final String sql;

		private final String expected;

		AssertFetch(DSLContext dsl, String sql, String expected) {
			this.dsl = dsl;
			this.sql = sql;
			this.expected = expected;
		}

		@Override
		public void run(org.jooq.Configuration configuration) {
			assertThat(this.dsl.fetch(this.sql).getValue(0, 0).toString()).isEqualTo(this.expected);
		}

	}

	static class ExecuteSql implements TransactionalRunnable {

		private final DSLContext dsl;

		private final String[] sql;

		ExecuteSql(DSLContext dsl, String... sql) {
			this.dsl = dsl;
			this.sql = sql;
		}

		@Override
		public void run(org.jooq.Configuration configuration) {
			for (String statement : this.sql) {
				this.dsl.execute(statement);
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JooqDataSourceConfiguration {

		@Bean
		DataSource jooqDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:jooqtest").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TxManagerConfiguration {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

	}

	static class TestRecordMapperProvider implements RecordMapperProvider {

		@Override
		public <R extends Record, E> RecordMapper<R, E> provide(RecordType<R> recordType, Class<? extends E> aClass) {
			return null;
		}

	}

	static class TestRecordUnmapperProvider implements RecordUnmapperProvider {

		@Override
		public <E, R extends Record> RecordUnmapper<E, R> provide(Class<? extends E> aClass, RecordType<R> recordType) {
			return null;
		}

	}

	static class TestRecordListenerProvider implements RecordListenerProvider {

		@Override
		public RecordListener provide() {
			return null;
		}

	}

	@Order(100)
	static class TestExecuteListenerProvider implements ExecuteListenerProvider {

		@Override
		public ExecuteListener provide() {
			return null;
		}

	}

	static class TestVisitListenerProvider implements VisitListenerProvider {

		@Override
		public VisitListener provide() {
			return null;
		}

	}

	static class TestTransactionListenerProvider implements TransactionListenerProvider {

		@Override
		public TransactionListener provide() {
			return null;
		}

	}

	static class TestExecutorProvider implements ExecutorProvider {

		@Override
		public Executor provide() {
			return null;
		}

	}

}
