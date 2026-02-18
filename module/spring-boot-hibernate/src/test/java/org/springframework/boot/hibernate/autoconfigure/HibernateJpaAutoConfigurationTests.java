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

package org.springframework.boot.hibernate.autoconfigure;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.transaction.Synchronization;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategySnakeCaseImpl;
import org.hibernate.cfg.ManagedBeanSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.hibernate.SpringJtaPlatform;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfigurationTests.JpaUsingApplicationListenerConfiguration.EventCapturingApplicationListener;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaConfiguration.HibernateRuntimeHints;
import org.springframework.boot.hibernate.autoconfigure.mapping.NonAnnotatedEntity;
import org.springframework.boot.hibernate.autoconfigure.test.city.City;
import org.springframework.boot.hibernate.autoconfigure.test.country.Country;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.XADataSourceAutoConfiguration;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryBuilderCustomizer;
import org.springframework.boot.jpa.autoconfigure.JpaBaseConfiguration;
import org.springframework.boot.jpa.autoconfigure.JpaProperties;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.BuildOutput;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizationAutoConfiguration;
import org.springframework.boot.transaction.jta.autoconfigure.JtaAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.orm.jpa.persistenceunit.ManagedClassNameFilter;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Base for JPA tests and tests for {@link JpaBaseConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Yanming Zhou
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Chris Bono
 * @author Moritz Halbritter
 */
class HibernateJpaAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.datasource.generate-unique-name=true",
				"spring.jta.log-dir=" + new File(new BuildOutput(getClass()).getRootLocation(), "transaction-logs"))
		.withUserConfiguration(TestConfiguration.class)
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, TransactionAutoConfiguration.class,
				TransactionManagerCustomizationAutoConfiguration.class, DataSourceInitializationAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class));

	@Test
	void notConfiguredIfDataSourceIsNotAvailable() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
			.run(assertJpaIsNotAutoConfigured());
	}

	@Test
	void notConfiguredIfNoSingleDataSourceCandidateIsAvailable() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
			.withUserConfiguration(TestTwoDataSourcesConfiguration.class)
			.run(assertJpaIsNotAutoConfigured());
	}

	protected ContextConsumer<AssertableApplicationContext> assertJpaIsNotAutoConfigured() {
		return (context) -> {
			assertThat(context).hasNotFailed();
			assertThat(context).hasSingleBean(JpaProperties.class);
			assertThat(context).doesNotHaveBean(TransactionManager.class);
			assertThat(context).doesNotHaveBean(EntityManagerFactory.class);
		};
	}

	@Test
	void configuredWithAutoConfiguredDataSource() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(DataSource.class);
			assertThat(context).hasSingleBean(JpaTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
			assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
		});
	}

	@Test
	void configuredWithSingleCandidateDataSource() {
		this.contextRunner.withUserConfiguration(TestTwoDataSourcesAndPrimaryConfiguration.class).run((context) -> {
			assertThat(context).getBeans(DataSource.class).hasSize(2);
			assertThat(context).hasSingleBean(JpaTransactionManager.class);
			assertThat(context).hasSingleBean(EntityManagerFactory.class);
			assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
		});
	}

	@Test
	void jpaTransactionManagerTakesPrecedenceOverSimpleDataSourceOne() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasSingleBean(DataSource.class);
				assertThat(context).hasSingleBean(JpaTransactionManager.class);
				assertThat(context).getBean("transactionManager").isInstanceOf(JpaTransactionManager.class);
			});
	}

	@Test
	void openEntityManagerInViewInterceptorIsCreated() {
		new WebApplicationContextRunner().withPropertyValues("spring.datasource.generate-unique-name=true")
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).hasSingleBean(OpenEntityManagerInViewInterceptor.class));
	}

	@Test
	void openEntityManagerInViewInterceptorIsNotRegisteredWhenFilterPresent() {
		new WebApplicationContextRunner().withPropertyValues("spring.datasource.generate-unique-name=true")
			.withUserConfiguration(TestFilterConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OpenEntityManagerInViewInterceptor.class));
	}

	@Test
	void openEntityManagerInViewInterceptorIsNotRegisteredWhenFilterRegistrationPresent() {
		new WebApplicationContextRunner().withPropertyValues("spring.datasource.generate-unique-name=true")
			.withUserConfiguration(TestFilterRegistrationConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OpenEntityManagerInViewInterceptor.class));
	}

	@Test
	void openEntityManagerInViewInterceptorAutoConfigurationBacksOffWhenManuallyRegistered() {
		new WebApplicationContextRunner().withPropertyValues("spring.datasource.generate-unique-name=true")
			.withUserConfiguration(TestInterceptorManualConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).getBean(OpenEntityManagerInViewInterceptor.class)
				.isExactlyInstanceOf(
						TestInterceptorManualConfiguration.ManualOpenEntityManagerInViewInterceptor.class));
	}

	@Test
	void openEntityManagerInViewInterceptorIsNotRegisteredWhenExplicitlyOff() {
		new WebApplicationContextRunner()
			.withPropertyValues("spring.datasource.generate-unique-name=true", "spring.jpa.open-in-view=false")
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
					TransactionAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OpenEntityManagerInViewInterceptor.class));
	}

	@Test
	void customJpaProperties() {
		this.contextRunner
			.withPropertyValues("spring.jpa.properties.a:b", "spring.jpa.properties.a.b:c", "spring.jpa.properties.c:d")
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean bean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				Map<String, Object> map = bean.getJpaPropertyMap();
				assertThat(map).containsEntry("a", "b");
				assertThat(map).containsEntry("c", "d");
				assertThat(map).containsEntry("a.b", "c");
			});
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void usesManuallyDefinedLocalContainerEntityManagerFactoryBeanUsingBuilder() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.a=b")
			.withUserConfiguration(TestConfigurationWithEntityManagerFactoryBuilder.class)
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean factoryBean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				Map<String, Object> map = factoryBean.getJpaPropertyMap();
				assertThat(map).containsEntry("configured", "manually").containsEntry("a", "b");
			});
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void usesManuallyDefinedLocalContainerEntityManagerFactoryBeanIfAvailable() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithLocalContainerEntityManagerFactoryBean.class)
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean factoryBean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				Map<String, Object> map = factoryBean.getJpaPropertyMap();
				assertThat(map).containsEntry("configured", "manually");
			});
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void usesManuallyDefinedEntityManagerFactoryIfAvailable() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithLocalContainerEntityManagerFactoryBean.class)
			.run((context) -> {
				EntityManagerFactory factoryBean = context.getBean(EntityManagerFactory.class);
				Map<String, Object> map = factoryBean.getProperties();
				assertThat(map).containsEntry("configured", "manually");
			});
	}

	@Test
	void usesManuallyDefinedTransactionManagerBeanIfAvailable() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithTransactionManager.class).run((context) -> {
			assertThat(context).hasSingleBean(JpaTransactionManager.class);
			JpaTransactionManager txManager = context.getBean(JpaTransactionManager.class);
			assertThat(txManager).isInstanceOf(CustomJpaTransactionManager.class);
		});
	}

	@Test
	void defaultPersistenceManagedTypes() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
			EntityManager entityManager = context.getBean(EntityManagerFactory.class).createEntityManager();
			assertThat(getManagedJavaTypes(entityManager)).contains(City.class).doesNotContain(Country.class);
		});
	}

	@Test
	void customPersistenceManagedTypes() {
		this.contextRunner
			.withBean(PersistenceManagedTypes.class, () -> PersistenceManagedTypes.of(Country.class.getName()))
			.run((context) -> {
				assertThat(context).hasSingleBean(PersistenceManagedTypes.class);
				EntityManager entityManager = context.getBean(EntityManagerFactory.class).createEntityManager();
				assertThat(getManagedJavaTypes(entityManager)).contains(Country.class).doesNotContain(City.class);
			});
	}

	@Test
	void customPersistenceUnitManager() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithCustomPersistenceUnitManager.class)
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				assertThat(entityManagerFactoryBean).hasFieldOrPropertyWithValue("persistenceUnitManager",
						context.getBean(PersistenceUnitManager.class));
			});
	}

	@Test
	void customPersistenceUnitPostProcessors() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithCustomPersistenceUnitPostProcessors.class)
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				PersistenceUnitInfo persistenceUnitInfo = entityManagerFactoryBean.getPersistenceUnitInfo();
				assertThat(persistenceUnitInfo).isNotNull();
				assertThat(persistenceUnitInfo.getManagedClassNames())
					.contains("customized.attribute.converter.class.name");
			});
	}

	@Test
	void customPersistenceUnitProcessorsAddedByServeralContributors() {
		this.contextRunner.withUserConfiguration(TestConfigurationWithMultipleCustomPersistenceUnitPostProcessors.class)
			.run((context) -> {
				LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = context
					.getBean(LocalContainerEntityManagerFactoryBean.class);
				PersistenceUnitInfo persistenceUnitInfo = entityManagerFactoryBean.getPersistenceUnitInfo();
				assertThat(persistenceUnitInfo).isNotNull();
				assertThat(persistenceUnitInfo.getManagedClassNames()).contains(
						"customized.attribute.converter.class.name",
						"customized.attribute.converter.class.anotherName");
			});
	}

	@Test
	void customManagedClassNameFilter() {
		this.contextRunner.withBean(ManagedClassNameFilter.class, () -> (s) -> !s.endsWith("City"))
			.withUserConfiguration(AutoConfigurePackageForCountry.class)
			.run((context) -> {
				EntityManager entityManager = context.getBean(EntityManagerFactory.class).createEntityManager();
				assertThat(getManagedJavaTypes(entityManager)).contains(Country.class).doesNotContain(City.class);
			});
	}

	@Test
	void testDmlScriptWithMissingDdl() {
		this.contextRunner.withPropertyValues("spring.sql.init.data-locations:classpath:/city.sql",
				// Missing:
				"spring.sql.init.schema-locations:classpath:/ddl.sql")
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure()).hasMessageContaining("ddl.sql");
			});
	}

	@Test
	void testDmlScript() {
		// This can't succeed because the data SQL is executed immediately after the
		// schema and Hibernate hasn't initialized yet at that point
		this.contextRunner.withPropertyValues("spring.sql.init.data-locations:/city.sql").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).isInstanceOf(BeanCreationException.class);
		});
	}

	@Test
	@WithResource(name = "city.sql",
			content = "INSERT INTO CITY (ID, NAME, STATE, COUNTRY, MAP) values (2000, 'Washington', 'DC', 'US', 'Google')")
	void testDmlScriptRunsEarly() {
		this.contextRunner.withUserConfiguration(TestInitializedJpaConfiguration.class)
			.withClassLoader(new HideDataScriptClassLoader())
			.withPropertyValues("spring.jpa.show-sql=true", "spring.jpa.properties.hibernate.format_sql=true",
					"spring.jpa.properties.hibernate.highlight_sql=true", "spring.jpa.hibernate.ddl-auto:create-drop",
					"spring.sql.init.data-locations:/city.sql", "spring.jpa.defer-datasource-initialization=true")
			.run((context) -> assertThat(context.getBean(TestInitializedJpaConfiguration.class).called).isTrue());
	}

	@Test
	@WithResource(name = "db/city/V1__init.sql", content = """
			CREATE SEQUENCE city_seq INCREMENT BY 50;
			CREATE TABLE CITY (
			  id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
			  name VARCHAR(30),
			  state VARCHAR(30),
			  country VARCHAR(30),
			  map VARCHAR(30)
			);
			""")
	void testFlywaySwitchOffDdlAuto() {
		this.contextRunner.withPropertyValues("spring.sql.init.mode:never", "spring.flyway.locations:classpath:db/city")
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	@WithResource(name = "db/city/V1__init.sql", content = """
			CREATE SEQUENCE city_seq INCREMENT BY 50;

			CREATE TABLE CITY (
			  id         BIGINT GENERATED BY DEFAULT AS IDENTITY,
			  name VARCHAR(30),
			  state VARCHAR(30),
			  country VARCHAR(30),
			  map VARCHAR(30)
			);
			""")
	void testFlywayPlusValidation() {
		this.contextRunner
			.withPropertyValues("spring.sql.init.mode:never", "spring.flyway.locations:classpath:db/city",
					"spring.jpa.hibernate.ddl-auto:validate")
			.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	@WithResource(name = "db/changelog/db.changelog-city.yaml", content = """
			databaseChangeLog:
			- changeSet:
			    id: 1
			    author: dsyer
			    changes:
			      - createSequence:
			          sequenceName: city_seq
			          incrementBy: 50
			      - createTable:
			          tableName: city
			          columns:
			            - column:
			                name: id
			                type: bigint
			                autoIncrement: true
			                constraints:
			                  primaryKey: true
			                  nullable: false
			            - column:
			                name: name
			                type: varchar(50)
			                constraints:
			                  nullable: false
			            - column:
			                name: state
			                type: varchar(50)
			                constraints:
			                  nullable: false
			            - column:
			                name: country
			                type: varchar(50)
			                constraints:
			                  nullable: false
			            - column:
			                name: map
			                type: varchar(50)
			                constraints:
			                  nullable: true
			""")
	void testLiquibasePlusValidation() {
		this.contextRunner
			.withPropertyValues("spring.liquibase.change-log:classpath:db/changelog/db.changelog-city.yaml",
					"spring.jpa.hibernate.ddl-auto:validate")
			.withConfiguration(AutoConfigurations.of(LiquibaseAutoConfiguration.class))
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void hibernateDialectIsNotSetByDefault() {
		this.contextRunner.run(assertJpaVendorAdapter(
				(adapter) -> assertThat(adapter.getJpaPropertyMap()).doesNotContainKeys("hibernate.dialect")));
	}

	@Test
	void shouldConfigureHibernateJpaDialectWithSqlExceptionTranslatorIfPresent() {
		SQLStateSQLExceptionTranslator sqlExceptionTranslator = new SQLStateSQLExceptionTranslator();
		this.contextRunner.withBean(SQLStateSQLExceptionTranslator.class, () -> sqlExceptionTranslator)
			.run(assertJpaVendorAdapter(
					(adapter) -> assertThat(adapter.getJpaDialect()).extracting("exceptionTranslator")
						.hasFieldOrPropertyWithValue("jdbcExceptionTranslator", sqlExceptionTranslator)));
	}

	@Test
	void shouldNotConfigureHibernateJpaDialectWithSqlExceptionTranslatorIfNotUnique() {
		SQLStateSQLExceptionTranslator sqlExceptionTranslator1 = new SQLStateSQLExceptionTranslator();
		SQLStateSQLExceptionTranslator sqlExceptionTranslator2 = new SQLStateSQLExceptionTranslator();
		this.contextRunner
			.withBean("sqlExceptionTranslator1", SQLExceptionTranslator.class, () -> sqlExceptionTranslator1)
			.withBean("sqlExceptionTranslator2", SQLExceptionTranslator.class, () -> sqlExceptionTranslator2)
			.run(assertJpaVendorAdapter(
					(adapter) -> assertThat(adapter.getJpaDialect()).extracting("exceptionTranslator")
						.hasFieldOrPropertyWithValue("jdbcExceptionTranslator", null)));
	}

	@Test
	void hibernateDialectIsSetWhenDatabaseIsSet() {
		this.contextRunner.withPropertyValues("spring.jpa.database=H2")
			.run(assertJpaVendorAdapter((adapter) -> assertThat(adapter.getJpaPropertyMap())
				.contains(entry("hibernate.dialect", H2Dialect.class.getName()))));
	}

	@Test
	void hibernateDialectIsSetWhenDatabasePlatformIsSet() {
		String databasePlatform = TestH2Dialect.class.getName();
		this.contextRunner.withPropertyValues("spring.jpa.database-platform=" + databasePlatform)
			.run(assertJpaVendorAdapter((adapter) -> assertThat(adapter.getJpaPropertyMap())
				.contains(entry("hibernate.dialect", databasePlatform))));
	}

	private ContextConsumer<AssertableApplicationContext> assertJpaVendorAdapter(
			Consumer<HibernateJpaVendorAdapter> adapter) {
		return (context) -> {
			assertThat(context).hasSingleBean(JpaVendorAdapter.class);
			assertThat(context).hasSingleBean(HibernateJpaVendorAdapter.class);
			adapter.accept(context.getBean(HibernateJpaVendorAdapter.class));
		};
	}

	@Test
	void jtaDefaultPlatform() {
		this.contextRunner.withUserConfiguration(JtaTransactionManagerConfiguration.class)
			.run(assertJtaPlatform(SpringJtaPlatform.class));
	}

	@Test
	void jtaCustomPlatform() {
		this.contextRunner
			.withPropertyValues(
					"spring.jpa.properties.hibernate.transaction.jta.platform:" + TestJtaPlatform.class.getName())
			.withConfiguration(AutoConfigurations.of(JtaAutoConfiguration.class))
			.run(assertJtaPlatform(TestJtaPlatform.class));
	}

	@Test
	void jtaNotUsedByTheApplication() {
		this.contextRunner.run(assertJtaPlatform(NoJtaPlatform.class));
	}

	private ContextConsumer<AssertableApplicationContext> assertJtaPlatform(Class<? extends JtaPlatform> expectedType) {
		return (context) -> {
			SessionFactoryImpl sessionFactory = context.getBean(LocalContainerEntityManagerFactoryBean.class)
				.getNativeEntityManagerFactory()
				.unwrap(SessionFactoryImpl.class);
			assertThat(sessionFactory.getServiceRegistry().getService(JtaPlatform.class)).isInstanceOf(expectedType);
		};
	}

	@Test
	void jtaCustomTransactionManagerUsingProperties() {
		this.contextRunner
			.withPropertyValues("spring.transaction.default-timeout:30",
					"spring.transaction.rollback-on-commit-failure:true")
			.run((context) -> {
				JpaTransactionManager transactionManager = context.getBean(JpaTransactionManager.class);
				assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
				assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
			});
	}

	@Test
	void autoConfigurationBacksOffWithSeveralDataSources() {
		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DataSourceTransactionManagerAutoConfiguration.class,
					XADataSourceAutoConfiguration.class, JtaAutoConfiguration.class))
			.withUserConfiguration(TestTwoDataSourcesConfiguration.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				assertThat(context).doesNotHaveBean(EntityManagerFactory.class);
			});
	}

	@Test
	void providerDisablesAutoCommitIsConfigured() {
		this.contextRunner
			.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
					"spring.datasource.hikari.auto-commit:false")
			.run((context) -> {
				Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
					.getJpaPropertyMap();
				assertThat(jpaProperties).contains(entry("hibernate.connection.provider_disables_autocommit", "true"));
			});
	}

	@Test
	void providerDisablesAutoCommitIsNotConfiguredIfAutoCommitIsEnabled() {
		this.contextRunner
			.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
					"spring.datasource.hikari.auto-commit:true")
			.run((context) -> {
				Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
					.getJpaPropertyMap();
				assertThat(jpaProperties).doesNotContainKeys("hibernate.connection.provider_disables_autocommit");
			});
	}

	@Test
	void providerDisablesAutoCommitIsNotConfiguredIfPropertyIsSet() {
		this.contextRunner
			.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
					"spring.datasource.hikari.auto-commit:false",
					"spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false")
			.run((context) -> {
				Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
					.getJpaPropertyMap();
				assertThat(jpaProperties).contains(entry("hibernate.connection.provider_disables_autocommit", "false"));
			});
	}

	@Test
	void providerDisablesAutoCommitIsNotConfiguredWithJta() {
		this.contextRunner.withUserConfiguration(JtaTransactionManagerConfiguration.class)
			.withPropertyValues("spring.datasource.type:" + HikariDataSource.class.getName(),
					"spring.datasource.hikari.auto-commit:false")
			.run((context) -> {
				Map<String, Object> jpaProperties = context.getBean(LocalContainerEntityManagerFactoryBean.class)
					.getJpaPropertyMap();
				assertThat(jpaProperties).doesNotContainKeys("hibernate.connection.provider_disables_autocommit");
			});
	}

	@Test
	@WithResource(name = "META-INF/mappings/non-annotated.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8" ?>
					<entity-mappings xmlns="http://xmlns.jcp.org/xml/ns/persistence/orm"
									 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
									 xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence/orm https://www.oracle.com/webfolder/technetwork/jsc/xml/ns/persistence/orm_2_1.xsd"
									 version="2.1">
						<entity class="org.springframework.boot.hibernate.autoconfigure.mapping.NonAnnotatedEntity">
							<table name="NON_ANNOTATED"/>
							<attributes>
								<id name="id">
									<column name="id"/>
									<generated-value strategy="IDENTITY"/>
								</id>
								<basic name="item">
									<column name="item"/>
								</basic>
							</attributes>
						</entity>
					</entity-mappings>
					""")
	@WithResource(name = "non-annotated-data.sql",
			content = "INSERT INTO NON_ANNOTATED (id, item) values (2000, 'Test');")
	void customResourceMapping() {
		this.contextRunner.withClassLoader(new HideDataScriptClassLoader())
			.withPropertyValues("spring.sql.init.data-locations:classpath:non-annotated-data.sql",
					"spring.jpa.mapping-resources=META-INF/mappings/non-annotated.xml",
					"spring.jpa.defer-datasource-initialization=true")
			.run((context) -> {
				EntityManager em = context.getBean(EntityManagerFactory.class).createEntityManager();
				NonAnnotatedEntity found = em.find(NonAnnotatedEntity.class, 2000L);
				assertThat(found).isNotNull();
				assertThat(found.getItem()).isEqualTo("Test");
			});
	}

	@Test
	void physicalNamingStrategyCanBeUsed() {
		this.contextRunner.withUserConfiguration(TestPhysicalNamingStrategyConfiguration.class).run((context) -> {
			Map<String, Object> hibernateProperties = getVendorProperties(context);
			assertThat(hibernateProperties)
				.contains(entry("hibernate.physical_naming_strategy", context.getBean("testPhysicalNamingStrategy")));
			assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
		});
	}

	@Test
	void implicitNamingStrategyCanBeUsed() {
		this.contextRunner.withUserConfiguration(TestImplicitNamingStrategyConfiguration.class).run((context) -> {
			Map<String, Object> hibernateProperties = getVendorProperties(context);
			assertThat(hibernateProperties)
				.contains(entry("hibernate.implicit_naming_strategy", context.getBean("testImplicitNamingStrategy")));
			assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
		});
	}

	@Test
	void namingStrategyInstancesTakePrecedenceOverNamingStrategyProperties() {
		this.contextRunner
			.withUserConfiguration(TestPhysicalNamingStrategyConfiguration.class,
					TestImplicitNamingStrategyConfiguration.class)
			.withPropertyValues("spring.jpa.hibernate.naming.physical-strategy:com.example.Physical",
					"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit")
			.run((context) -> {
				Map<String, Object> hibernateProperties = getVendorProperties(context);
				assertThat(hibernateProperties).contains(
						entry("hibernate.physical_naming_strategy", context.getBean("testPhysicalNamingStrategy")),
						entry("hibernate.implicit_naming_strategy", context.getBean("testImplicitNamingStrategy")));
				assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
			});
	}

	@Test
	void hibernatePropertiesCustomizerTakesPrecedenceOverStrategyInstancesAndNamingStrategyProperties() {
		this.contextRunner
			.withUserConfiguration(TestHibernatePropertiesCustomizerConfiguration.class,
					TestPhysicalNamingStrategyConfiguration.class, TestImplicitNamingStrategyConfiguration.class)
			.withPropertyValues("spring.jpa.hibernate.naming.physical-strategy:com.example.Physical",
					"spring.jpa.hibernate.naming.implicit-strategy:com.example.Implicit")
			.run((context) -> {
				Map<String, Object> hibernateProperties = getVendorProperties(context);
				TestHibernatePropertiesCustomizerConfiguration configuration = context
					.getBean(TestHibernatePropertiesCustomizerConfiguration.class);
				assertThat(hibernateProperties).contains(
						entry("hibernate.physical_naming_strategy", configuration.physicalNamingStrategy),
						entry("hibernate.implicit_naming_strategy", configuration.implicitNamingStrategy));
				assertThat(hibernateProperties).doesNotContainKeys("hibernate.ejb.naming_strategy");
			});
	}

	@Test
	@WithResource(name = "city.sql",
			content = "INSERT INTO CITY (ID, NAME, STATE, COUNTRY, MAP) values (2000, 'Washington', 'DC', 'US', 'Google')")
	void eventListenerCanBeRegisteredAsBeans() {
		this.contextRunner.withUserConfiguration(TestInitializedJpaConfiguration.class)
			.withClassLoader(new HideDataScriptClassLoader())
			.withPropertyValues("spring.jpa.show-sql=true", "spring.jpa.hibernate.ddl-auto:create-drop",
					"spring.sql.init.data-locations:classpath:/city.sql",
					"spring.jpa.defer-datasource-initialization=true")
			.run((context) -> {
				// See CityListener
				assertThat(context).hasSingleBean(City.class);
				assertThat(context.getBean(City.class).getName()).isEqualTo("Washington");
			});
	}

	@Test
	void hibernatePropertiesCustomizerCanDisableBeanContainer() {
		this.contextRunner.withUserConfiguration(DisableBeanContainerConfiguration.class)
			.run((context) -> assertThat(context).doesNotHaveBean(City.class));
	}

	@Test
	void vendorPropertiesWithEmbeddedDatabaseAndNoDdlProperty() {
		this.contextRunner.run(vendorProperties((vendorProperties) -> {
			assertThat(vendorProperties).doesNotContainKeys(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
			assertThat(vendorProperties).containsEntry(SchemaToolingSettings.HBM2DDL_AUTO, "create-drop");
		}));
	}

	@Test
	void vendorPropertiesWhenDdlAutoPropertyIsSet() {
		this.contextRunner.withPropertyValues("spring.jpa.hibernate.ddl-auto=update")
			.run(vendorProperties((vendorProperties) -> {
				assertThat(vendorProperties).doesNotContainKeys(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
				assertThat(vendorProperties).containsEntry(SchemaToolingSettings.HBM2DDL_AUTO, "update");
			}));
	}

	@Test
	void vendorPropertiesWhenDdlAutoPropertyAndHibernatePropertiesAreSet() {
		this.contextRunner
			.withPropertyValues("spring.jpa.hibernate.ddl-auto=update",
					"spring.jpa.properties.hibernate.hbm2ddl.auto=create-drop")
			.run(vendorProperties((vendorProperties) -> {
				assertThat(vendorProperties).doesNotContainKeys(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
				assertThat(vendorProperties).containsEntry(SchemaToolingSettings.HBM2DDL_AUTO, "create-drop");
			}));
	}

	@Test
	void vendorPropertiesWhenDdlAutoPropertyIsSetToNone() {
		this.contextRunner.withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
			.run(vendorProperties((vendorProperties) -> assertThat(vendorProperties).doesNotContainKeys(
					SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, SchemaToolingSettings.HBM2DDL_AUTO)));
	}

	@Test
	void vendorPropertiesWhenJpaDdlActionIsSet() {
		this.contextRunner
			.withPropertyValues("spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create")
			.run(vendorProperties((vendorProperties) -> {
				assertThat(vendorProperties).containsEntry(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
						"create");
				assertThat(vendorProperties).doesNotContainKeys(SchemaToolingSettings.HBM2DDL_AUTO);
			}));
	}

	@Test
	void vendorPropertiesWhenBothDdlAutoPropertiesAreSet() {
		this.contextRunner
			.withPropertyValues("spring.jpa.properties.jakarta.persistence.schema-generation.database.action=create",
					"spring.jpa.hibernate.ddl-auto=create-only")
			.run(vendorProperties((vendorProperties) -> {
				assertThat(vendorProperties).containsEntry(SchemaToolingSettings.JAKARTA_HBM2DDL_DATABASE_ACTION,
						"create");
				assertThat(vendorProperties).containsEntry(SchemaToolingSettings.HBM2DDL_AUTO, "create-only");
			}));
	}

	private ContextConsumer<AssertableApplicationContext> vendorProperties(
			Consumer<Map<String, Object>> vendorProperties) {
		return (context) -> vendorProperties.accept(getVendorProperties(context));
	}

	private static Map<String, Object> getVendorProperties(ConfigurableApplicationContext context) {
		return context.getBean(HibernateJpaConfiguration.class).getVendorProperties(context.getBean(DataSource.class));
	}

	@Test
	void withSyncBootstrappingAnApplicationListenerThatUsesJpaDoesNotTriggerABeanCurrentlyInCreationException() {
		this.contextRunner.withUserConfiguration(JpaUsingApplicationListenerConfiguration.class).run((context) -> {
			assertThat(context).hasNotFailed();
			EventCapturingApplicationListener listener = context.getBean(EventCapturingApplicationListener.class);
			assertThat(listener.events).hasSize(1);
			assertThat(listener.events).hasOnlyElementsOfType(ContextRefreshedEvent.class);
		});
	}

	@Test
	void withAsyncBootstrappingAnApplicationListenerThatUsesJpaDoesNotTriggerABeanCurrentlyInCreationException() {
		this.contextRunner
			.withUserConfiguration(AsyncBootstrappingConfiguration.class,
					JpaUsingApplicationListenerConfiguration.class)
			.run((context) -> {
				assertThat(context).hasNotFailed();
				EventCapturingApplicationListener listener = context.getBean(EventCapturingApplicationListener.class);
				assertThat(listener.events).hasSize(1);
				assertThat(listener.events).hasOnlyElementsOfType(ContextRefreshedEvent.class);
				// createEntityManager requires Hibernate bootstrapping to be complete
				assertThatNoException()
					.isThrownBy(() -> context.getBean(EntityManagerFactory.class).createEntityManager());
			});
	}

	@Test
	@WithMetaInfPersistenceXmlResource
	void whenLocalContainerEntityManagerFactoryBeanHasNoJpaVendorAdapterAutoConfigurationSucceeds() {
		this.contextRunner
			.withUserConfiguration(
					TestConfigurationWithLocalContainerEntityManagerFactoryBeanWithNoJpaVendorAdapter.class)
			.run((context) -> {
				EntityManagerFactory factoryBean = context.getBean(EntityManagerFactory.class);
				Map<String, Object> map = factoryBean.getProperties();
				assertThat(map).containsEntry("configured", "manually");
			});
	}

	@Test
	void registersHintsForJtaClasses() {
		RuntimeHints hints = new RuntimeHints();
		new HibernateRuntimeHints().registerHints(hints, getClass().getClassLoader());
		for (String noJtaPlatformClass : Arrays.asList(
				"org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform",
				"org.hibernate.service.jta.platform.internal.NoJtaPlatform")) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(TypeReference.of(noJtaPlatformClass))
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
		}
	}

	@Test
	void registersHintsForNamingClasses() {
		RuntimeHints hints = new RuntimeHints();
		new HibernateRuntimeHints().registerHints(hints, getClass().getClassLoader());
		for (Class<?> noJtaPlatformClass : Arrays.asList(SpringImplicitNamingStrategy.class,
				PhysicalNamingStrategySnakeCaseImpl.class)) {
			assertThat(RuntimeHintsPredicates.reflection()
				.onType(noJtaPlatformClass)
				.withMemberCategories(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS)).accepts(hints);
		}
	}

	@Test
	@Disabled("gh-40177")
	void whenSpringJpaGenerateDdlIsNotSetThenTableIsNotCreated() {
		// spring.jpa.generated-ddl defaults to false but this test still fails because
		// we're using an embedded database which means that HibernateProperties defaults
		// hibernate.hbm2ddl.auto to create-drop, replacing the
		// hibernate.hbm2ddl.auto=none that comes from generate-ddl being false.
		this.contextRunner.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenSpringJpaGenerateDdlIsTrueThenTableIsCreated() {
		this.contextRunner.withPropertyValues("spring.jpa.generate-ddl=true")
			.run((context) -> assertThat(tablesFrom(context)).contains("CITY"));
	}

	@Test
	@Disabled("gh-40177")
	void whenSpringJpaGenerateDdlIsFalseThenTableIsNotCreated() {
		// This test fails because we're using an embedded database which means that
		// HibernateProperties defaults hibernate.hbm2ddl.auto to create-drop, replacing
		// the hibernate.hbm2ddl.auto=none that comes from setting generate-ddl to false.
		this.contextRunner.withPropertyValues("spring.jpa.generate-ddl=false")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenHbm2DdlAutoIsNoneThenTableIsNotCreated() {
		this.contextRunner.withPropertyValues("spring.jpa.properties.hibernate.hbm2ddl.auto=none")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenSpringJpaHibernateDdlAutoIsNoneThenTableIsNotCreated() {
		this.contextRunner.withPropertyValues("spring.jpa.hibernate.ddl-auto=none")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	@Disabled("gh-40177")
	void whenSpringJpaGenerateDdlIsTrueAndSpringJpaHibernateDdlAutoIsNoneThenTableIsNotCreated() {
		// This test fails because when ddl-auto is set to none, we remove
		// hibernate.hbm2ddl.auto from Hibernate properties. This then allows
		// spring.jpa.generate-ddl to set it to create-drop
		this.contextRunner.withPropertyValues("spring.jpa.generate-ddl=true", "spring.jpa.hibernate.ddl-auto=none")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenSpringJpaGenerateDdlIsTrueAndSpringJpaHibernateDdlAutoIsDropThenTableIsNotCreated() {
		this.contextRunner.withPropertyValues("spring.jpa.generate-ddl=true", "spring.jpa.hibernate.ddl-auto=drop")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenSpringJpaGenerateDdlIsTrueAndJakartaSchemaGenerationIsNoneThenTableIsNotCreated() {
		this.contextRunner
			.withPropertyValues("spring.jpa.generate-ddl=true",
					"spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	@Test
	void whenSpringJpaGenerateDdlIsTrueSpringJpaHibernateDdlAutoIsCreateAndJakartaSchemaGenerationIsNoneThenTableIsNotCreated() {
		this.contextRunner
			.withPropertyValues("spring.jpa.generate-ddl=true", "spring.jpa.hibernate.ddl-auto=create",
					"spring.jpa.properties.jakarta.persistence.schema-generation.database.action=none")
			.run((context) -> assertThat(tablesFrom(context)).doesNotContain("CITY"));
	}

	private List<String> tablesFrom(AssertableApplicationContext context) {
		DataSource dataSource = context.getBean(DataSource.class);
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		List<String> tables = jdbc.query("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES",
				(results, row) -> results.getString(1));
		return tables;
	}

	private Class<?>[] getManagedJavaTypes(EntityManager entityManager) {
		Set<ManagedType<?>> managedTypes = entityManager.getMetamodel().getManagedTypes();
		return managedTypes.stream().map(ManagedType::getJavaType).toArray(Class<?>[]::new);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class TestTwoDataSourcesConfiguration {

		@Bean
		DataSource firstDataSource() {
			return createRandomDataSource();
		}

		@Bean
		DataSource secondDataSource() {
			return createRandomDataSource();
		}

		private DataSource createRandomDataSource() {
			String url = "jdbc:h2:mem:init-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestTwoDataSourcesAndPrimaryConfiguration {

		@Bean
		@Primary
		DataSource firstDataSource() {
			return createRandomDataSource();
		}

		@Bean
		DataSource secondDataSource() {
			return createRandomDataSource();
		}

		private DataSource createRandomDataSource() {
			String url = "jdbc:h2:mem:init-" + UUID.randomUUID();
			return DataSourceBuilder.create().url(url).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestFilterConfiguration {

		@Bean
		OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
			return new OpenEntityManagerInViewFilter();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestFilterRegistrationConfiguration {

		@Bean
		FilterRegistrationBean<OpenEntityManagerInViewFilter> openEntityManagerInViewFilterFilterRegistrationBean() {
			return new FilterRegistrationBean<>();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestInterceptorManualConfiguration {

		@Bean
		OpenEntityManagerInViewInterceptor openEntityManagerInViewInterceptor() {
			return new ManualOpenEntityManagerInViewInterceptor();
		}

		static class ManualOpenEntityManagerInViewInterceptor extends OpenEntityManagerInViewInterceptor {

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfigurationWithEntityManagerFactoryBuilder extends TestConfiguration {

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(EntityManagerFactoryBuilder builder,
				DataSource dataSource) {
			return builder.dataSource(dataSource).properties(Map.of("configured", "manually")).build();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfigurationWithLocalContainerEntityManagerFactoryBean extends TestConfiguration {

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource, JpaVendorAdapter adapter) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(adapter);
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			factoryBean.setJpaPropertyMap(properties);
			return factoryBean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfigurationWithEntityManagerFactory extends TestConfiguration {

		@Bean
		EntityManagerFactory entityManagerFactory(DataSource dataSource, JpaVendorAdapter adapter) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setJpaVendorAdapter(adapter);
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			factoryBean.setJpaPropertyMap(properties);
			factoryBean.afterPropertiesSet();
			EntityManagerFactory bean = factoryBean.getObject();
			assertThat(bean).isNotNull();
			return bean;
		}

		@Bean
		PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
			JpaTransactionManager transactionManager = new JpaTransactionManager();
			transactionManager.setEntityManagerFactory(emf);
			return transactionManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	static class TestConfigurationWithTransactionManager {

		@Bean
		JpaTransactionManager testTransactionManager() {
			return new CustomJpaTransactionManager();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(Country.class)
	static class AutoConfigurePackageForCountry {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(HibernateJpaAutoConfigurationTests.class)
	static class TestConfigurationWithCustomPersistenceUnitManager {

		private final DataSource dataSource;

		TestConfigurationWithCustomPersistenceUnitManager(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Bean
		PersistenceUnitManager persistenceUnitManager() {
			DefaultPersistenceUnitManager persistenceUnitManager = new DefaultPersistenceUnitManager();
			persistenceUnitManager.setDefaultDataSource(this.dataSource);
			persistenceUnitManager.setPackagesToScan(City.class.getPackage().getName());
			return persistenceUnitManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(HibernateJpaAutoConfigurationTests.class)
	static class TestConfigurationWithCustomPersistenceUnitPostProcessors {

		@Bean
		EntityManagerFactoryBuilderCustomizer entityManagerFactoryBuilderCustomizer() {
			return (builder) -> builder.setPersistenceUnitPostProcessors(
					(pui) -> pui.addManagedClassName("customized.attribute.converter.class.name"));
		}

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(HibernateJpaAutoConfigurationTests.class)
	static class TestConfigurationWithMultipleCustomPersistenceUnitPostProcessors {

		@Bean
		EntityManagerFactoryBuilderCustomizer entityManagerFactoryBuilderCustomizer() {
			return (builder) -> builder.addPersistenceUnitPostProcessors(
					(pui) -> pui.addManagedClassName("customized.attribute.converter.class.name"));
		}

		@Bean
		EntityManagerFactoryBuilderCustomizer anotherEntityManagerFactoryBuilderCustomizer() {
			return (builder) -> builder.addPersistenceUnitPostProcessors(
					(pui) -> pui.addManagedClassName("customized.attribute.converter.class.anotherName"));
		}

	}

	static class CustomJpaTransactionManager extends JpaTransactionManager {

	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@WithResource(name = "META-INF/persistence.xml",
			content = """
					<?xml version="1.0" encoding="UTF-8"?>
					<persistence version="2.0" xmlns="http://java.sun.com/xml/ns/persistence" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://java.sun.com/xml/ns/persistence https://java.sun.com/xml/ns/persistence/persistence_2_0.xsd">
						<persistence-unit name="manually-configured">
							<class>org.springframework.boot.jpa.autoconfigure.test.city.City</class>
							<exclude-unlisted-classes>true</exclude-unlisted-classes>
						</persistence-unit>
					</persistence>
					""")
	protected @interface WithMetaInfPersistenceXmlResource {

	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@DependsOnDatabaseInitialization
	static class TestInitializedJpaConfiguration {

		private boolean called;

		@Autowired
		void validateDataSourceIsInitialized(EntityManagerFactory entityManagerFactory) {
			// Inject the entity manager to validate it is initialized at the injection
			// point
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			City city = entityManager.find(City.class, 2000L);
			assertThat(city).isNotNull();
			assertThat(city.getName()).isEqualTo("Washington");
			this.called = true;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestImplicitNamingStrategyConfiguration {

		@Bean
		ImplicitNamingStrategy testImplicitNamingStrategy() {
			return new SpringImplicitNamingStrategy();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestPhysicalNamingStrategyConfiguration {

		@Bean
		PhysicalNamingStrategy testPhysicalNamingStrategy() {
			return new PhysicalNamingStrategySnakeCaseImpl();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestHibernatePropertiesCustomizerConfiguration {

		private final PhysicalNamingStrategy physicalNamingStrategy = new PhysicalNamingStrategySnakeCaseImpl();

		private final ImplicitNamingStrategy implicitNamingStrategy = new SpringImplicitNamingStrategy();

		@Bean
		HibernatePropertiesCustomizer testHibernatePropertiesCustomizer() {
			return (hibernateProperties) -> {
				hibernateProperties.put("hibernate.physical_naming_strategy", this.physicalNamingStrategy);
				hibernateProperties.put("hibernate.implicit_naming_strategy", this.implicitNamingStrategy);
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DisableBeanContainerConfiguration {

		@Bean
		HibernatePropertiesCustomizer disableBeanContainerHibernatePropertiesCustomizer() {
			return (hibernateProperties) -> hibernateProperties.remove(ManagedBeanSettings.BEAN_CONTAINER);
		}

	}

	public static class TestJtaPlatform implements JtaPlatform {

		@Override
		public TransactionManager retrieveTransactionManager() {
			return mock(TransactionManager.class);
		}

		@Override
		public UserTransaction retrieveUserTransaction() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Object getTransactionIdentifier(Transaction transaction) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean canRegisterSynchronization() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void registerSynchronization(Synchronization synchronization) {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getCurrentStatus() {
			throw new UnsupportedOperationException();
		}

	}

	static class HideDataScriptClassLoader extends URLClassLoader {

		private static final List<String> HIDDEN_RESOURCES = Arrays.asList("schema-all.sql", "schema.sql");

		HideDataScriptClassLoader() {
			super(new URL[0], Thread.currentThread().getContextClassLoader());
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if (HIDDEN_RESOURCES.contains(name)) {
				return Collections.emptyEnumeration();
			}
			return super.getResources(name);
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class JpaUsingApplicationListenerConfiguration {

		@Bean
		EventCapturingApplicationListener jpaUsingApplicationListener(EntityManagerFactory emf) {
			return new EventCapturingApplicationListener();
		}

		static class EventCapturingApplicationListener implements ApplicationListener<ApplicationEvent> {

			private final List<ApplicationEvent> events = new ArrayList<>();

			@Override
			public void onApplicationEvent(ApplicationEvent event) {
				this.events.add(event);
			}

		}

	}

	@Configuration(proxyBeanMethods = false)
	static class AsyncBootstrappingConfiguration {

		@Bean
		ThreadPoolTaskExecutor ThreadPoolTaskExecutor() {
			return new ThreadPoolTaskExecutor();
		}

		@Bean
		EntityManagerFactoryBuilderCustomizer asyncBootstrappingCustomizer(ThreadPoolTaskExecutor executor) {
			return (builder) -> builder.setBootstrapExecutor(executor);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestConfigurationWithLocalContainerEntityManagerFactoryBeanWithNoJpaVendorAdapter
			extends TestConfiguration {

		@Bean
		LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(dataSource);
			factoryBean.setPersistenceUnitName("manually-configured");
			factoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			Map<String, Object> properties = new HashMap<>();
			properties.put("configured", "manually");
			properties.put("hibernate.transaction.jta.platform", NoJtaPlatform.INSTANCE);
			factoryBean.setJpaPropertyMap(properties);
			return factoryBean;
		}

	}

	public static class TestH2Dialect extends H2Dialect {

	}

	@Configuration(proxyBeanMethods = false)
	static class JtaTransactionManagerConfiguration {

		@Bean
		JtaTransactionManager jtaTransactionManager() {
			JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
			jtaTransactionManager.setUserTransaction(mock(UserTransaction.class));
			jtaTransactionManager.setTransactionManager(mock(TransactionManager.class));
			return jtaTransactionManager;
		}

	}

}
