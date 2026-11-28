/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import javax.sql.DataSource;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.reactor.extensions.CoreSubscriberProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.core.Ordered;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy;
import org.springframework.transaction.ReactiveTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link R2dbcJooqAutoConfiguration}.
 *
 * @author George Papadopoulos
 */
class R2dbcJooqAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(JooqAutoConfiguration.class, R2dbcJooqAutoConfiguration.class,
				R2dbcTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	void noConnectionFactory() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DSLContext.class));
	}

	@Test
	void noR2dbc() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("io.r2dbc", "org.springframework.r2dbc"))
			.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(DSLContext.class));
	}

	@Test
	void noSubscriberProvider() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(CoreSubscriberProvider.class))
			.withBean(ConnectionFactory.class, this::connectionFactory)
			.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(DSLContext.class));
	}

	@Test
	void noCoroutines() {
		this.contextRunner.withClassLoader(new FilteredClassLoader("kotlin", "kotlinx.coroutines"))
			.withBean(ConnectionFactory.class, this::connectionFactory)
			.run((context) -> assertThat(context).hasSingleBean(DSLContext.class));
	}

	@Test
	void autoConfiguredConnectionFactory() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class, DataSourceAutoConfiguration.class))
			.withPropertyValues("spring.r2dbc.url=r2dbc:h2:mem:///jooq")
			.run((context) -> {
				assertThat(context).hasSingleBean(DSLContext.class).doesNotHaveBean(DataSource.class);
				org.jooq.Configuration configuration = context.getBean(DSLContext.class).configuration();
				assertThat(configuration.dialect()).isEqualTo(SQLDialect.H2);
				assertThat(configuration.subscriberProvider()).isInstanceOf(CoreSubscriberProvider.class);
				assertThat(configuration.connectionFactory()).isInstanceOfSatisfying(
						TransactionAwareConnectionFactoryProxy.class,
						(proxy) -> assertThat(proxy.getTargetConnectionFactory())
							.isSameAs(context.getBean(ConnectionFactory.class)));
				assertThat(context).hasSingleBean(ReactiveTransactionManager.class)
					.hasSingleBean(JooqR2dbcTransactionManager.class);
			});
	}

	@Test
	void jdbcConfigurationTakesPrecedenceWhenBothConnectionTypesArePresent() {
		this.contextRunner.withUserConfiguration(JooqAutoConfigurationTests.JooqDataSourceConfiguration.class)
			.withBean(ConnectionFactory.class, this::connectionFactory)
			.run((context) -> {
				assertThat(context).hasSingleBean(DSLContext.class).doesNotHaveBean(JooqR2dbcTransactionManager.class);
				assertThat(context.getBean(DSLContext.class).dialect()).isEqualTo(SQLDialect.HSQLDB);
			});
	}

	@Test
	void multipleConnectionFactories() {
		this.contextRunner.withBean("first", ConnectionFactory.class, this::connectionFactory)
			.withBean("second", ConnectionFactory.class, this::connectionFactory)
			.run((context) -> assertThat(context).doesNotHaveBean(DSLContext.class));
	}

	@Test
	void primaryConnectionFactory() {
		ConnectionFactory primary = connectionFactory();
		this.contextRunner.withBean("first", ConnectionFactory.class, this::connectionFactory)
			.withBean("second", ConnectionFactory.class, () -> primary, (definition) -> definition.setPrimary(true))
			.run((context) -> {
				assertThat(context).hasSingleBean(DSLContext.class);
				assertThat(context.getBean(DSLContext.class).configuration().connectionFactory())
					.isInstanceOfSatisfying(TransactionAwareConnectionFactoryProxy.class,
							(proxy) -> assertThat(proxy.getTargetConnectionFactory()).isSameAs(primary));
			});
	}

	@Test
	void configuredDialect() {
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withPropertyValues("spring.jooq.sql-dialect=POSTGRES")
			.run((context) -> assertThat(context.getBean(DSLContext.class).dialect()).isEqualTo(SQLDialect.POSTGRES));
	}

	@Test
	void unknownDialect() {
		ConnectionFactory factory = mock(ConnectionFactory.class);
		given(factory.getMetadata()).willReturn(() -> "unknown");
		this.contextRunner.withBean(ConnectionFactory.class, () -> factory)
			.run((context) -> assertThat(context.getBean(DSLContext.class).dialect()).isEqualTo(SQLDialect.DEFAULT));
	}

	@Test
	void customConfiguration() {
		org.jooq.Configuration configuration = new DefaultConfiguration().set(SQLDialect.POSTGRES);
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withBean(org.jooq.Configuration.class, () -> configuration)
			.run((context) -> assertThat(context.getBean(DSLContext.class).configuration()).isSameAs(configuration));
	}

	@Test
	void customDslContext() {
		DSLContext dsl = DSL.using(SQLDialect.POSTGRES);
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withBean("customDslContext", DSLContext.class, () -> dsl)
			.run((context) -> assertThat(context.getBean(DSLContext.class)).isSameAs(dsl));
	}

	@Test
	void customTransactionManager() {
		ReactiveTransactionManager manager = new R2dbcTransactionManager(connectionFactory());
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withBean(ReactiveTransactionManager.class, () -> manager)
			.run((context) -> {
				assertThat(context).hasSingleBean(ReactiveTransactionManager.class);
				assertThat(context.getBean(ReactiveTransactionManager.class)).isSameAs(manager);
			});
	}

	@Test
	void settingsBean() {
		Settings settings = new Settings().withFetchSize(42);
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withBean(Settings.class, () -> settings)
			.run((context) -> assertThat(context.getBean(DSLContext.class).settings().getFetchSize()).isEqualTo(42));
	}

	@Test
	void settingsFromConfigProperty() {
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withPropertyValues("spring.jooq.config=classpath:org/springframework/boot/jooq/autoconfigure/settings.xml")
			.run((context) -> assertThat(context.getBean(DSLContext.class).settings().getBatchSize()).isEqualTo(100));
	}

	@Test
	void customizersOverrideDefaultsInOrder() {
		this.contextRunner.withBean(ConnectionFactory.class, this::connectionFactory)
			.withBean("first", DefaultConfigurationCustomizer.class, () -> new DialectCustomizer(0, SQLDialect.MYSQL))
			.withBean("second", DefaultConfigurationCustomizer.class,
					() -> new DialectCustomizer(1, SQLDialect.POSTGRES))
			.run((context) -> assertThat(context.getBean(DSLContext.class).dialect()).isEqualTo(SQLDialect.POSTGRES));
	}

	private ConnectionFactory connectionFactory() {
		return ConnectionFactories.get("r2dbc:h2:mem:///jooq");
	}

	private record DialectCustomizer(int order, SQLDialect dialect) implements DefaultConfigurationCustomizer, Ordered {

		@Override
		public int getOrder() {
			return this.order;
		}

		@Override
		public void customize(DefaultConfiguration configuration) {
			configuration.set(this.dialect);
		}

	}

}
