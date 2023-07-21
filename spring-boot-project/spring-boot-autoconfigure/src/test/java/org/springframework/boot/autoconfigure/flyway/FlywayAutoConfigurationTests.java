/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.flyway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.core.internal.database.postgresql.PostgreSQLConfigurationExtension;
import org.flywaydb.core.internal.license.FlywayTeamsUpgradeRequiredException;
import org.flywaydb.database.oracle.OracleConfigurationExtension;
import org.flywaydb.database.sqlserver.SQLServerConfigurationExtension;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.postgresql.Driver;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.FlywayAutoConfigurationRuntimeHints;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.OracleFlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.PostgresqlFlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.SqlServerFlywayConfigurationCustomizer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.SchemaManagement;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link FlywayAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Dominic Gunn
 * @author András Deák
 * @author Takaaki Shimbo
 * @author Chris Bono
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class FlywayAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void backsOffWithNoDataSourceBeanAndNoFlywayUrl() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(Flyway.class));
	}

	@Test
	void createsDataSourceWithNoDataSourceBeanAndFlywayUrl() {
		this.contextRunner.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:" + UUID.randomUUID())
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource()).isNotNull();
			});
	}

	@Test
	void backsOffWithFlywayUrlAndNoSpringJdbc() {
		this.contextRunner.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:" + UUID.randomUUID())
			.withClassLoader(new FilteredClassLoader("org.springframework.jdbc"))
			.run((context) -> assertThat(context).doesNotHaveBean(Flyway.class));
	}

	@Test
	void createDataSourceWithUrl() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:flywaytest")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource()).isNotNull();
			});
	}

	@Test
	void flywayPropertiesAreUsedOverJdbcConnectionDetails() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JdbcConnectionDetailsConfiguration.class,
					MockFlywayMigrationStrategy.class)
			.withPropertyValues("spring.flyway.url=jdbc:hsqldb:mem:flywaytest", "spring.flyway.user=some-user",
					"spring.flyway.password=some-password",
					"spring.flyway.driver-class-name=org.hsqldb.jdbc.JDBCDriver")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				DataSource dataSource = flyway.getConfiguration().getDataSource();
				assertThat(dataSource).isInstanceOf(SimpleDriverDataSource.class);
				SimpleDriverDataSource simpleDriverDataSource = (SimpleDriverDataSource) dataSource;
				assertThat(simpleDriverDataSource.getUrl()).isEqualTo("jdbc:hsqldb:mem:flywaytest");
				assertThat(simpleDriverDataSource.getUsername()).isEqualTo("some-user");
				assertThat(simpleDriverDataSource.getPassword()).isEqualTo("some-password");
				assertThat(simpleDriverDataSource.getDriver()).isInstanceOf(org.hsqldb.jdbc.JDBCDriver.class);
			});
	}

	@Test
	void flywayConnectionDetailsAreUsedOverFlywayProperties() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, FlywayConnectionDetailsConfiguration.class,
					MockFlywayMigrationStrategy.class)
			.withPropertyValues("spring.flyway.url=jdbc:hsqldb:mem:flywaytest", "spring.flyway.user=some-user",
					"spring.flyway.password=some-password",
					"spring.flyway.driver-class-name=org.hsqldb.jdbc.JDBCDriver")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				DataSource dataSource = flyway.getConfiguration().getDataSource();
				assertThat(dataSource).isInstanceOf(SimpleDriverDataSource.class);
				SimpleDriverDataSource simpleDriverDataSource = (SimpleDriverDataSource) dataSource;
				assertThat(simpleDriverDataSource.getUrl())
					.isEqualTo("jdbc:postgresql://database.example.com:12345/database-1");
				assertThat(simpleDriverDataSource.getUsername()).isEqualTo("user-1");
				assertThat(simpleDriverDataSource.getPassword()).isEqualTo("secret-1");
				assertThat(simpleDriverDataSource.getDriver()).isInstanceOf(Driver.class);
			});
	}

	@Test
	void shouldUseMainDataSourceWhenThereIsNoFlywaySpecificConfiguration() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JdbcConnectionDetailsConfiguration.class,
					MockFlywayMigrationStrategy.class)
			.withPropertyValues("spring.datasource.url=jdbc:hsqldb:mem:flywaytest", "spring.datasource.user=some-user",
					"spring.datasource.password=some-password",
					"spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver")
			.run((context) -> {
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getDataSource()).isSameAs(context.getBean(DataSource.class));
			});
	}

	@Test
	void createDataSourceWithUser() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.datasource.url:jdbc:hsqldb:mem:" + UUID.randomUUID(), "spring.flyway.user:sa")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource()).isNotNull();
			});
	}

	@Test
	void createDataSourceDoesNotFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:flywaytest")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				DataSource dataSource = context.getBean(Flyway.class).getConfiguration().getDataSource();
				assertThat(dataSource).isNotNull();
				assertThat(dataSource).hasFieldOrPropertyWithValue("username", null);
				assertThat(dataSource).hasFieldOrPropertyWithValue("password", null);
			});
	}

	@Test
	void createDataSourceWithUserAndFallbackToEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(PropertiesBackedH2DataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.user:test", "spring.flyway.password:secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				DataSource dataSource = context.getBean(Flyway.class).getConfiguration().getDataSource();
				assertThat(dataSource).isNotNull();
				assertThat(dataSource).extracting("url").asString().startsWith("jdbc:h2:mem:");
				assertThat(dataSource).extracting("username").asString().isEqualTo("test");
			});
	}

	@Test
	void createDataSourceWithUserAndCustomEmbeddedProperties() {
		this.contextRunner.withUserConfiguration(CustomBackedH2DataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.user:test", "spring.flyway.password:secret")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				String expectedName = context.getBean(CustomBackedH2DataSourceConfiguration.class).name;
				String propertiesName = context.getBean(DataSourceProperties.class).determineDatabaseName();
				assertThat(expectedName).isNotEqualTo(propertiesName);
				DataSource dataSource = context.getBean(Flyway.class).getConfiguration().getDataSource();
				assertThat(dataSource).isNotNull();
				assertThat(dataSource).extracting("url").asString().startsWith("jdbc:h2:mem:").contains(expectedName);
				assertThat(dataSource).extracting("username").asString().isEqualTo("test");
			});
	}

	@Test
	void flywayDataSource() {
		this.contextRunner
			.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource())
					.isEqualTo(context.getBean("flywayDataSource"));
			});
	}

	@Test
	void flywayDataSourceIsUsedWhenJdbcConnectionDetailsIsAvailable() {
		this.contextRunner
			.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class,
					JdbcConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(JdbcConnectionDetails.class);
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource())
					.isEqualTo(context.getBean("flywayDataSource"));
			});
	}

	@Test
	void flywayDataSourceIsUsedWhenFlywayConnectionDetailsIsAvailable() {
		this.contextRunner
			.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class,
					FlywayConnectionDetailsConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(FlywayConnectionDetails.class);
				assertThat(context).hasSingleBean(Flyway.class);
				assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource())
					.isEqualTo(context.getBean("flywayDataSource"));
			});
	}

	@Test
	void flywayDataSourceWithoutDataSourceAutoConfiguration() {
		this.contextRunner.withUserConfiguration(FlywayDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource())
				.isEqualTo(context.getBean("flywayDataSource"));
		});
	}

	@Test
	void flywayMultipleDataSources() {
		this.contextRunner.withUserConfiguration(FlywayMultipleDataSourcesConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			assertThat(context.getBean(Flyway.class).getConfiguration().getDataSource())
				.isEqualTo(context.getBean("flywayDataSource"));
		});
	}

	@Test
	void schemaManagementProviderDetectsDataSource() {
		this.contextRunner
			.withUserConfiguration(FlywayDataSourceConfiguration.class, EmbeddedDataSourceConfiguration.class)
			.run((context) -> {
				FlywaySchemaManagementProvider schemaManagementProvider = context
					.getBean(FlywaySchemaManagementProvider.class);
				assertThat(schemaManagementProvider.getSchemaManagement(context.getBean(DataSource.class)))
					.isEqualTo(SchemaManagement.UNMANAGED);
				assertThat(schemaManagementProvider
					.getSchemaManagement(context.getBean("flywayDataSource", DataSource.class)))
					.isEqualTo(SchemaManagement.MANAGED);
			});
	}

	@Test
	void defaultFlyway() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(Flyway.class);
			Flyway flyway = context.getBean(Flyway.class);
			assertThat(flyway.getConfiguration().getLocations())
				.containsExactly(new Location("classpath:db/migration"));
		});
	}

	@Test
	void overrideLocations() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.locations:classpath:db/changelog,classpath:db/migration")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getLocations())
					.containsExactly(new Location("classpath:db/changelog"), new Location("classpath:db/migration"));
			});
	}

	@Test
	void overrideLocationsList() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.locations[0]:classpath:db/changelog",
					"spring.flyway.locations[1]:classpath:db/migration")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getLocations())
					.containsExactly(new Location("classpath:db/changelog"), new Location("classpath:db/migration"));
			});
	}

	@Test
	void overrideSchemas() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.schemas:public")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(Arrays.asList(flyway.getConfiguration().getSchemas())).hasToString("[public]");
			});
	}

	@Test
	void overrideDataSourceAndDriverClassName() {
		String jdbcUrl = "jdbc:hsqldb:mem:flyway" + UUID.randomUUID();
		String driverClassName = "org.hsqldb.jdbcDriver";
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.url:" + jdbcUrl, "spring.flyway.driver-class-name:" + driverClassName)
			.run((context) -> {
				Flyway flyway = context.getBean(Flyway.class);
				SimpleDriverDataSource dataSource = (SimpleDriverDataSource) flyway.getConfiguration().getDataSource();
				assertThat(dataSource.getUrl()).isEqualTo(jdbcUrl);
				assertThat(dataSource.getDriver().getClass().getName()).isEqualTo(driverClassName);
			});
	}

	@Test
	void changeLogDoesNotExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.fail-on-missing-locations=true",
					"spring.flyway.locations:filesystem:no-such-dir")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
			});
	}

	@Test
	void failOnMissingLocationsAllMissing() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.fail-on-missing-locations=true")
			.withPropertyValues("spring.flyway.locations:classpath:db/missing1,classpath:db/migration2")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context).getFailure().isInstanceOf(BeanCreationException.class);
				assertThat(context).getFailure().hasMessageContaining("Unable to resolve location");
			});
	}

	@Test
	void failOnMissingLocationsAllExist() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.fail-on-missing-locations=true")
			.withPropertyValues("spring.flyway.locations:classpath:db/changelog,classpath:db/migration")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void failOnMissingLocationsAllExistWithImplicitClasspathPrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.fail-on-missing-locations=true")
			.withPropertyValues("spring.flyway.locations:db/changelog,db/migration")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void failOnMissingLocationsAllExistWithFilesystemPrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.fail-on-missing-locations=true")
			.withPropertyValues("spring.flyway.locations:filesystem:src/test/resources/db/migration")
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void customFlywayMigrationStrategy() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, MockFlywayMigrationStrategy.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				context.getBean(MockFlywayMigrationStrategy.class).assertCalled();
			});
	}

	@Test
	void flywayJavaMigrations() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, FlywayJavaMigrationsConfiguration.class)
			.run((context) -> {
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getJavaMigrations()).hasSize(2);
			});
	}

	@Test
	void customFlywayMigrationInitializer() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayMigrationInitializer.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				FlywayMigrationInitializer initializer = context.getBean(FlywayMigrationInitializer.class);
				assertThat(initializer.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
			});
	}

	@Test
	void customFlywayWithJpa() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayWithJpaConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void customFlywayWithJdbc() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CustomFlywayWithJdbcConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void customFlywayMigrationInitializerWithJpa() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
					CustomFlywayMigrationInitializerWithJpaConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void customFlywayMigrationInitializerWithJdbc() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class,
					CustomFlywayMigrationInitializerWithJdbcConfiguration.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void overrideBaselineVersionString() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.baseline-version=0")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("0"));
			});
	}

	@Test
	void overrideBaselineVersionNumber() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.baseline-version=1")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("1"));
			});
	}

	@Test
	void useVendorDirectory() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.locations=classpath:db/vendors/{vendor},classpath:db/changelog")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getLocations()).containsExactlyInAnyOrder(
						new Location("classpath:db/vendors/h2"), new Location("classpath:db/changelog"));
			});
	}

	@Test
	void useOneLocationWithVendorDirectory() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.locations=classpath:db/vendors/{vendor}")
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getLocations())
					.containsExactly(new Location("classpath:db/vendors/h2"));
			});
	}

	@Test
	void callbacksAreConfiguredAndOrderedByName() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, CallbackConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				Callback callbackOne = context.getBean("callbackOne", Callback.class);
				Callback callbackTwo = context.getBean("callbackTwo", Callback.class);
				assertThat(flyway.getConfiguration().getCallbacks()).hasSize(2);
				InOrder orderedCallbacks = inOrder(callbackOne, callbackTwo);
				orderedCallbacks.verify(callbackTwo).handle(any(Event.class), any(Context.class));
				orderedCallbacks.verify(callbackOne).handle(any(Event.class), any(Context.class));
			});
	}

	@Test
	void configurationCustomizersAreConfiguredAndOrdered() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, ConfigurationCustomizerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getConnectRetries()).isEqualTo(5);
				assertThat(flyway.getConfiguration().getBaselineDescription()).isEqualTo("<< Custom baseline >>");
				assertThat(flyway.getConfiguration().getBaselineVersion()).isEqualTo(MigrationVersion.fromVersion("1"));
			});
	}

	@Test
	void callbackAndMigrationBeansAreAppliedToConfigurationBeforeCustomizersAreCalled() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, FlywayJavaMigrationsConfiguration.class,
					CallbackConfiguration.class)
			.withBean(FlywayConfigurationCustomizer.class, () -> (configuration) -> {
				assertThat(configuration.getCallbacks()).isNotEmpty();
				assertThat(configuration.getJavaMigrations()).isNotEmpty();
			})
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void batchIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.batch=true")
			.run(validateFlywayTeamsPropertyOnly("batch"));
	}

	@Test
	void dryRunOutputIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.dryRunOutput=dryrun.sql")
			.run(validateFlywayTeamsPropertyOnly("dryRunOutput"));
	}

	@Test
	void errorOverridesIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.errorOverrides=D12345")
			.run(validateFlywayTeamsPropertyOnly("errorOverrides"));
	}

	@Test
	void licenseKeyIsCorrectlyMapped(CapturedOutput output) {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.license-key=<<secret>>")
			.run((context) -> assertThat(output).contains("License key detected - in order to use Teams or "
					+ "Enterprise features, download Flyway Teams Edition & Flyway Enterprise Edition"));
	}

	@Test
	void oracleExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new OracleFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

	@Test
	void oracleSqlplusIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle.sqlplus=true")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getSqlplus()).isTrue());

	}

	@Test
	@Deprecated(since = "3.2.0", forRemoval = true)
	void oracleSqlplusIsCorrectlyMappedWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle-sqlplus=true")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getSqlplus()).isTrue());

	}

	@Test
	void oracleSqlplusWarnIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle.sqlplus-warn=true")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getSqlplusWarn()).isTrue());
	}

	@Test
	@Deprecated(since = "3.2.0", forRemoval = true)
	void oracleSqlplusWarnIsCorrectlyMappedWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle-sqlplus-warn=true")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getSqlplusWarn()).isTrue());
	}

	@Test
	void oracleWallerLocationIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle.wallet-location=/tmp/my.wallet")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getWalletLocation()).isEqualTo("/tmp/my.wallet"));
	}

	@Test
	@Deprecated(since = "3.2.0", forRemoval = true)
	void oracleWallerLocationIsCorrectlyMappedWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle-wallet-location=/tmp/my.wallet")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getWalletLocation()).isEqualTo("/tmp/my.wallet"));
	}

	@Test
	void oracleKerberosCacheFileIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle.kerberos-cache-file=/tmp/cache")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getKerberosCacheFile()).isEqualTo("/tmp/cache"));
	}

	@Test
	@Deprecated(since = "3.2.0", forRemoval = true)
	void oracleKerberosCacheFileIsCorrectlyMappedWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.oracle-kerberos-cache-file=/tmp/cache")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(OracleConfigurationExtension.class)
				.getKerberosCacheFile()).isEqualTo("/tmp/cache"));
	}

	@Test
	void streamIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.stream=true")
			.run(validateFlywayTeamsPropertyOnly("stream"));
	}

	@Test
	void undoSqlMigrationPrefix() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.undo-sql-migration-prefix=undo")
			.run(validateFlywayTeamsPropertyOnly("undoSqlMigrationPrefix"));
	}

	@Test
	void customFlywayClassLoader() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, ResourceLoaderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(Flyway.class);
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getClassLoader()).isInstanceOf(CustomClassLoader.class);
			});
	}

	@Test
	void initSqlsWithDataSource() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.init-sqls=SELECT 1")
			.run((context) -> {
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getInitSql()).isEqualTo("SELECT 1");
			});
	}

	@Test
	void initSqlsWithFlywayUrl() {
		this.contextRunner
			.withPropertyValues("spring.flyway.url:jdbc:h2:mem:" + UUID.randomUUID(),
					"spring.flyway.init-sqls=SELECT 1")
			.run((context) -> {
				Flyway flyway = context.getBean(Flyway.class);
				assertThat(flyway.getConfiguration().getInitSql()).isEqualTo("SELECT 1");
			});
	}

	@Test
	void cherryPickIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.cherry-pick=1.1")
			.run(validateFlywayTeamsPropertyOnly("cherryPick"));
	}

	@Test
	void jdbcPropertiesAreCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.jdbc-properties.prop=value")
			.run(validateFlywayTeamsPropertyOnly("jdbcProperties"));
	}

	@Test
	void kerberosConfigFileIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.kerberos-config-file=/tmp/config")
			.run(validateFlywayTeamsPropertyOnly("kerberosConfigFile"));
	}

	@Test
	void outputQueryResultsIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.output-query-results=false")
			.run(validateFlywayTeamsPropertyOnly("outputQueryResults"));
	}

	@Test
	void postgresqlExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new PostgresqlFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

	@Test
	void postgresqlTransactionalLockIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.postgresql.transactional-lock=false")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(PostgreSQLConfigurationExtension.class)
				.isTransactionalLock()).isFalse());
	}

	@Test
	void sqlServerExtensionIsNotLoadedByDefault() {
		FluentConfiguration configuration = mock(FluentConfiguration.class);
		new SqlServerFlywayConfigurationCustomizer(new FlywayProperties()).customize(configuration);
		then(configuration).shouldHaveNoInteractions();
	}

	@Test
	void sqlServerKerberosLoginFileIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.sqlserver.kerberos-login-file=/tmp/config")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(SQLServerConfigurationExtension.class)
				.getKerberos()
				.getLogin()
				.getFile()).isEqualTo("/tmp/config"));
	}

	@Test
	@Deprecated(since = "3.2.0", forRemoval = true)
	void sqlServerKerberosLoginFileIsCorrectlyMappedWithDeprecatedProperty() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.sql-server-kerberos-login-file=/tmp/config")
			.run((context) -> assertThat(context.getBean(Flyway.class)
				.getConfiguration()
				.getPluginRegister()
				.getPlugin(SQLServerConfigurationExtension.class)
				.getKerberos()
				.getLogin()
				.getFile()).isEqualTo("/tmp/config"));
	}

	@Test
	void skipExecutingMigrationsIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.skip-executing-migrations=true")
			.run(validateFlywayTeamsPropertyOnly("skipExecutingMigrations"));
	}

	@Test
	void whenFlywayIsAutoConfiguredThenJooqDslContextDependsOnFlywayBeans() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JooqConfiguration.class)
			.run((context) -> {
				BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
				assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("flywayInitializer", "flyway");
			});
	}

	@Test
	void whenCustomMigrationInitializerIsDefinedThenJooqDslContextDependsOnIt() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JooqConfiguration.class,
					CustomFlywayMigrationInitializer.class)
			.run((context) -> {
				BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
				assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("flywayMigrationInitializer",
						"flyway");
			});
	}

	@Test
	void whenCustomFlywayIsDefinedThenJooqDslContextDependsOnIt() {
		this.contextRunner
			.withUserConfiguration(EmbeddedDataSourceConfiguration.class, JooqConfiguration.class, CustomFlyway.class)
			.run((context) -> {
				BeanDefinition beanDefinition = context.getBeanFactory().getBeanDefinition("dslContext");
				assertThat(beanDefinition.getDependsOn()).containsExactlyInAnyOrder("customFlyway");
			});
	}

	@Test
	void scriptPlaceholderPrefixIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.script-placeholder-prefix=SPP")
			.run((context) -> assertThat(context.getBean(Flyway.class).getConfiguration().getScriptPlaceholderPrefix())
				.isEqualTo("SPP"));
	}

	@Test
	void scriptPlaceholderSuffixIsCorrectlyMapped() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.script-placeholder-suffix=SPS")
			.run((context) -> assertThat(context.getBean(Flyway.class).getConfiguration().getScriptPlaceholderSuffix())
				.isEqualTo("SPS"));
	}

	@Test
	void containsResourceProviderCustomizer() {
		this.contextRunner.withPropertyValues("spring.flyway.url:jdbc:hsqldb:mem:" + UUID.randomUUID())
			.run((context) -> assertThat(context).hasSingleBean(ResourceProviderCustomizer.class));
	}

	@Test
	void loggers() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.run((context) -> assertThat(context.getBean(Flyway.class).getConfiguration().getLoggers())
				.containsExactly("slf4j"));
	}

	@Test
	void overrideLoggers() {
		this.contextRunner.withUserConfiguration(EmbeddedDataSourceConfiguration.class)
			.withPropertyValues("spring.flyway.loggers=log4j2")
			.run((context) -> assertThat(context.getBean(Flyway.class).getConfiguration().getLoggers())
				.containsExactly("log4j2"));
	}

	@Test
	void shouldRegisterResourceHints() {
		RuntimeHints runtimeHints = new RuntimeHints();
		new FlywayAutoConfigurationRuntimeHints().registerHints(runtimeHints, getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.resource().forResource("db/migration/")).accepts(runtimeHints);
		assertThat(RuntimeHintsPredicates.resource().forResource("db/migration/V1__init.sql")).accepts(runtimeHints);
	}

	private ContextConsumer<AssertableApplicationContext> validateFlywayTeamsPropertyOnly(String propertyName) {
		return (context) -> {
			assertThat(context).hasFailed();
			Throwable failure = context.getStartupFailure();
			assertThat(failure).hasRootCauseInstanceOf(FlywayTeamsUpgradeRequiredException.class);
			assertThat(failure).hasMessageContaining(String.format(" %s ", propertyName));
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class FlywayDataSourceConfiguration {

		@Bean
		@Primary
		DataSource normalDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:normal").username("sa").build();
		}

		@FlywayDataSource
		@Bean
		DataSource flywayDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:flywaytest").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FlywayMultipleDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:first").username("sa").build();
		}

		@Bean
		DataSource secondDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:second").username("sa").build();
		}

		@FlywayDataSource
		@Bean
		DataSource flywayDataSource() {
			return DataSourceBuilder.create().url("jdbc:hsqldb:mem:flywaytest").username("sa").build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class FlywayJavaMigrationsConfiguration {

		@Bean
		TestMigration migration1() {
			return new TestMigration("2", "M1");
		}

		@Bean
		TestMigration migration2() {
			return new TestMigration("3", "M2");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceLoaderConfiguration {

		@Bean
		@Primary
		ResourceLoader customClassLoader() {
			return new DefaultResourceLoader(new CustomClassLoader(getClass().getClassLoader()));
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlywayMigrationInitializer {

		@Bean
		FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
			FlywayMigrationInitializer initializer = new FlywayMigrationInitializer(flyway);
			initializer.setOrder(Ordered.HIGHEST_PRECEDENCE);
			return initializer;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlyway {

		@Bean
		Flyway customFlyway() {
			return Flyway.configure().load();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlywayMigrationInitializerWithJpaConfiguration {

		@Bean
		FlywayMigrationInitializer customFlywayMigrationInitializer(Flyway flyway) {
			return new FlywayMigrationInitializer(flyway);
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(DataSource dataSource) {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), properties, null)
				.dataSource(dataSource)
				.build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomFlywayWithJpaConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayWithJpaConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		Flyway customFlyway() {
			return Flyway.configure().load();
		}

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), properties, null)
				.dataSource(this.dataSource)
				.build();
		}

	}

	@Configuration
	static class CustomFlywayWithJdbcConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayWithJdbcConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		Flyway customFlyway() {
			return Flyway.configure().load();
		}

		@Bean
		JdbcOperations jdbcOperations() {
			return new JdbcTemplate(this.dataSource);
		}

		@Bean
		NamedParameterJdbcOperations namedParameterJdbcOperations() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	@Configuration
	protected static class CustomFlywayMigrationInitializerWithJdbcConfiguration {

		private final DataSource dataSource;

		protected CustomFlywayMigrationInitializerWithJdbcConfiguration(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		public FlywayMigrationInitializer customFlywayMigrationInitializer(Flyway flyway) {
			return new FlywayMigrationInitializer(flyway);
		}

		@Bean
		public JdbcOperations jdbcOperations() {
			return new JdbcTemplate(this.dataSource);
		}

		@Bean
		public NamedParameterJdbcOperations namedParameterJdbcOperations() {
			return new NamedParameterJdbcTemplate(this.dataSource);
		}

	}

	@Component
	static class MockFlywayMigrationStrategy implements FlywayMigrationStrategy {

		private boolean called = false;

		@Override
		public void migrate(Flyway flyway) {
			this.called = true;
		}

		void assertCalled() {
			assertThat(this.called).isTrue();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CallbackConfiguration {

		@Bean
		Callback callbackOne() {
			return mockCallback("b");
		}

		@Bean
		Callback callbackTwo() {
			return mockCallback("a");
		}

		private Callback mockCallback(String name) {
			Callback callback = mock(Callback.class);
			given(callback.supports(any(Event.class), any(Context.class))).willReturn(true);
			given(callback.getCallbackName()).willReturn(name);
			return callback;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ConfigurationCustomizerConfiguration {

		@Bean
		@Order(1)
		FlywayConfigurationCustomizer customizerOne() {
			return (configuration) -> configuration.connectRetries(5).baselineVersion("1");
		}

		@Bean
		@Order(0)
		FlywayConfigurationCustomizer customizerTwo() {
			return (configuration) -> configuration.connectRetries(10).baselineDescription("<< Custom baseline >>");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JooqConfiguration {

		@Bean
		DSLContext dslContext() {
			return new DefaultDSLContext(SQLDialect.H2);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(DataSourceProperties.class)
	abstract static class AbstractUserH2DataSourceConfiguration {

		@Bean(destroyMethod = "shutdown")
		EmbeddedDatabase dataSource(DataSourceProperties properties) throws SQLException {
			EmbeddedDatabase database = new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2)
				.setName(getDatabaseName(properties))
				.build();
			insertUser(database);
			return database;
		}

		protected abstract String getDatabaseName(DataSourceProperties properties);

		private void insertUser(EmbeddedDatabase database) throws SQLException {
			try (Connection connection = database.getConnection()) {
				connection.prepareStatement("CREATE USER test password 'secret'").execute();
				connection.prepareStatement("ALTER USER test ADMIN TRUE").execute();
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class PropertiesBackedH2DataSourceConfiguration extends AbstractUserH2DataSourceConfiguration {

		@Override
		protected String getDatabaseName(DataSourceProperties properties) {
			return properties.determineDatabaseName();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomBackedH2DataSourceConfiguration extends AbstractUserH2DataSourceConfiguration {

		private final String name = UUID.randomUUID().toString();

		@Override
		protected String getDatabaseName(DataSourceProperties properties) {
			return this.name;
		}

	}

	static final class CustomClassLoader extends ClassLoader {

		private CustomClassLoader(ClassLoader parent) {
			super(parent);
		}

	}

	private static final class TestMigration implements JavaMigration {

		private final MigrationVersion version;

		private final String description;

		private TestMigration(String version, String description) {
			this.version = MigrationVersion.fromVersion(version);
			this.description = description;
		}

		@Override
		public MigrationVersion getVersion() {
			return this.version;
		}

		@Override
		public String getDescription() {
			return this.description;
		}

		@Override
		public Integer getChecksum() {
			return 1;
		}

		@Override
		public boolean canExecuteInTransaction() {
			return true;
		}

		@Override
		public void migrate(org.flywaydb.core.api.migration.Context context) {

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
	static class FlywayConnectionDetailsConfiguration {

		@Bean
		FlywayConnectionDetails flywayConnectionDetails() {
			return new FlywayConnectionDetails() {

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

}
