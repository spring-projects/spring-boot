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

package org.springframework.boot.autoconfigure.liquibase;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
	void createsDataSourceWithNoDataSourceBeanAndLiquibaseUrl() {
		String jdbcUrl = "jdbc:hsqldb:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withPropertyValues("spring.liquibase.url:" + jdbcUrl).run(assertLiquibase((liquibase) -> {
			SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
			assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
		}));
	}

	@Test
	void backsOffWithLiquibaseUrlAndNoSpringJdbc() {
		this.contextRunner.withPropertyValues("spring.liquibase.url:jdbc:hsqldb:mem:" + UUID.randomUUID())
				.withClassLoader(new FilteredClassLoader("org.springframework.jdbc"))
				.run((context) -> assertThat(context).doesNotHaveBean(SpringLiquibase.class));
	}

	@Test
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
					assertThat(liquibase.isClearCheckSums()).isEqualTo(properties.isClearChecksums());
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
	void overrideClearChecksums() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.clear-checksums:true")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.isClearCheckSums()).isTrue()));
	}

	@Test
	void overrideDataSource() {
		String jdbcUrl = "jdbc:hsqldb:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:" + jdbcUrl).run(assertLiquibase((liquibase) -> {
					SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
					assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
					assertThat(dataSource.getDriver().getClass().getName()).isEqualTo("org.hsqldb.jdbc.JDBCDriver");
				}));
	}

	@Test
	void overrideDataSourceAndDriverClassName() {
		String jdbcUrl = "jdbc:hsqldb:mem:liquibase" + UUID.randomUUID();
		String driverClassName = "org.hsqldb.jdbcDriver";
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:" + jdbcUrl,
						"spring.liquibase.driver-class-name:" + driverClassName)
				.run(assertLiquibase((liquibase) -> {
					SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
					assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
					assertThat(dataSource.getDriver().getClass().getName()).isEqualTo(driverClassName);
				}));
	}

	@Test
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
	void overrideUserWhenCustom() {
		this.contextRunner.withUserConfiguration(CustomDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.user:test", "spring.liquibase.password:secret").run((context) -> {
					String expectedName = context.getBean(CustomDataSourceConfiguration.class).name;
					SpringLiquibase liquibase = context.getBean(SpringLiquibase.class);
					SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
					assertThat(dataSource.getUrl()).contains(expectedName);
					assertThat(dataSource.getUsername()).isEqualTo("test");
				});
	}

	@Test
	void createDataSourceDoesNotFallbackToEmbeddedProperties() {
		String jdbcUrl = "jdbc:hsqldb:mem:liquibase" + UUID.randomUUID();
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.url:" + jdbcUrl).run(assertLiquibase((liquibase) -> {
					SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
					assertThat(dataSource.getUsername()).isNull();
					assertThat(dataSource.getPassword()).isNull();
				}));
	}

	@Test
	void overrideUserAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.user:sa").run(assertLiquibase((liquibase) -> {
					SimpleDriverDataSource dataSource = (SimpleDriverDataSource) liquibase.getDataSource();
					assertThat(dataSource.getUrl()).startsWith("jdbc:h2:mem:");
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
				.run(assertLiquibase((liquibase) -> assertThat(output).doesNotContain(": liquibase:")));
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
					assertThat(parameters).containsKey("foo");
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

	@Test
	void overrideTag() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
				.withPropertyValues("spring.liquibase.tag:1.0.0")
				.run(assertLiquibase((liquibase) -> assertThat(liquibase.getTag()).isEqualTo("1.0.0")));
	}

	@Test
	void whenLiquibaseIsAutoConfiguredThenJooqDslContextDependsOnSpringLiquibaseBeans() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JooqAutoConfiguration.class))
				.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
					assertThat(beanDefinition.getDependsOn()).containsExactly("liquibase");
				});
	}

	@Test
	void whenCustomSpringLiquibaseIsDefinedThenJooqDslContextDependsOnSpringLiquibaseBeans() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(JooqAutoConfiguration.class))
				.withUserConfiguration(LiquibaseUserConfiguration.class, EmbeddedDataSourceConfiguration.class)
				.run((context) -> {
					BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
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
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal" + UUID.randomUUID()).username("sa").build();
		}

		@LiquibaseDataSource
		@Bean
		DataSource liquibaseDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:liquibasetest" + UUID.randomUUID()).username("sa")
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

		private String name = UUID.randomUUID().toString();

		@Bean(destroyMethod = "shutdown")
		EmbeddedDatabase dataSource() throws SQLException {
			EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
					.setName(this.name).build();
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

		private String name = UUID.randomUUID().toString();

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

	static class CustomH2Driver extends org.h2.Driver {

	}

}
