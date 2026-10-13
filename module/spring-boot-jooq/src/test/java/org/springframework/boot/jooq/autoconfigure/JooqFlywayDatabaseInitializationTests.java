/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.flywaydb.core.Flyway;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for jOQQ when Flyway is being used for DB initialization.
 *
 * @author Andy Wilkinson
 */
class JooqFlywayDatabaseInitializationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
		.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JooqConfiguration.class)
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void whenFlywayIsAutoConfiguredThenJooqDslContextDependsOnFlywayBeans() {
		this.contextRunner.run((context) -> {
			BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
			assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("flywayInitializer", "flyway");
		});
	}

	@Test
	void whenCustomMigrationInitializerIsDefinedThenJooqDslContextDependsOnIt() {
		this.contextRunner.withUserConfiguration(CustomFlywayMigrationInitializer.class).run((context) -> {
			BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
			assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("flywayMigrationInitializer", "flyway");
		});
	}

	@Test
	void whenCustomFlywayIsDefinedThenJooqDslContextDependsOnIt() {
		this.contextRunner.withUserConfiguration(CustomFlyway.class).run((context) -> {
			BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
			assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("customFlyway");
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JooqConfiguration {

		@Bean
		DSLContext dslContext() {
			return new DefaultDSLContext(SQLDialect.H2);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlywayMigrationInitializer {

		@Bean
		FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
			FlywayMigrationInitializer initializer = new FlywayMigrationInitializer(flyway);
			initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return initializer;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlyway {

		@Bean
		Flyway customFlyway() {
			return Flyway.configure().load();
		}

	}

}
