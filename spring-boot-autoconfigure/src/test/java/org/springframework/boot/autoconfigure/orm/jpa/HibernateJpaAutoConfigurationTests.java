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

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
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

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Override
	protected Class<?> getAutoConfigureClass() {
		return HibernateJpaAutoConfiguration.class;
	}

	@Test
	public void testDataScriptWithMissingDdl() throws Exception {
		this.thrown.expectMessage("ddl.sql");
		this.thrown.expectMessage("spring.datasource.schema");
		load("spring.datasource.data:classpath:/city.sql",
				// Missing:
				"spring.datasource.schema:classpath:/ddl.sql");
	}

	@Test
	public void testDataScript() throws Exception {
		// This can't succeed because the data SQL is executed immediately after the
		// schema
		// and Hibernate hasn't initialized yet at that point
		this.thrown.expect(BeanCreationException.class);
		load("spring.datasource.data:classpath:/city.sql");
	}

	@Test
	public void testDataScriptRunsEarly() {
		load(new Class<?>[] { TestInitializedJpaConfiguration.class }, null,
				new HideDataScriptClassLoader(), "spring.jpa.show-sql=true",
				"spring.jpa.hibernate.ddl-auto:create-drop",
				"spring.datasource.data:classpath:/city.sql");
		assertThat(this.context.getBean(TestInitializedJpaConfiguration.class).called)
				.isTrue();
	}

	@Test
	public void testFlywaySwitchOffDdlAuto() throws Exception {
		load(new Class<?>[0], new Class<?>[] { FlywayAutoConfiguration.class },
				"spring.datasource.initialize:false",
				"spring.flyway.locations:classpath:db/city");
	}

	@Test
	public void testFlywayPlusValidation() throws Exception {
		load(new Class<?>[0], new Class<?>[] { FlywayAutoConfiguration.class },
				"spring.datasource.initialize:false",
				"spring.flyway.locations:classpath:db/city",
				"spring.jpa.hibernate.ddl-auto:validate");
	}

	@Test
	public void testLiquibasePlusValidation() throws Exception {
		load(new Class<?>[0], new Class<?>[] { LiquibaseAutoConfiguration.class },
				"spring.datasource.initialize:false",
				"spring.liquibase.changeLog:classpath:db/changelog/db.changelog-city.yaml",
				"spring.jpa.hibernate.ddl-auto:validate");
	}

	@Test
	public void defaultJtaPlatform() throws Exception {
		load(JtaAutoConfiguration.class);
		Map<String, Object> jpaPropertyMap = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class)
				.getJpaPropertyMap();
		assertThat(jpaPropertyMap.get("hibernate.transaction.jta.platform"))
				.isInstanceOf(SpringJtaPlatform.class);
	}

	@Test
	public void testCustomJtaPlatform() throws Exception {
		load(JtaAutoConfiguration.class,
				"spring.jpa.properties.hibernate.transaction.jta.platform:"
						+ TestJtaPlatform.class.getName());
		Map<String, Object> jpaPropertyMap = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class)
				.getJpaPropertyMap();
		assertThat((String) jpaPropertyMap.get("hibernate.transaction.jta.platform"))
				.isEqualTo(TestJtaPlatform.class.getName());
	}

	@Test
	public void testCustomJpaTransactionManagerUsingProperties() throws Exception {
		load("spring.transaction.default-timeout:30",
				"spring.transaction.rollback-on-commit-failure:true");
		JpaTransactionManager transactionManager = this.context
				.getBean(JpaTransactionManager.class);
		assertThat(transactionManager.getDefaultTimeout()).isEqualTo(30);
		assertThat(transactionManager.isRollbackOnCommitFailure()).isTrue();
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
