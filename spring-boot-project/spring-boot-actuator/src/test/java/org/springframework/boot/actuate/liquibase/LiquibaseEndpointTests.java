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

package org.springframework.boot.actuate.liquibase;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.liquibase.LiquibaseEndpoint.LiquibaseBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LiquibaseEndpoint}.
 *
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Leo Li
 */
class LiquibaseEndpointTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(DataSourceAutoConfiguration.class, LiquibaseAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void liquibaseReportIsReturned() {
		this.contextRunner.withUserConfiguration(Config.class).run((context) -> {
			Map<String, LiquibaseBean> liquibaseBeans = context.getBean(LiquibaseEndpoint.class).liquibaseBeans()
					.getContexts().get(context.getId()).getLiquibaseBeans();
			assertThat(liquibaseBeans.get("liquibase").getChangeSets()).hasSize(1);
		});
	}

	@Test
	void liquibaseReportIsReturnedForContextHierarchy() {
		this.contextRunner.withUserConfiguration().run((parent) -> {
			this.contextRunner.withUserConfiguration(Config.class).withParent(parent).run((context) -> {
				Map<String, LiquibaseBean> liquibaseBeans = context.getBean(LiquibaseEndpoint.class).liquibaseBeans()
						.getContexts().get(parent.getId()).getLiquibaseBeans();
				assertThat(liquibaseBeans.get("liquibase").getChangeSets()).hasSize(1);
			});
		});
	}

	@Test
	void invokeWithCustomSchema() {
		this.contextRunner.withUserConfiguration(Config.class, DataSourceWithSchemaConfiguration.class)
				.withPropertyValues("spring.liquibase.default-schema=CUSTOMSCHEMA").run((context) -> {
					Map<String, LiquibaseBean> liquibaseBeans = context.getBean(LiquibaseEndpoint.class)
							.liquibaseBeans().getContexts().get(context.getId()).getLiquibaseBeans();
					assertThat(liquibaseBeans.get("liquibase").getChangeSets()).hasSize(1);
				});
	}

	@Test
	void invokeWithCustomTables() {
		this.contextRunner.withUserConfiguration(Config.class)
				.withPropertyValues("spring.liquibase.database-change-log-lock-table=liquibase_database_changelog_lock",
						"spring.liquibase.database-change-log-table=liquibase_database_changelog")
				.run((context) -> {
					Map<String, LiquibaseBean> liquibaseBeans = context.getBean(LiquibaseEndpoint.class)
							.liquibaseBeans().getContexts().get(context.getId()).getLiquibaseBeans();
					assertThat(liquibaseBeans.get("liquibase").getChangeSets()).hasSize(1);
				});
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
	void whenMultipleLiquibaseBeansArePresentChangeSetsAreCorrectlyReportedForEachBean() {
		this.contextRunner.withUserConfiguration(Config.class, MultipleDataSourceLiquibaseConfiguration.class)
				.run((context) -> {
					Map<String, LiquibaseBean> liquibaseBeans = context.getBean(LiquibaseEndpoint.class)
							.liquibaseBeans().getContexts().get(context.getId()).getLiquibaseBeans();
					assertThat(liquibaseBeans.get("liquibase").getChangeSets()).hasSize(1);
					assertThat(liquibaseBeans.get("liquibase").getChangeSets().get(0).getChangeLog())
							.isEqualTo("db/changelog/db.changelog-master.yaml");
					assertThat(liquibaseBeans.get("liquibaseBackup").getChangeSets()).hasSize(1);
					assertThat(liquibaseBeans.get("liquibaseBackup").getChangeSets().get(0).getChangeLog())
							.isEqualTo("db/changelog/db.changelog-master-backup.yaml");
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
			DataSource dataSource = new EmbeddedDatabaseBuilder()
					.setType(EmbeddedDatabaseConnection.get(getClass().getClassLoader()).getType())
					.setName(UUID.randomUUID().toString()).build();
			DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
			settings.setSchemaLocations(Arrays.asList("classpath:/db/create-custom-schema.sql"));
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
			return new EmbeddedDatabaseBuilder().generateUniqueName(true)
					.setType(EmbeddedDatabaseConnection.HSQLDB.getType()).build();
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
