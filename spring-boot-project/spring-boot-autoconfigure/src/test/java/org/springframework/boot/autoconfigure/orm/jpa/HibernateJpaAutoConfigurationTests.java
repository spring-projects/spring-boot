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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.XADataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.mapping.NonAnnotatedEntity;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Stephane Nicoll
 */
public class HibernateJpaAutoConfigurationTests
		extends AbstractJpaAutoConfigurationTests {

	public HibernateJpaAutoConfigurationTests() {
		super(HibernateJpaAutoConfiguration.class);
	}

	@Test
	public void testDataScriptWithMissingDdl() {
		contextRunner().withPropertyValues("spring.datasource.data:classpath:/city.sql",
				// Missing:
				"spring.datasource.schema:classpath:/ddl.sql").run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasMessageContaining("ddl.sql");
					assertThat(context.getStartupFailure())
							.hasMessageContaining("spring.datasource.schema");
				});
	}

	@Test
	public void testDataScript() {
		// This can't succeed because the data SQL is executed immediately after the
		// schema
		// and Hibernate hasn't initialized yet at that point
		contextRunner().withPropertyValues("spring.datasource.data:classpath:/city.sql")
				.run((context) -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.isInstanceOf(BeanCreationException.class);
				});
	}

	@Test
	public void testDataScriptRunsEarly() {
		contextRunner().withUserConfiguration(TestInitializedJpaConfiguration.class)
				.withClassLoader(new HideDataScriptClassLoader())
				.withPropertyValues("spring.jpa.show-sql=true",
						"spring.jpa.hibernate.ddl-auto:create-drop",
						"spring.datasource.data:classpath:/city.sql")
				.run((context) -> assertThat(
						context.getBean(TestInitializedJpaConfiguration.class).called)
								.isTrue());
	}

	@Test
	public void testFlywaySwitchOffDdlAuto() {
		contextRunner()
				.withPropertyValues("spring.datasource.initialization-mode:never",
						"spring.flyway.locations:classpath:db/city")
				.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void testFlywayPlusValidation() {
		contextRunner()
				.withPropertyValues("spring.datasource.initialization-mode:never",
						"spring.flyway.locations:classpath:db/city",
						"spring.jpa.hibernate.ddl-auto:validate")
				.withConfiguration(AutoConfigurations.of(FlywayAutoConfiguration.class))
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void testLiquibasePlusValidation() {
		contextRunner()
				.withPropertyValues("spring.datasource.initialization-mode:never",
						"spring.liquibase.changeLog:classpath:db/changelog/db.changelog-city.yaml",
						"spring.jpa.hibernate.ddl-auto:validate")
				.withConfiguration(
						AutoConfigurations.of(LiquibaseAutoConfiguration.class))
				.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	public void jtaDefaultPlatform() {
		contextRunner()
				.withConfiguration(AutoConfigurations.of(JtaAutoConfiguration.class))
				.run((context) -> {
					Map<String, Object> jpaPropertyMap = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat(jpaPropertyMap.get("hibernate.transaction.jta.platform"))
							.isInstanceOf(SpringJtaPlatform.class);
				});
	}

	@Test
	public void jtaCustomPlatform() {
		contextRunner()
				.withPropertyValues(
						"spring.jpa.properties.hibernate.transaction.jta.platform:"
								+ TestJtaPlatform.class.getName())
				.withConfiguration(AutoConfigurations.of(JtaAutoConfiguration.class))
				.run((context) -> {
					Map<String, Object> jpaPropertyMap = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat((String) jpaPropertyMap
							.get("hibernate.transaction.jta.platform"))
									.isEqualTo(TestJtaPlatform.class.getName());
				});
	}

	@Test
	public void jtaCustomTransactionManagerUsingProperties() {
		contextRunner()
				.withPropertyValues("spring.transaction.default-timeout:30",
						"spring.transaction.rollback-on-commit-failure:true")
				.run((context) -> {
					JpaTransactionManager transactionManager = context
							.getBean(JpaTransactionManager.class);
					assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
					assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
				});
	}

	@Test
	public void autoConfigurationBacksOffWithSeveralDataSources() {
		contextRunner()
				.withConfiguration(AutoConfigurations.of(
						DataSourceTransactionManagerAutoConfiguration.class,
						XADataSourceAutoConfiguration.class, JtaAutoConfiguration.class))
				.withUserConfiguration(TestTwoDataSourcesConfiguration.class)
				.run((context) -> {
					assertThat(context).hasNotFailed();
					assertThat(context).doesNotHaveBean(EntityManagerFactory.class);
				});
	}

	@Test
	public void providerDisablesAutoCommitIsConfigured() {
		contextRunner().withPropertyValues(
				"spring.datasource.type:" + HikariDataSource.class.getName(),
				"spring.datasource.hikari.auto-commit:false").run((context) -> {
					Map<String, Object> jpaProperties = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat(jpaProperties).contains(entry(
							"hibernate.connection.provider_disables_autocommit", "true"));
				});
	}

	@Test
	public void providerDisablesAutoCommitIsNotConfiguredIfAutoCommitIsEnabled() {
		contextRunner().withPropertyValues(
				"spring.datasource.type:" + HikariDataSource.class.getName(),
				"spring.datasource.hikari.auto-commit:true").run((context) -> {
					Map<String, Object> jpaProperties = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat(jpaProperties).doesNotContainKeys(
							"hibernate.connection.provider_disables_autocommit");
				});
	}

	@Test
	public void providerDisablesAutoCommitIsNotConfiguredIfPropertyIsSet() {
		contextRunner()
				.withPropertyValues(
						"spring.datasource.type:" + HikariDataSource.class.getName(),
						"spring.datasource.hikari.auto-commit:false",
						"spring.jpa.properties.hibernate.connection.provider_disables_autocommit=false")
				.run((context) -> {
					Map<String, Object> jpaProperties = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat(jpaProperties).contains(
							entry("hibernate.connection.provider_disables_autocommit",
									"false"));
				});
	}

	@Test
	public void providerDisablesAutoCommitIsNotConfiguredWithJta() {
		contextRunner()
				.withConfiguration(AutoConfigurations.of(JtaAutoConfiguration.class))
				.withPropertyValues(
						"spring.datasource.type:" + HikariDataSource.class.getName(),
						"spring.datasource.hikari.auto-commit:false")
				.run((context) -> {
					Map<String, Object> jpaProperties = context
							.getBean(LocalContainerEntityManagerFactoryBean.class)
							.getJpaPropertyMap();
					assertThat(jpaProperties).doesNotContainKeys(
							"hibernate.connection.provider_disables_autocommit");
				});
	}

	@Test
	public void customResourceMapping() {
		contextRunner().withClassLoader(new HideDataScriptClassLoader())
				.withPropertyValues(
						"spring.datasource.data:classpath:/db/non-annotated-data.sql",
						"spring.jpa.mapping-resources=META-INF/mappings/non-annotated.xml")
				.run((context) -> {
					EntityManager em = context.getBean(EntityManagerFactory.class)
							.createEntityManager();
					NonAnnotatedEntity found = em.find(NonAnnotatedEntity.class, 2000L);
					assertThat(found).isNotNull();
					assertThat(found.getValue()).isEqualTo("Test");
				});
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	static class TestInitializedJpaConfiguration {

		private boolean called;

		@Autowired
		public void validateDataSourceIsInitialized(
				EntityManagerFactory entityManagerFactory) {
			// Inject the entity manager to validate it is initialized at the injection
			// point
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			City city = entityManager.find(City.class, 2000L);
			assertThat(city).isNotNull();
			assertThat(city.getName()).isEqualTo("Washington");
			this.called = true;
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
		public int getCurrentStatus() throws SystemException {
			throw new UnsupportedOperationException();
		}

	}

	private static class HideDataScriptClassLoader extends URLClassLoader {

		private static final List<String> HIDDEN_RESOURCES = Arrays
				.asList("schema-all.sql", "schema.sql");

		HideDataScriptClassLoader() {
			super(new URL[0], HideDataScriptClassLoader.class.getClassLoader());
		}

		@Override
		public Enumeration<URL> getResources(String name) throws IOException {
			if (HIDDEN_RESOURCES.contains(name)) {
				return new Vector<URL>().elements();
			}
			return super.getResources(name);
		}

	}

}
