/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.test.autoconfigure;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.jdbc.test.autoconfigure.TestDatabaseAutoConfiguration.EmbeddedDataSourceFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TestDatabaseAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class TestDatabaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(TestDatabaseAutoConfiguration.class));

	@Test
	void replaceWithNoDataSourceAvailable() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(DataSource.class));
	}

	@Test
	void replaceWithUniqueDatabase() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(EmbeddedDataSourceFactoryBean.class);
			DataSource datasource = context.getBean(DataSource.class);
			JdbcTemplate jdbcTemplate = new JdbcTemplate(datasource);
			jdbcTemplate.execute("create table example (id int, name varchar);");
			this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class).run((secondContext) -> {
				DataSource anotherDatasource = secondContext.getBean(DataSource.class);
				JdbcTemplate anotherJdbcTemplate = new JdbcTemplate(anotherDatasource);
				anotherJdbcTemplate.execute("create table example (id int, name varchar);");
			});
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "non-test", "non_test" })
	void replaceWithNonTestValue(String replace) {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
			.withPropertyValues("spring.test.database.replace=" + replace)
			.run((context) -> assertThat(context).hasSingleBean(EmbeddedDataSourceFactoryBean.class));
	}

	@ParameterizedTest
	@ValueSource(strings = { "auto-configured", "auto_configured" })
	void replaceWithAutoConfiguredValue(String replace) {
		this.contextRunner.withPropertyValues("spring.test.database.replace=" + replace)
			.run((context) -> assertThat(context).hasSingleBean(DataSource.class));
	}

	@Test
	void replaceWithEmptyValueDefaultsToNonTest() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
			.withPropertyValues("spring.test.database.replace=")
			.run((context) -> assertThat(context).hasSingleBean(EmbeddedDataSourceFactoryBean.class));
	}

	@Test
	void replaceWithInvalidValueFails() {
		this.contextRunner.withPropertyValues("spring.test.database.replace=invalid")
			.run((context) -> assertThat(context).getFailure()
				.cause()
				.isInstanceOf(BindException.class)
				.hasMessageContaining("spring.test.database.replace"));
	}

	@Test
	void whenUsingAotGeneratedArtifactsEmbeddedDataSourceFactoryBeanIsNotDefined() {
		this.contextRunner.withUserConfiguration(ExistingDataSourceConfiguration.class)
			.withSystemProperties("spring.aot.enabled=true")
			.run((context) -> assertThat(context).doesNotHaveBean(EmbeddedDataSourceFactoryBean.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class ExistingDataSourceConfiguration {

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.HSQL).build();
		}

	}

}
