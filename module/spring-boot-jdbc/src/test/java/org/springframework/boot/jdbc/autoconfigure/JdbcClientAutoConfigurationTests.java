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
