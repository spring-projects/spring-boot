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

package org.springframework.boot.liquibase.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.Liquibase;
import liquibase.UpdateSummaryEnum;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.command.core.helpers.ShowSummaryArgument;
import liquibase.integration.spring.Customizer;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.ui.UIServiceEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.EmbeddedDataSourceConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration.LiquibaseAutoConfigurationRuntimeHints;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link LiquibaseAutoConfiguration}.
 *
 * @author Marcel Overdijk
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Dominic Gunn
 * @author András Deák
 * @author Andrii Hrytsiuk
 * @author Ferenc Gratzer
 * @author Evgeniy Cheban
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author Ahmed Ashour
 */
@ExtendWith(OutputCaptureExtension.class)
class LiquibaseAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void backsOffWithNoDataSourceBeanAndNoLiquibaseUrl() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(SpringLiquibase.class));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void createsDataSourceWithNoDataSourceBeanAndLiquibaseUrl() {
		String jdbcUrl = "jdbc:h2:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withPropertyValues("spring.liquibase.url:" + jdbcUrl).run(assertLiquibase((liquibase) -> {
			SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
			assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
		}));
	}

	@Test
	void backsOffWithLiquibaseUrlAndNoSpringJdbc() {
		this.contextRunner.withPropertyValues("spring.liquibase.url:jdbc:h2:mem:" + UUID.randomUUID())
			.withClassLoader(new FilteredClassLoader("org.springframework.jdbc"))
			.run((context) -> assertThat(context).doesNotHaveBean(SpringLiquibase.class));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void defaultSpringLiquibase() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				assertThat(liquibase.getChangeLog()).isEqualTo("classpath:/db/changelog/db.changelog-master.yaml");
				assertThat(liquibase.getContexts()).isNull();
				assertThat(liquibase.getDefaultSchema()).isNull();
				assertThat(liquibase.isDropFirst()).isFalse();
				assertThat(liquibase.isClearCheckSums()).isFalse();
			}));
	}

	@Test
	void shouldUseMainDataSourceWhenThereIsNoLiquibaseSpecificConfiguration() {
		this.contextRunner.withSystemProperties("shouldRun=false")
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JdbcConnectionDetailsConfiguration.class)
			.run((context) -> {
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				assertThat(liquibase.getDataSource()).isSameAs(context.getBean(DataSource.class));
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void liquibaseDataSourceIsUsedOverJdbcConnectionDetails() {
		this.contextRunner
			.withUserConfiguration(LiquibaseDataSourceConfiguration.class, JdbcConnectionDetailsConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				HikariDataSource dataSource = (HikariDataSource) liquibase.getDataSource();
				assertThat(dataSource.getJdbcUrl()).startsWith("jdbc:h2:mem:liquibasetest");
				assertThat(dataSource.getUsername()).isEqualTo("sa");
				assertThat(dataSource.getPassword()).isNull();
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void liquibaseDataSourceIsUsedOverLiquibaseConnectionDetails() {
		this.contextRunner
			.withUserConfiguration(LiquibaseDataSourceConfiguration.class,
					LiquibaseConnectionDetailsConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				HikariDataSource dataSource = (HikariDataSource) liquibase.getDataSource();
				assertThat(dataSource.getJdbcUrl()).startsWith("jdbc:h2:mem:liquibasetest");
				assertThat(dataSource.getUsername()).isEqualTo("sa");
				assertThat(dataSource.getPassword()).isNull();
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void liquibasePropertiesAreUsedOverJdbcConnectionDetails() {
		this.contextRunner
			.withPropertyValues("spring.liquibase.url=jdbc:h2:mem:liquibasetest", "spring.liquibase.user=some-user",
					"spring.liquibase.password=some-password", "spring.liquibase.driver-class-name=org.h2.Driver")
			.withUserConfiguration(JdbcConnectionDetailsConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).startsWith("jdbc:h2:mem:liquibasetest");
				assertThat(dataSource.getUsername()).isEqualTo("some-user");
				assertThat(dataSource.getPassword()).isEqualTo("some-password");
			}));
	}

	@Test
	void liquibaseConnectionDetailsAreUsedOverLiquibaseProperties() {
		this.contextRunner.withSystemProperties("shouldRun=false")
			.withPropertyValues("spring.liquibase.url=jdbc:h2:mem:liquibasetest", "spring.liquibase.user=some-user",
					"spring.liquibase.password=some-password", "spring.liquibase.driver-class-name=org.h2.Driver")
			.withUserConfiguration(LiquibaseConnectionDetailsConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).isEqualTo("jdbc:postgresql://database.example.com:12345/database-1");
				assertThat(dataSource.getUsername()).isEqualTo("user-1");
				assertThat(dataSource.getPassword()).isEqualTo("secret-1");
			}));
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-override.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8"?>
					<databaseChangeLog
					        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
					        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
					        xmlns:ext="http://www.liquibase.org/xml/ns/dbchangelog-ext"
					        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd
					        http://www.liquibase.org/xml/ns/dbchangelog-ext http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-ext.xsd">

					</databaseChangeLog>
					""")
	void changelogXml() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.xml")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.xml")));
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-override.json", content = """
			{
			    "databaseChangeLog": []
			}
			""")
	void changelogJson() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.json")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.json")));
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-override.sql", content = """
			--liquibase formatted sql

			--changeset author:awilkinson

			CREATE TABLE customer (
			    id int AUTO_INCREMENT NOT NULL PRIMARY KEY,
			    name varchar(50) NOT NULL
			);
			""")
	void changelogSql() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.sql")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
				.isEqualTo("classpath:/db/changelog/db.changelog-override.sql")));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void defaultValues() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.run(assertLiquibase((liquibase) -> {
				LiquibaseProperties properties = new LiquibaseProperties();
				assertThat(liquibase.getDatabaseChangeLogTable()).isEqualTo(properties.getDatabaseChangeLogTable());
				assertThat(liquibase.getDatabaseChangeLogLockTable())
					.isEqualTo(properties.getDatabaseChangeLogLockTable());
				assertThat(liquibase.isDropFirst()).isEqualTo(properties.isDropFirst());
				assertThat(liquibase.isClearCheckSums()).isEqualTo(properties.isClearChecksums());
				assertThat(liquibase.isTestRollbackOnUpdate()).isEqualTo(properties.isTestRollbackOnUpdate());
				assertThat(liquibase).extracting("showSummary").isNull();
				assertThat(ShowSummaryArgument.SHOW_SUMMARY.getDefaultValue()).isEqualTo(UpdateSummaryEnum.SUMMARY);
				assertThat(liquibase).extracting("showSummaryOutput").isEqualTo(UpdateSummaryOutputEnum.LOG);
				assertThat(liquibase).extracting("uiService").isEqualTo(UIServiceEnum.LOGGER);
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideContexts() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.contexts:test, production")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getContexts()).isEqualTo("test,production")));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideDefaultSchema() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.default-schema:public")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getDefaultSchema()).isEqualTo("public")));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideLiquibaseInfrastructure() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.liquibase-schema:public",
					"spring.liquibase.liquibase-tablespace:infra",
					"spring.liquibase.database-change-log-table:LIQUI_LOG",
					"spring.liquibase.database-change-log-lock-table:LIQUI_LOCK")
			.run((context) -> {
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				assertThat(liquibase.getLiquibaseSchema()).isEqualTo("public");
				assertThat(liquibase.getLiquibaseTablespace()).isEqualTo("infra");
				assertThat(liquibase.getDatabaseChangeLogTable()).isEqualTo("LIQUI_LOG");
				assertThat(liquibase.getDatabaseChangeLogLockTable()).isEqualTo("LIQUI_LOCK");
				JdbcTemplate jdbcTemplate = new JdbcTemplate(context.getBean(DataSource.class));
				assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.LIQUI_LOG", Integer.class)).isOne();
				assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.LIQUI_LOCK", Integer.class))
					.isOne();
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideDropFirst() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.drop-first:true")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.isDropFirst()).isTrue()));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideClearChecksums() {
		String jdbcUrl = "jdbc:h2:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.url:" + jdbcUrl)
			.run((context) -> assertThat(context).hasNotFailed());
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.clear-checksums:true", "spring.liquibase.url:" + jdbcUrl)
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.isClearCheckSums()).isTrue()));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideDataSource() {
		String jdbcUrl = "jdbc:h2:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.url:" + jdbcUrl)
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
				Driver driver = dataSource.getDriver();
				assertThat(driver).isNotNull();
				assertThat(driver.getClass().getName()).isEqualTo("org.h2.Driver");
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideDataSourceAndDriverClassName() {
		String jdbcUrl = "jdbc:h2:mem:liquibase" + UUID.randomUUID();
		String driverClassName = "org.h2.Driver";
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.url:" + jdbcUrl,
					"spring.liquibase.driver-class-name:" + driverClassName)
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
				Driver driver = dataSource.getDriver();
				assertThat(driver).isNotNull();
				assertThat(driver.getClass().getName()).isEqualTo(driverClassName);
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideUser() {
		String databaseName = "normal" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.generate-unique-name:false",
					"spring.datasource.name:" + databaseName, "spring.datasource.username:not-sa",
					"spring.liquibase.user:sa")
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).contains("jdbc:h2:mem:" + databaseName);
				assertThat(dataSource.getUsername()).isEqualTo("sa");
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideUserWhenCustom() {
		this.contextRunner.withUserConfiguration(CustomDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.user:test", "spring.liquibase.password:secret")
			.run((context) -> {
				String expectedName = context.getBean(CustomDataSourceConfiguration.class).name;
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).contains(expectedName);
				assertThat(dataSource.getUsername()).isEqualTo("test");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void createDataSourceDoesNotFallbackToEmbeddedProperties() {
		String jdbcUrl = "jdbc:h2:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.url:" + jdbcUrl)
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUsername()).isNull();
				assertThat(dataSource.getPassword()).isNull();
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideUserAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.user:sa")
			.run(assertLiquibase((liquibase) -> {
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
				assertThat(dataSource.getUrl()).startsWith("jdbc:h2:mem:");
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideTestRollbackOnUpdate() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.test-rollback-on-update:true")
			.run((context) -> {
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				assertThat(liquibase.isTestRollbackOnUpdate()).isTrue();
			});
	}

	@Test
	void changeLogDoesNotExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.change-log:classpath:/no-such-changelog.yaml")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void logging(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.run(assertLiquibase((liquibase) -> assertThat(output).doesNotContain(": liquibase:")));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideLabelFilter() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.label-filter:test, production")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getLabelFilter()).isEqualTo("test,production")));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideShowSummary() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.show-summary=off")
			.run(assertLiquibase((liquibase) -> {
				UpdateSummaryEnum showSummary = (UpdateSummaryEnum) ReflectionTestUtils.getField(liquibase,
						"showSummary");
				assertThat(showSummary).isEqualTo(UpdateSummaryEnum.OFF);
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideShowSummaryOutput() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.show-summary-output=all")
			.run(assertLiquibase((liquibase) -> {
				UpdateSummaryOutputEnum showSummaryOutput = (UpdateSummaryOutputEnum) ReflectionTestUtils
					.getField(liquibase, "showSummaryOutput");
				assertThat(showSummaryOutput).isEqualTo(UpdateSummaryOutputEnum.ALL);
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideUiService() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.ui-service=console")
			.run(assertLiquibase(
					(liquibase) -> assertThat(liquibase).extracting("uiService").isEqualTo(UIServiceEnum.CONSOLE)));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	@SuppressWarnings("unchecked")
	void testOverrideParameters() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.parameters.foo:bar")
			.run(assertLiquibase((liquibase) -> {
				Map<String, String> parameters = (Map<String, String>) ReflectionTestUtils.getField(liquibase,
						"parameters");
				assertThat(parameters).containsKey("foo");
				assertThat(parameters).containsEntry("foo", "bar");
			}));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void rollbackFile(@TempDir Path temp) throws IOException {
		File file = Files.createTempFile(temp, "rollback-file", "sql").toFile();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.rollback-file:" + file.getAbsolutePath())
			.run((context) -> {
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				File actualFile = (File) ReflectionTestUtils.getField(liquibase, "rollbackFile");
				assertThat(actualFile).isEqualTo(file).exists();
				assertThat(contentOf(file)).contains("DROP TABLE PUBLIC.customer;");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void liquibaseDataSource() {
		this.contextRunner
			.withUserConfiguration(LiquibaseDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
				assertThat(liquibase.getDataSource()).isEqualTo(context.getBean("liquibaseDataSource"));
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void liquibaseDataSourceWithoutDataSourceAutoConfiguration() {
		this.contextRunner.withUserConfiguration(LiquibaseDataSourceConfiguration.class).run((context) -> {
			SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
			assertThat(liquibase.getDataSource()).isEqualTo(context.getBean("liquibaseDataSource"));
		});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void userConfigurationBeans() {
		this.contextRunner
			.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasBean("springLiquibase");
				assertThat(context).doesNotHaveBean("liquibase");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void userConfigurationJdbcTemplateDependency() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
			.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("jdbcTemplate");
				assertThat(beanDefinition.getDependsOn()).containsExactly("springLiquibase");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void overrideTag() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.tag:1.0.0")
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getTag()).isEqualTo("1.0.0")));
	}

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = new RuntimeHints();
		new LiquibaseAutoConfigurationRuntimeHints().registerHints(hints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("db/changelog/")).accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("db/changelog/db.changelog-master.yaml"))
			.accepts(hints);
		assertThat(RuntimeHintsPredicates.resource().forResource("db/changelog/tables/init.sql")).accepts(hints);
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void springLiquibaseTakesPrecedenceOverLiquibaseDefaults() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.properties.liquibase.duplicateFileMode=WARN")
			.run((context) -> {
				assertThat(context).hasSingleBean(SpringLiquibase.class);

				var liquibaseConfig = liquibase.Scope.getCurrentScope()
					.getSingleton(liquibase.configuration.LiquibaseConfiguration.class);

				var provider = liquibaseConfig.getProviders()
					.stream()
					.filter(p -> p.getClass()
						.getName()
						.equals("org.springframework.boot.liquibase.autoconfigure.EnvironmentConfigurationValueProvider"))
					.findFirst()
					.orElseThrow();

				var provided = provider.getProvidedValue("liquibase.duplicateFileMode");
				assertThat(provided).isNotNull();
				assertThat(String.valueOf(provided.getValue())).isEqualTo("WARN");

				// Now check the resolved value, which should be different from the
				// default 'ERROR'
				var resolved = liquibaseConfig.getCurrentConfiguredValue(null, null, "liquibase.duplicateFileMode");
				assertThat(String.valueOf(resolved.getValue())).isEqualTo("WARN");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void systemPropertyTakesPrecedenceOverSpringLiquibaseProperties() {
		this.contextRunner.withSystemProperties("liquibase.duplicateFileMode=ERROR")
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.properties.liquibase.duplicateFileMode=WARN")
			.run((context) -> {
				assertThat(context).hasSingleBean(SpringLiquibase.class);

				var liquibaseConfig = liquibase.Scope.getCurrentScope()
					.getSingleton(liquibase.configuration.LiquibaseConfiguration.class);

				var provider = liquibaseConfig.getProviders()
					.stream()
					.filter(p -> p.getClass()
						.getName()
						.equals("org.springframework.boot.liquibase.autoconfigure.EnvironmentConfigurationValueProvider"))
					.findFirst()
					.orElseThrow();

				// Our provider should return the value set through Spring property
				var providedBySpring = provider.getProvidedValue("liquibase.duplicateFileMode");
				assertThat(providedBySpring).isNotNull();
				assertThat(String.valueOf(providedBySpring.getValue())).isEqualTo("WARN");

				// Now check the resolved value, which should be the system property
				var resolved = liquibaseConfig.getCurrentConfiguredValue(null, null, "liquibase.duplicateFileMode");
				assertThat(String.valueOf(resolved.getValue())).isEqualTo("ERROR");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void arbitraryLiquibaseKeyIsPassedThroughAsIs() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.properties.my.extension.custom.option=true")
			.run((context) -> {
				assertThat(context).hasSingleBean(SpringLiquibase.class);

				var liquibaseConfig = liquibase.Scope.getCurrentScope()
					.getSingleton(liquibase.configuration.LiquibaseConfiguration.class);

				var provider = liquibaseConfig.getProviders()
					.stream()
					.filter(p -> p.getClass()
						.getName()
						.equals("org.springframework.boot.liquibase.autoconfigure.EnvironmentConfigurationValueProvider"))
					.findFirst()
					.orElseThrow();

				var provided = provider.getProvidedValue("my.extension.custom.option");
				assertThat(provided).isNotNull();
				assertThat(String.valueOf(provided.getValue())).isEqualTo("true");
			});
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void whenCustomizerBeanIsDefinedThenItIsConfiguredOnSpringLiquibase() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomizerConfiguration.class)
			.run(assertLiquibase((liquibase) -> assertThat(liquibase.getCustomizer()).isNotNull()));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void whenAnalyticsEnabledIsFalseThenSpringLiquibaseHasAnalyticsDisabled() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.analytics-enabled=false")
			.run((context) -> assertThat(context.getBean(SpringLiquibase.class))
				.extracting(SpringLiquibase::getAnalyticsEnabled)
				.isEqualTo(Boolean.FALSE));
	}

	@Test
	@WithDbChangelogMasterYamlResource
	void whenLicenseKeyIsSetThenSpringLiquibaseHasLicenseKey() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.liquibase.license-key=a1b2c3d4")
			.run((context) -> assertThat(context.getBean(SpringLiquibase.class))
				.extracting(SpringLiquibase::getLicenseKey)
				.isEqualTo("a1b2c3d4"));
	}

	private ContextConsumer<AssertableApplicationContext> assertLiquibase(Consumer<SpringLiquibase> consumer) {
		return (context) -> {
			assertThat(context).hasSingleBean(SpringLiquibase.class);
			SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
			consumer.accept(liquibase);
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class LiquibaseDataSourceConfiguration {

		@Bean
		@Primary
		DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:h2:mem:normal" + UUID.randomUUID()).username("sa").build();
		}

		@LiquibaseDataSource
		@Bean
		DataSource liquibaseDataSource() {
			return DataSourceBuilder.create()
				.url("jdbc:h2:mem:liquibasetest" + UUID.randomUUID())
				.username("sa")
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LiquibaseUserConfiguration {

		@Bean
		SpringLiquibase springLiquibase(DataSource dataSource) {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
			liquibase.setShouldRun(true);
			liquibase.setDataSource(dataSource);
			return liquibase;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDataSourceConfiguration {

		private final String name = UUID.randomUUID().toString();

		@Bean(destroyMethod = "shutdown")
		EmbeddedDatabase dataSource() throws SQLException {
			EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.setName(this.name)
				.build();
			insertUser(database);
			return database;
		}

		private void insertUser(EmbeddedDatabase database) throws SQLException {
			try (Connection connection = database.getConnection()) {
				connection.prepareStatement("CREATE USER test password 'secret'").execute();
				connection.prepareStatement("ALTER USER test ADMIN TRUE").execute();
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDriverConfiguration {

		private final String name = UUID.randomUUID().toString();

		@Bean
		SimpleDriverDataSource dataSource() {
			SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
			dataSource.setDriverClass(CustomH2Driver.class);
			dataSource.setUrl(String.format("jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false", this.name));
			dataSource.setUsername("sa");
			dataSource.setPassword("");
			return dataSource;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JdbcConnectionDetailsConfiguration {

		@Bean
		JdbcConnectionDetails jdbcConnectionDetails() {
			return new JdbcConnectionDetails() {

				@Override
				public String getJdbcUrl() {
					return "jdbc:postgresql://database.example.com:12345/database-1";
				}

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class LiquibaseConnectionDetailsConfiguration {

		@Bean
		LiquibaseConnectionDetails liquibaseConnectionDetails() {
			return new LiquibaseConnectionDetails() {

				@Override
				public String getJdbcUrl() {
					return "jdbc:postgresql://database.example.com:12345/database-1";
				}

				@Override
				public String getUsername() {
					return "user-1";
				}

				@Override
				public String getPassword() {
					return "secret-1";
				}

			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		Customizer<Liquibase> customizer() {
			return (liquibase) -> liquibase.setChangeLogParameter("some key", "some value");
		}

	}

	static class CustomH2Driver extends org.h2.Driver {

	}

	@WithResource(name = "db/changelog/db.changelog-master.yaml", content = """
			databaseChangeLog:
			  - changeSet:
			      id: 1
			      author: marceloverdijk
			      changes:
			        - createTable:
			            tableName: customer
			            columns:
			              - column:
			                  name: id
			                  type: int
			                  autoIncrement: true
			                  constraints:
			                    primaryKey: true
			                    nullable: false
			              - column:
			                  name: name
			                  type: varchar(50)
			                  constraints:
			                    nullable: false
			""")
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@interface WithDbChangelogMasterYamlResource {

	}

}
