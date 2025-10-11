/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jooq.autoconfigure;

import javax.sql.DataSource;

import org.jooq.CharsetProvider;
import org.jooq.ConnectionProvider;
import org.jooq.ConverterProvider;
import org.jooq.DSLContext;
import org.jooq.ExecuteListener;
import org.jooq.ExecuteListenerProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;
import org.jooq.TransactionalRunnable;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
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
 * @author Dennis Melzer
 * @author Moritz Halbritter
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
				assertThatExceptionOfType(DataIntegrityViolationException.class)
					.isThrownBy(() -> dsl.transaction(new ExecuteSql(dsl, "insert into jooqtest (name) values ('bar');",
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
	void jooqWithDefaultTransactionProvider() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class, TxManagerConfiguration.class)
			.run((context) -> {
				DSLContext dsl = context.getBean(DSLContext.class);
				TransactionProvider expectedTransactionProvider = context.getBean(TransactionProvider.class);
				TransactionProvider transactionProvider = dsl.configuration().transactionProvider();
				assertThat(transactionProvider).isSameAs(expectedTransactionProvider);
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
	void relaxedBindingOfSqlDialect() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
			.withPropertyValues("spring.jooq.sql-dialect:PoSTGrES")
			.run((context) -> assertThat(context.getBean(org.jooq.Configuration.class).dialect())
				.isEqualTo(SQLDialect.POSTGRES));
	}

	@Test
	void transactionProviderBacksOffOnExistingTransactionProvider() {
		this.contextRunner
			.withUserConfiguration(JooqDataSourceConfiguration.class, CustomTransactionProviderConfiguration.class)
			.run((context) -> {
				TransactionProvider transactionProvider = context.getBean(TransactionProvider.class);
				assertThat(transactionProvider).isInstanceOf(CustomTransactionProvider.class);
				DSLContext dsl = context.getBean(DSLContext.class);
				assertThat(dsl.configuration().transactionProvider()).isSameAs(transactionProvider);
			});
	}

	@Test
	void jooqExceptionTranslatorProviderFromConfigurationCustomizerOverridesJooqExceptionTranslatorBean() {
		this.contextRunner
			.withUserConfiguration(JooqDataSourceConfiguration.class, CustomJooqExceptionTranslatorConfiguration.class)
			.run((context) -> {
				assertThat(context.getBean(ExceptionTranslatorExecuteListener.class))
					.isInstanceOf(CustomJooqExceptionTranslator.class);
				assertThat(context.getBean(DefaultExecuteListenerProvider.class).provide())
					.isInstanceOf(CustomJooqExceptionTranslator.class);
			});
	}

	@Test
	void jooqWithDefaultJooqExceptionTranslator() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class).run((context) -> {
			ExceptionTranslatorExecuteListener translator = context.getBean(ExceptionTranslatorExecuteListener.class);
			assertThat(translator).isInstanceOf(DefaultExceptionTranslatorExecuteListener.class);
		});
	}

	@Test
	void transactionProviderFromConfigurationCustomizerOverridesTransactionProviderBean() {
		this.contextRunner
			.withUserConfiguration(JooqDataSourceConfiguration.class, TxManagerConfiguration.class,
					CustomTransactionProviderFromCustomizerConfiguration.class)
			.run((context) -> {
				TransactionProvider transactionProvider = context.getBean(TransactionProvider.class);
				assertThat(transactionProvider).isInstanceOf(SpringTransactionProvider.class);
				DSLContext dsl = context.getBean(DSLContext.class);
				assertThat(dsl.configuration().transactionProvider()).isInstanceOf(CustomTransactionProvider.class);
			});
	}

	@Test
	void autoConfiguredJooqConfigurationCanBeUsedToCreateCustomDslContext() {
		this.contextRunner.withUserConfiguration(CustomDslContextConfiguration.class, JooqDataSourceConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(DSLContext.class).hasBean("customDslContext"));
	}

	@Test
	void shouldLoadSettingsFromConfigPropertyThroughJaxb() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
			.withPropertyValues("spring.jooq.config=classpath:org/springframework/boot/jooq/autoconfigure/settings.xml")
			.run((context) -> {
				assertThat(context).hasSingleBean(Settings.class);
				Settings settings = context.getBean(Settings.class);
				assertThat(settings.getBatchSize()).isEqualTo(100);
			});
	}

	@Test
	void shouldNotProvideSettingsIfJaxbIsMissing() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
			.withClassLoader(new FilteredClassLoader("jakarta.xml.bind"))
			.withPropertyValues("spring.jooq.config=classpath:org/springframework/boot/autoconfigure/jooq/settings.xml")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasRootCauseInstanceOf(JaxbNotAvailableException.class));
	}

	@Test
	void shouldFailWithSensibleErrorMessageIfConfigIsNotFound() {
		this.contextRunner.withUserConfiguration(JooqDataSourceConfiguration.class)
			.withPropertyValues("spring.jooq.config=classpath:does-not-exist.xml")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.hasMessageContaining("spring.jooq.config")
				.hasMessageContaining("does-not-exist.xml"));
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
			assertThat(this.dsl.fetch(this.sql).getValue(0, 0)).hasToString(this.expected);
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
	static class CustomTransactionProviderConfiguration {

		@Bean
		TransactionProvider transactionProvider() {
			return new CustomTransactionProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomJooqExceptionTranslatorConfiguration {

		@Bean
		ExceptionTranslatorExecuteListener jooqExceptionTranslator() {
			return new CustomJooqExceptionTranslator();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomTransactionProviderFromCustomizerConfiguration {

		@Bean
		DefaultConfigurationCustomizer transactionProviderCustomizer() {
			return (configuration) -> configuration.setTransactionProvider(new CustomTransactionProvider());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TxManagerConfiguration {

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDslContextConfiguration {

		@Bean
		DSLContext customDslContext(org.jooq.Configuration configuration) {
			return new DefaultDSLContext(configuration);
		}

	}

	@Order(100)
	static class TestExecuteListenerProvider implements ExecuteListenerProvider {

		@Override
		public ExecuteListener provide() {
			return mock(ExecuteListener.class);
		}

	}

	static class CustomTransactionProvider implements TransactionProvider {

		@Override
		public void begin(TransactionContext ctx) {

		}

		@Override
		public void commit(TransactionContext ctx) {

		}

		@Override
		public void rollback(TransactionContext ctx) {

		}

	}

	static class CustomJooqExceptionTranslator implements ExceptionTranslatorExecuteListener {

	}

}
