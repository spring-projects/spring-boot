/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jdbc.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JdbcClientAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class JdbcClientAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true")
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
				JdbcClientAutoConfiguration.class));

	@Test
	void jdbcClientWhenNoAvailableJdbcTemplateIsNotCreated() {
		new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(DataSourceAutoConfiguration.class, JdbcClientAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(JdbcClient.class));
	}

	@Test
	void jdbcClientWhenExistingJdbcTemplateIsCreated() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(JdbcClient.class);
			NamedParameterJdbcTemplate namedParameterJdbcTemplate = context.getBean(NamedParameterJdbcTemplate.class);
			assertThat(namedParameterJdbcTemplate.getJdbcOperations()).isEqualTo(context.getBean(JdbcOperations.class));
		});
	}

	@Test
	void jdbcClientWithCustomJdbcClientIsNotCreated() {
		this.contextRunner.withBean("customJdbcClient", JdbcClient.class, () -> mock(JdbcClient.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcClient.class);
				assertThat(context.getBean(JdbcClient.class)).isEqualTo(context.getBean("customJdbcClient"));
			});
	}

}
