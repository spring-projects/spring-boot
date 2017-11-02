/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.session.JdbcSessionConfiguration.SpringBootJdbcHttpSessionConfiguration;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.test.context.HideClassesClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
public class SessionAutoConfigurationJdbcTests
		extends AbstractSessionAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class,
					JdbcTemplateAutoConfiguration.class, SessionAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	public void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc")
				.run(this::validateDefaultConfig);
	}

	@Test
	public void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(
						new HideClassesClassLoader(HazelcastSessionRepository.class,
								MongoOperationsSessionRepository.class,
								RedisOperationsSessionRepository.class))
				.run(this::validateDefaultConfig);
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		JdbcOperationsSessionRepository repository = validateSessionRepository(context,
				JdbcOperationsSessionRepository.class);
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("tableName"))
				.isEqualTo("SPRING_SESSION");
		assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
				.isEqualTo(DataSourceInitializationMode.EMBEDDED);
		assertThat(context.getBean(JdbcOperations.class)
				.queryForList("select * from SPRING_SESSION")).isEmpty();
		SpringBootJdbcHttpSessionConfiguration configuration = context
				.getBean(SpringBootJdbcHttpSessionConfiguration.class);
		assertThat(new DirectFieldAccessor(configuration).getPropertyValue("cleanupCron"))
				.isEqualTo("0 * * * * *");
	}

	@Test
	public void filterOrderCanBeCustomized() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc",
				"spring.session.servlet.filter-order=123").run((context) -> {
					FilterRegistrationBean<?> registration = context
							.getBean(FilterRegistrationBean.class);
					assertThat(registration.getOrder()).isEqualTo(123);
				});
	}

	@Test
	public void disableDataSourceInitializer() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc",
				"spring.session.jdbc.initialize-schema=never").run((context) -> {
					JdbcOperationsSessionRepository repository = validateSessionRepository(
							context, JdbcOperationsSessionRepository.class);
					assertThat(new DirectFieldAccessor(repository)
							.getPropertyValue("tableName")).isEqualTo("SPRING_SESSION");
					assertThat(context.getBean(JdbcSessionProperties.class)
							.getInitializeSchema())
									.isEqualTo(DataSourceInitializationMode.NEVER);
					this.thrown.expect(BadSqlGrammarException.class);
					context.getBean(JdbcOperations.class)
							.queryForList("select * from SPRING_SESSION");
				});
	}

	@Test
	public void customTableName() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc",
						"spring.session.jdbc.table-name=FOO_BAR",
						"spring.session.jdbc.schema=classpath:session/custom-schema-h2.sql")
				.run((context) -> {
					JdbcOperationsSessionRepository repository = validateSessionRepository(
							context, JdbcOperationsSessionRepository.class);
					assertThat(new DirectFieldAccessor(repository)
							.getPropertyValue("tableName")).isEqualTo("FOO_BAR");
					assertThat(context.getBean(JdbcSessionProperties.class)
							.getInitializeSchema())
									.isEqualTo(DataSourceInitializationMode.EMBEDDED);
					assertThat(context.getBean(JdbcOperations.class)
							.queryForList("select * from FOO_BAR")).isEmpty();
				});
	}

	@Test
	public void customCleanupCron() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc",
						"spring.session.jdbc.cleanup-cron=0 0 12 * * *")
				.run((context) -> {
					assertThat(
							context.getBean(JdbcSessionProperties.class).getCleanupCron())
									.isEqualTo("0 0 12 * * *");
					SpringBootJdbcHttpSessionConfiguration configuration = context
							.getBean(SpringBootJdbcHttpSessionConfiguration.class);
					assertThat(new DirectFieldAccessor(configuration)
							.getPropertyValue("cleanupCron")).isEqualTo("0 0 12 * * *");
				});
	}

}
