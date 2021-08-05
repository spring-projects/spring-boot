/*
 * Copyright 2012-2021 the original author or authors.
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

import javax.sql.DataSource;

import org.jooq.CharsetProvider;
import org.jooq.ConnectionProvider;
import org.jooq.ConverterProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.ExecutorProvider;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionListenerProvider;
import org.jooq.TransactionalRunnable;
import org.jooq.VisitListenerProvider;
import org.jooq.impl.DataSourceConnectionProvider;
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
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

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
	void jooqWithDefaultConnectionProvider() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class).run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			ConnectionProvider connectionProvider = dsl.configuration().connectionProvider();
			assertThat(connectionProvider).isInstanceOf(DataSourceConnectionProvider.class);
			DataSource connectionProviderDataSource = ((DataSourceConnectionProvider) connectionProvider).dataSource();
			assertThat(connectionProviderDataSource).isInstanceOf(TransactionAwareDataSourceProxy.class);
		});
	}

	@Test
	void jooqWithDefaultExecuteListenerProvider() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class).run((context) -> {
			DSLContext dsl = context.getBean(DSLContext.class);
			assertThat(dsl.configuration().executeListenerProviders()).hasSize(1);
		});
	}

	@Test
	void jooqWithSeveralExecuteListenerProviders() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class, TestExecuteListenerProvider.class)
				.run((context) -> {
					DSLContext dsl = context.getBean(DSLContext.class);
					ExecuteListenerProvider[] executeListenerProviders = dsl.configuration().executeListenerProviders();
					assertThat(executeListenerProviders).hasSize(2);
					assertThat(executeListenerProviders[0]).isInstanceOf(DefaultExecuteListenerProvider.class);
					assertThat(executeListenerProviders[1]).isInstanceOf(TestExecuteListenerProvider.class);
				});
	}

	@Test
	void dslContextWithConfigurationCustomizersAreApplied() {
		ConverterProvider converterProvider = mock(ConverterProvider.class);
		CharsetProvider charsetProvider = mock(CharsetProvider.class);
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
				.withBean("configurationCustomizer1", DefaultConfigurationCustomizer.class,
						() -> (configuration) -> configuration.set(converterProvider))
				.withBean("configurationCustomizer2", DefaultConfigurationCustomizer.class,
						() -> (configuration) -> configuration.set(charsetProvider))
				.run((context) -> {
					DSLContext dsl = context.getBean(DSLContext.class);
					assertThat(dsl.configuration().converterProvider()).isSameAs(converterProvider);
					assertThat(dsl.configuration().charsetProvider()).isSameAs(charsetProvider);
				});
	}

	@Test
	@Deprecated
	void customProvidersArePickedUp() {
		RecordMapperProvider recordMapperProvider = mock(RecordMapperProvider.class);
		RecordUnmapperProvider recordUnmapperProvider = mock(RecordUnmapperProvider.class);
		RecordListenerProvider recordListenerProvider = mock(RecordListenerProvider.class);
		VisitListenerProvider visitListenerProvider = mock(VisitListenerProvider.class);
		TransactionListenerProvider transactionListenerProvider = mock(TransactionListenerProvider.class);
		ExecutorProvider executorProvider = mock(ExecutorProvider.class);
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class, TxManagerConfiguration.class)
				.withBean(RecordMapperProvider.class, () -> recordMapperProvider)
				.withBean(RecordUnmapperProvider.class, () -> recordUnmapperProvider)
				.withBean(RecordListenerProvider.class, () -> recordListenerProvider)
				.withBean(VisitListenerProvider.class, () -> visitListenerProvider)
				.withBean(TransactionListenerProvider.class, () -> transactionListenerProvider)
				.withBean(ExecutorProvider.class, () -> executorProvider).run((context) -> {
					DSLContext dsl = context.getBean(DSLContext.class);
					assertThat(dsl.configuration().recordMapperProvider()).isSameAs(recordMapperProvider);
					assertThat(dsl.configuration().recordUnmapperProvider()).isSameAs(recordUnmapperProvider);
					assertThat(dsl.configuration().executorProvider()).isSameAs(executorProvider);
					assertThat(dsl.configuration().recordListenerProviders()).containsExactly(recordListenerProvider);
					assertThat(dsl.configuration().visitListenerProviders()).containsExactly(visitListenerProvider);
					assertThat(dsl.configuration().transactionListenerProviders())
							.containsExactly(transactionListenerProvider);
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

	@Order(100)
	static class TestExecuteListenerProvider implements ExecuteListenerProvider {

		@Override
		public ExecuteListener provide() {
			return null;
		}

	}

}
