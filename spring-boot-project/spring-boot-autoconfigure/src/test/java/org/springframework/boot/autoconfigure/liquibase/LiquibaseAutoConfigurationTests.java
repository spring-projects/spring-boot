/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.liquibase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import liquibase.integration.spring.SpringLiquibase;
import liquibase.logging.core.Slf4jLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.liquibase.LiquibaseServiceLocatorApplicationListener;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
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
 */
@ExtendWith(OutputCaptureExtension.class)
class LiquibaseAutoConfigurationTests {

	@BeforeEach
	void init() {
		new LiquibaseServiceLocatorApplicationListener()
				.onApplicationEvent(new ApplicationStartingEvent(new SpringApplication(Object.class), new String[0]));
	}

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
			.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void backsOffWithNoDataSourceBeanAndNoLiquibaseUrl() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(SpringLiquibase.class));
	}

	@Test
	void createsDataSourceWithNoDataSourceBeanAndLiquibaseUrl() {
		this.contextRunner.withPropertyValues("spring.liquibase.url:jdbc:hsqldb:mem:liquibase")
				.run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getJdbcUrl()).isEqualTo("jdbc:hsqldb:mem:liquibase");
				}));
	}

	@Test
	void defaultSpringLiquibase() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.run(assertLiquibase((liquibase) -> {
					assertThat(liquibase.getChangeLog()).isEqualTo("classpath:/db/changelog/db.changelog-master.yaml");
					assertThat(liquibase.getContexts()).isNull();
					assertThat(liquibase.getDefaultSchema()).isNull();
					assertThat(liquibase.isDropFirst()).isFalse();
				}));
	}

	@Test
	void changelogXml() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.xml")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
						.isEqualTo("classpath:/db/changelog/db.changelog-override.xml")));
	}

	@Test
	void changelogJson() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.json")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
						.isEqualTo("classpath:/db/changelog/db.changelog-override.json")));
	}

	@Test
	@EnabledOnJre(JRE.JAVA_8)
	void changelogSql() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.change-log:classpath:/db/changelog/db.changelog-override.sql")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getChangeLog())
						.isEqualTo("classpath:/db/changelog/db.changelog-override.sql")));
	}

	@Test
	void defaultValues() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.run(assertLiquibase((liquibase) -> {
					LiquibaseProperties properties = new LiquibaseProperties();
					assertThat(liquibase.getDatabaseChangeLogTable()).isEqualTo(properties.getDatabaseChangeLogTable());
					assertThat(liquibase.getDatabaseChangeLogLockTable())
							.isEqualTo(properties.getDatabaseChangeLogLockTable());
					assertThat(liquibase.isDropFirst()).isEqualTo(properties.isDropFirst());
					assertThat(liquibase.isTestRollbackOnUpdate()).isEqualTo(properties.isTestRollbackOnUpdate());
				}));
	}

	@Test
	void overrideContexts() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.contexts:test, production")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getContexts()).isEqualTo("test, production")));
	}

	@Test
	void overrideDefaultSchema() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.default-schema:public")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getDefaultSchema()).isEqualTo("public")));
	}

	@Test
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
					assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.LIQUI_LOG", Integer.class))
							.isEqualTo(1);
					assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.LIQUI_LOCK", Integer.class))
							.isEqualTo(1);
				});
	}

	@Test
	void overrideDropFirst() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.drop-first:true")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.isDropFirst()).isTrue()));
	}

	@Test
	void overrideDataSource() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:jdbc:hsqldb:mem:liquibase")
				.run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getJdbcUrl()).isEqualTo("jdbc:hsqldb:mem:liquibase");
				}));
	}

	@Test
	void overrideUser() {
		String jdbcUrl = "jdbc:hsqldb:mem:normal";
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.datasource.url:" + jdbcUrl, "spring.datasource.username:not-sa",
						"spring.liquibase.user:sa")
				.run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getJdbcUrl()).isEqualTo(jdbcUrl);
					assertThat(((HikariDataSource) dataSource).getUsername()).isEqualTo("sa");
				}));
	}

	@Test
	void overrideDataSourceAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:jdbc:hsqldb:mem:liquibase")
				.run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getUsername()).isEqualTo("sa");
					assertThat(((HikariDataSource) dataSource).getPassword()).isEqualTo("");
				}));
	}

	@Test
	void overrideUserAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.user:sa").run(assertLiquibase((liquibase) -> {
					DataSource dataSource = liquibase.getDataSource();
					assertThat(((HikariDataSource) dataSource).isClosed()).isTrue();
					assertThat(((HikariDataSource) dataSource).getJdbcUrl()).startsWith("jdbc:h2:mem:");
				}));
	}

	@Test
	void overrideTestRollbackOnUpdate() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.test-rollback-on-update:true").run((context) -> {
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					assertThat(liquibase.isTestRollbackOnUpdate()).isTrue();
				});
	}

	@Test
	void changeLogDoesNotExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.change-log:classpath:/no-such-changelog.yaml").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	void logging(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.run(assertLiquibase((liquibase) -> {
					Object log = ReflectionTestUtils.getField(liquibase, "log");
					assertThat(log).isInstanceOf(Slf4jLogger.class);
					assertThat(output).doesNotContain(": liquibase:");
				}));
	}

	@Test
	void overrideLabels() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.labels:test, production")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getLabels()).isEqualTo("test, production")));
	}

	@Test
	@SuppressWarnings("unchecked")
	void testOverrideParameters() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.parameters.foo:bar").run(assertLiquibase((liquibase) -> {
					Map<String, String> parameters = (Map<String, String>) ReflectionTestUtils.getField(liquibase,
							"parameters");
					assertThat(parameters.containsKey("foo")).isTrue();
					assertThat(parameters.get("foo")).isEqualTo("bar");
				}));
	}

	@Test
	void rollbackFile(@TempDir Path temp) throws IOException {
		File file = Files.createTempFile(temp, "rollback-file", "sql").toFile();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.rollbackFile:" + file.getAbsolutePath()).run((context) -> {
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					File actualFile = (File) ReflectionTestUtils.getField(liquibase, "rollbackFile");
					assertThat(actualFile).isEqualTo(file).exists();
					assertThat(contentOf(file)).contains("DROP TABLE PUBLIC.customer;");
				});
	}

	@Test
	void liquibaseDataSource() {
		this.contextRunner
				.withUserConfiguration(LiquibaseDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					assertThat(liquibase.getDataSource()).isEqualTo(context.getBean("liquibaseDataSource"));
				});
	}

	@Test
	void liquibaseDataSourceWithoutDataSourceAutoConfiguration() {
		this.contextRunner.withUserConfiguration(LiquibaseDataSourceConfiguration.class).run((context) -> {
			SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
			assertThat(liquibase.getDataSource()).isEqualTo(context.getBean("liquibaseDataSource"));
		});
	}

	@Test
	void userConfigurationBeans() {
		this.contextRunner
				.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("springLiquibase");
					assertThat(context).doesNotHaveBean("liquibase");
				});
	}

	@Test
	void userConfigurationEntityManagerFactoryDependency() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
				.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("entityManagerFactory");
					assertThat(beanDefinition.getDependsOn()).containsExactly("springLiquibase");
				});
	}

	@Test
	void userConfigurationJdbcTemplateDependency() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JdbcTemplateAutoConfiguration.class))
				.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("jdbcTemplate");
					assertThat(beanDefinition.getDependsOn()).containsExactly("springLiquibase");
				});
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
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa").build();
		}

		@LiquibaseDataSource
		@Bean
		DataSource liquibaseDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:liquibasetest").username("sa").build();
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

}
