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

package org.springframework.boot.liquibase.actuate.endpoint;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.liquibase.actuate.endpoint.LiquibaseEndpoint.ContextLiquibaseBeansDescriptor;
import org.springframework.boot.liquibase.actuate.endpoint.LiquibaseEndpoint.LiquibaseBeanDescriptor;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseEndpoint}.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Leo Li
 */
@WithResource(name = "db/changelog/db.changelog-master.yaml", content = """
		databaseChangeLog:
		  - changeSet:
		      id: 1
		      author: test
		""")
class LiquibaseEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void liquibaseReportIsReturned() {
		this.contextRunner.withUserConfiguration(Config.class).run(hasEndpointWithInitializedSchema());
	}

	@Test
	void liquibaseReportIsReturnedForContextHierarchy() {
		this.contextRunner.withUserConfiguration().run((parent) -> {
			this.contextRunner.withUserConfiguration(Config.class).withParent(parent).run((context) -> {
				Map<@Nullable String, ContextLiquibaseBeansDescriptor> contexts = context
					.getBean(LiquibaseEndpoint.class)
					.liquibaseBeans()
					.getContexts();
				ContextLiquibaseBeansDescriptor parentContext = contexts.get(parent.getId());
				assertThat(parentContext).isNotNull();
				Map<String, LiquibaseBeanDescriptor> liquibaseBeans = parentContext.getLiquibaseBeans();
				assertThat(liquibaseBeans).hasEntrySatisfying("liquibase",
						(descriptor) -> assertThat(descriptor.getChangeSets()).hasSize(1));
			});
		});
	}

	@Test
	@WithResource(name = "db/create-custom-schema.sql", content = "CREATE SCHEMA ANOTHER_SCHEMA;")
	void invokeWithCustomDefaultSchemaFailsIfItDoesNotExist() {
		this.contextRunner.withUserConfiguration(Config.class, DataSourceWithSchemaConfiguration.class)
			.withPropertyValues("spring.liquibase.default-schema=CUSTOM_DEFAULT_SCHEMA")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("CUSTOM_DEFAULT_SCHEMA"));
	}

	@Test
	@WithResource(name = "db/create-custom-schema.sql", content = "CREATE SCHEMA CUSTOM_SCHEMA;")
	void invokeWithCustomDefaultSchema() {
		this.contextRunner.withUserConfiguration(Config.class, DataSourceWithSchemaConfiguration.class)
			.withPropertyValues("spring.liquibase.default-schema=CUSTOM_SCHEMA")
			.run(hasEndpointWithInitializedSchema());
	}

	@Test
	@WithResource(name = "db/create-custom-schema.sql", content = "CREATE SCHEMA ANOTHER_SCHEMA;")
	void invokeWithLiquibaseSchemaFailsIfItDoesNotExist() {
		this.contextRunner.withUserConfiguration(Config.class, DataSourceWithSchemaConfiguration.class)
			.withPropertyValues("spring.liquibase.liquibase-schema=CUSTOM_LIQUIBASE_SCHEMA")
			.run((context) -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("CUSTOM_LIQUIBASE_SCHEMA"));
	}

	@Test
	@WithResource(name = "db/create-custom-schema.sql", content = "CREATE SCHEMA LIQUIBASE_SCHEMA;")
	void invokeWithLiquibaseSchema() {
		this.contextRunner.withUserConfiguration(Config.class, DataSourceWithSchemaConfiguration.class)
			.withPropertyValues("spring.liquibase.liquibase-schema=LIQUIBASE_SCHEMA")
			.run(hasEndpointWithInitializedSchema());
	}

	@Test
	void invokeWithCustomTables() {
		this.contextRunner.withUserConfiguration(Config.class)
			.withPropertyValues("spring.liquibase.database-change-log-lock-table=liquibase_database_changelog_lock",
					"spring.liquibase.database-change-log-table=liquibase_database_changelog")
			.run(hasEndpointWithInitializedSchema());
	}

	private ContextConsumer<AssertableApplicationContext> hasEndpointWithInitializedSchema() {
		return (context) -> {
			ContextLiquibaseBeansDescriptor contextDescriptor = context.getBean(LiquibaseEndpoint.class)
				.liquibaseBeans()
				.getContexts()
				.get(context.getId());
			assertThat(contextDescriptor).isNotNull();
			Map<String, LiquibaseBeanDescriptor> liquibaseBeans = contextDescriptor.getLiquibaseBeans();
			assertThat(liquibaseBeans).hasEntrySatisfying("liquibase",
					(descriptor) -> assertThat(descriptor.getChangeSets()).hasSize(1));
		};
	}

	@Test
	void connectionAutoCommitPropertyIsReset() {
		this.contextRunner.withUserConfiguration(Config.class).run((context) -> {
			DataSource dataSource = context.getBean(DataSource.class);
			assertThat(getAutoCommit(dataSource)).isTrue();
			context.getBean(LiquibaseEndpoint.class).liquibaseBeans();
			assertThat(getAutoCommit(dataSource)).isTrue();
		});
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-master-backup.yaml", content = """
			databaseChangeLog:
			  - changeSet:
			      id: 1
			      author: test
			""")
	void whenMultipleLiquibaseBeansArePresentChangeSetsAreCorrectlyReportedForEachBean() {
		this.contextRunner.withUserConfiguration(Config.class, MultipleDataSourceLiquibaseConfiguration.class)
			.run((context) -> {
				ContextLiquibaseBeansDescriptor contextDescriptor = context.getBean(LiquibaseEndpoint.class)
					.liquibaseBeans()
					.getContexts()
					.get(context.getId());
				assertThat(contextDescriptor).isNotNull();
				Map<String, LiquibaseBeanDescriptor> liquibaseBeans = contextDescriptor.getLiquibaseBeans();
				assertThat(liquibaseBeans).hasEntrySatisfying("liquibase", (liquibase) -> {
					assertThat(liquibase.getChangeSets()).hasSize(1);
					assertThat(liquibase.getChangeSets().get(0).getChangeLog())
						.isEqualTo("db/changelog/db.changelog-master.yaml");
				});
				assertThat(liquibaseBeans).hasEntrySatisfying("liquibaseBackup", (liquibase) -> {
					assertThat(liquibase.getChangeSets()).hasSize(1);
					assertThat(liquibase.getChangeSets().get(0).getChangeLog())
						.isEqualTo("db/changelog/db.changelog-master-backup.yaml");
				});
			});
	}

	private boolean getAutoCommit(DataSource dataSource) throws SQLException {
		try (Connection connection = dataSource.getConnection()) {
			return connection.getAutoCommit();
		}
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		LiquibaseEndpoint endpoint(ApplicationContext context) {
			return new LiquibaseEndpoint(context);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DataSourceWithSchemaConfiguration {

		@Bean
		DataSource dataSource() {
			DataSource dataSource = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.setName(UUID.randomUUID().toString())
				.build();
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(List.of("classpath:/db/create-custom-schema.sql"));
			DataSourceScriptDatabaseInitializer initializer = new DataSourceScriptDatabaseInitializer(dataSource,
					settings);
			initializer.initializeDatabase();
			return dataSource;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipleDataSourceLiquibaseConfiguration {

		@Bean
		DataSource dataSource() {
			return createEmbeddedDatabase();
		}

		@Bean
		DataSource dataSourceBackup() {
			return createEmbeddedDatabase();
		}

		@Bean
		SpringLiquibase liquibase(DataSource dataSource) {
			return createSpringLiquibase("db.changelog-master.yaml", dataSource);
		}

		@Bean
		SpringLiquibase liquibaseBackup(DataSource dataSourceBackup) {
			return createSpringLiquibase("db.changelog-master-backup.yaml", dataSourceBackup);
		}

		private DataSource createEmbeddedDatabase() {
			return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
		}

		private SpringLiquibase createSpringLiquibase(String changeLog, DataSource dataSource) {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/db/changelog/" + changeLog);
			liquibase.setShouldRun(true);
			liquibase.setDataSource(dataSource);
			return liquibase;
		}

	}

}
