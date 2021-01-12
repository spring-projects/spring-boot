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

package org.springframework.boot.autoconfigure.session;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.session.JdbcSessionConfiguration.SpringBootJdbcHttpSessionConfiguration;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.SpringSessionDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * JDBC specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
class SessionAutoConfigurationJdbcTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
					SessionAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc").run(this::validateDefaultConfig);
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(HazelcastIndexedSessionRepository.class,
						MongoIndexedSessionRepository.class, RedisIndexedSessionRepository.class))
				.run(this::validateDefaultConfig);
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		JdbcIndexedSessionRepository repository = validateSessionRepository(context,
				JdbcIndexedSessionRepository.class);
		assertThat(repository).hasFieldOrPropertyWithValue("tableName", "SPRING_SESSION");
		assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
				.isEqualTo(DataSourceInitializationMode.EMBEDDED);
		assertThat(context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION")).isEmpty();
		SpringBootJdbcHttpSessionConfiguration configuration = context
				.getBean(SpringBootJdbcHttpSessionConfiguration.class);
		assertThat(configuration).hasFieldOrPropertyWithValue("cleanupCron", "0 * * * * *");
	}

	@Test
	void filterOrderCanBeCustomized() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.servlet.filter-order=123")
				.run((context) -> {
					FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
					assertThat(registration.getOrder()).isEqualTo(123);
				});
	}

	@Test
	void disableDataSourceInitializer() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.jdbc.initialize-schema=never")
				.run((context) -> {
					JdbcIndexedSessionRepository repository = validateSessionRepository(context,
							JdbcIndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("tableName", "SPRING_SESSION");
					assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.NEVER);
					assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(
							() -> context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION"));
				});
	}

	@Test
	void sessionDataSourceIsUsedWhenAvailable() {
		this.contextRunner.withUserConfiguration(SessionDataSourceConfiguration.class)
				.withPropertyValues("spring.session.store-type=jdbc").run((context) -> {
					JdbcIndexedSessionRepository repository = validateSessionRepository(context,
							JdbcIndexedSessionRepository.class);
					assertThat(repository).extracting("jdbcOperations").extracting("dataSource")
							.isEqualTo(context.getBean("sessionDataSource"));
					assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(
							() -> context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION"));
				});
	}

	@Test
	void customTableName() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.jdbc.table-name=FOO_BAR",
						"spring.session.jdbc.schema=classpath:session/custom-schema-h2.sql")
				.run((context) -> {
					JdbcIndexedSessionRepository repository = validateSessionRepository(context,
							JdbcIndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("tableName", "FOO_BAR");
					assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
							.isEqualTo(DataSourceInitializationMode.EMBEDDED);
					assertThat(context.getBean(JdbcOperations.class).queryForList("select * from FOO_BAR")).isEmpty();
				});
	}

	@Test
	void customCleanupCron() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.jdbc.cleanup-cron=0 0 12 * * *")
				.run((context) -> {
					assertThat(context.getBean(JdbcSessionProperties.class).getCleanupCron()).isEqualTo("0 0 12 * * *");
					SpringBootJdbcHttpSessionConfiguration configuration = context
							.getBean(SpringBootJdbcHttpSessionConfiguration.class);
					assertThat(configuration).hasFieldOrPropertyWithValue("cleanupCron", "0 0 12 * * *");
				});
	}

	@Test
	void customFlushMode() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.jdbc.flush-mode=immediate")
				.run((context) -> {
					assertThat(context.getBean(JdbcSessionProperties.class).getFlushMode())
							.isEqualTo(FlushMode.IMMEDIATE);
					SpringBootJdbcHttpSessionConfiguration configuration = context
							.getBean(SpringBootJdbcHttpSessionConfiguration.class);
					assertThat(configuration).hasFieldOrPropertyWithValue("flushMode", FlushMode.IMMEDIATE);
				});
	}

	@Test
	void customSaveMode() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=jdbc", "spring.session.jdbc.save-mode=on-get-attribute")
				.run((context) -> {
					assertThat(context.getBean(JdbcSessionProperties.class).getSaveMode())
							.isEqualTo(SaveMode.ON_GET_ATTRIBUTE);
					SpringBootJdbcHttpSessionConfiguration configuration = context
							.getBean(SpringBootJdbcHttpSessionConfiguration.class);
					assertThat(configuration).hasFieldOrPropertyWithValue("saveMode", SaveMode.ON_GET_ATTRIBUTE);
				});
	}

	@Configuration
	static class SessionDataSourceConfiguration {

		@Bean
		@SpringSessionDataSource
		DataSource sessionDataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
			dataSource.setUrl("jdbc:hsqldb:mem:sessiondb");
			dataSource.setUsername("sa");
			return dataSource;
		}

		@Bean
		@Primary
		DataSource mainDataSource() {
			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
			dataSource.setUrl("jdbc:hsqldb:mem:maindb");
			dataSource.setUsername("sa");
			return dataSource;
		}

	}

}
