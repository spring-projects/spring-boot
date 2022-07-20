/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.session.JdbcSessionConfiguration.SpringBootJdbcHttpSessionConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
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
			.withClassLoader(new FilteredClassLoader(HazelcastIndexedSessionRepository.class,
					MongoIndexedSessionRepository.class, RedisIndexedSessionRepository.class))
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					DataSourceTransactionManagerAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
					SessionAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void defaultConfig() {
		this.contextRunner.run(this::validateDefaultConfig);
	}

	@Test
	void jdbcTakesPrecedenceOverMongoAndHazelcast() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(RedisIndexedSessionRepository.class))
				.run(this::validateDefaultConfig);
	}

	private void validateDefaultConfig(AssertableWebApplicationContext context) {
		JdbcIndexedSessionRepository repository = validateSessionRepository(context,
				JdbcIndexedSessionRepository.class);
		assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
				(int) new ServerProperties().getServlet().getSession().getTimeout().getSeconds());
		assertThat(repository).hasFieldOrPropertyWithValue("tableName", "SPRING_SESSION");
		assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
				.isEqualTo(DatabaseInitializationMode.EMBEDDED);
		assertThat(context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION")).isEmpty();
		SpringBootJdbcHttpSessionConfiguration configuration = context
				.getBean(SpringBootJdbcHttpSessionConfiguration.class);
		assertThat(configuration).hasFieldOrPropertyWithValue("cleanupCron", "0 * * * * *");
	}

	@Test
	void filterOrderCanBeCustomized() {
		this.contextRunner.withPropertyValues("spring.session.servlet.filter-order=123").run((context) -> {
			FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration.getOrder()).isEqualTo(123);
		});
	}

	@Test
	void disableDataSourceInitializer() {
		this.contextRunner.withPropertyValues("spring.session.jdbc.initialize-schema=never").run((context) -> {
			assertThat(context).doesNotHaveBean(JdbcSessionDataSourceScriptDatabaseInitializer.class);
			JdbcIndexedSessionRepository repository = validateSessionRepository(context,
					JdbcIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("tableName", "SPRING_SESSION");
			assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
					.isEqualTo(DatabaseInitializationMode.NEVER);
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(
					() -> context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION"));
		});
	}

	@Test
	void customTimeout() {
		this.contextRunner.withPropertyValues("spring.session.timeout=1m").run((context) -> {
			JdbcIndexedSessionRepository repository = validateSessionRepository(context,
					JdbcIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", 60);
		});
	}

	@Test
	void customTableName() {
		this.contextRunner.withPropertyValues("spring.session.jdbc.table-name=FOO_BAR",
				"spring.session.jdbc.schema=classpath:session/custom-schema-h2.sql").run((context) -> {
					JdbcIndexedSessionRepository repository = validateSessionRepository(context,
							JdbcIndexedSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("tableName", "FOO_BAR");
					assertThat(context.getBean(JdbcSessionProperties.class).getInitializeSchema())
							.isEqualTo(DatabaseInitializationMode.EMBEDDED);
					assertThat(context.getBean(JdbcOperations.class).queryForList("select * from FOO_BAR")).isEmpty();
				});
	}

	@Test
	void customCleanupCron() {
		this.contextRunner.withPropertyValues("spring.session.jdbc.cleanup-cron=0 0 12 * * *").run((context) -> {
			assertThat(context.getBean(JdbcSessionProperties.class).getCleanupCron()).isEqualTo("0 0 12 * * *");
			SpringBootJdbcHttpSessionConfiguration configuration = context
					.getBean(SpringBootJdbcHttpSessionConfiguration.class);
			assertThat(configuration).hasFieldOrPropertyWithValue("cleanupCron", "0 0 12 * * *");
		});
	}

	@Test
	void customFlushMode() {
		this.contextRunner.withPropertyValues("spring.session.jdbc.flush-mode=immediate").run((context) -> {
			assertThat(context.getBean(JdbcSessionProperties.class).getFlushMode()).isEqualTo(FlushMode.IMMEDIATE);
			SpringBootJdbcHttpSessionConfiguration configuration = context
					.getBean(SpringBootJdbcHttpSessionConfiguration.class);
			assertThat(configuration).hasFieldOrPropertyWithValue("flushMode", FlushMode.IMMEDIATE);
		});
	}

	@Test
	void customSaveMode() {
		this.contextRunner.withPropertyValues("spring.session.jdbc.save-mode=on-get-attribute").run((context) -> {
			assertThat(context.getBean(JdbcSessionProperties.class).getSaveMode()).isEqualTo(SaveMode.ON_GET_ATTRIBUTE);
			SpringBootJdbcHttpSessionConfiguration configuration = context
					.getBean(SpringBootJdbcHttpSessionConfiguration.class);
			assertThat(configuration).hasFieldOrPropertyWithValue("saveMode", SaveMode.ON_GET_ATTRIBUTE);
		});
	}

	@Test
	void sessionDataSourceIsUsedWhenAvailable() {
		this.contextRunner.withUserConfiguration(SessionDataSourceConfiguration.class).run((context) -> {
			JdbcIndexedSessionRepository repository = validateSessionRepository(context,
					JdbcIndexedSessionRepository.class);
			DataSource sessionDataSource = context.getBean("sessionDataSource", DataSource.class);
			assertThat(repository).extracting("jdbcOperations.dataSource").isEqualTo(sessionDataSource);
			assertThat(context.getBean(JdbcSessionDataSourceScriptDatabaseInitializer.class))
					.hasFieldOrPropertyWithValue("dataSource", sessionDataSource);
			assertThatExceptionOfType(BadSqlGrammarException.class).isThrownBy(
					() -> context.getBean(JdbcOperations.class).queryForList("select * from SPRING_SESSION"));
		});
	}

	@Test
	void sessionRepositoryBeansDependOnJdbcSessionDataSourceInitializer() {
		this.contextRunner.run((context) -> {
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			String[] sessionRepositoryNames = beanFactory.getBeanNamesForType(JdbcIndexedSessionRepository.class);
			assertThat(sessionRepositoryNames).isNotEmpty();
			for (String sessionRepositoryName : sessionRepositoryNames) {
				assertThat(beanFactory.getBeanDefinition(sessionRepositoryName).getDependsOn())
						.contains("jdbcSessionDataSourceScriptDatabaseInitializer");
			}
		});
	}

	@Test
	void sessionRepositoryBeansDependOnFlyway() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.withPropertyValues("spring.session.jdbc.initialize-schema=never").run((context) -> {
					ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
					String[] sessionRepositoryNames = beanFactory
							.getBeanNamesForType(JdbcIndexedSessionRepository.class);
					assertThat(sessionRepositoryNames).isNotEmpty();
					for (String sessionRepositoryName : sessionRepositoryNames) {
						assertThat(beanFactory.getBeanDefinition(sessionRepositoryName).getDependsOn())
								.contains("flyway", "flywayInitializer");
					}
				});
	}

	@Test
	void sessionRepositoryBeansDependOnLiquibase() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
				.withPropertyValues("spring.session.jdbc.initialize-schema=never").run((context) -> {
					ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
					String[] sessionRepositoryNames = beanFactory
							.getBeanNamesForType(JdbcIndexedSessionRepository.class);
					assertThat(sessionRepositoryNames).isNotEmpty();
					for (String sessionRepositoryName : sessionRepositoryNames) {
						assertThat(beanFactory.getBeanDefinition(sessionRepositoryName).getDependsOn())
								.contains("liquibase");
					}
				});
	}

	@Test
	void whenTheUserDefinesTheirOwnJdbcSessionDatabaseInitializerThenTheAutoConfiguredInitializerBacksOff() {
		this.contextRunner.withUserConfiguration(CustomJdbcSessionDatabaseInitializerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.hasSingleBean(JdbcSessionDataSourceScriptDatabaseInitializer.class)
						.doesNotHaveBean("jdbcSessionDataSourceScriptDatabaseInitializer")
						.hasBean("customInitializer"));
	}

	@Test
	void whenTheUserDefinesTheirOwnDatabaseInitializerThenTheAutoConfiguredJdbcSessionInitializerRemains() {
		this.contextRunner.withUserConfiguration(CustomDatabaseInitializerConfiguration.class)
				.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
						DataSourceTransactionManagerAutoConfiguration.class))
				.run((context) -> assertThat(context)
						.hasSingleBean(JdbcSessionDataSourceScriptDatabaseInitializer.class)
						.hasBean("customInitializer"));
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

	@Configuration(proxyBeanMethods = false)
	static class CustomJdbcSessionDatabaseInitializerConfiguration {

		@Bean
		JdbcSessionDataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource,
				JdbcSessionProperties properties) {
			return new JdbcSessionDataSourceScriptDatabaseInitializer(dataSource, properties);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDatabaseInitializerConfiguration {

		@Bean
		DataSourceScriptDatabaseInitializer customInitializer(DataSource dataSource) {
			return new DataSourceScriptDatabaseInitializer(dataSource, new DatabaseInitializationSettings());
		}

	}

}
