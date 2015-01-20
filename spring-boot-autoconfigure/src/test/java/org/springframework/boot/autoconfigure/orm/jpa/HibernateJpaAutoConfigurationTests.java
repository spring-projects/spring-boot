/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.Map;

import javax.sql.DataSource;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.JtaAutoConfiguration;
import org.springframework.boot.autoconfigure.jta.JtaProperties;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class HibernateJpaAutoConfigurationTests extends AbstractJpaAutoConfigurationTests {

	@Override
	protected Class<?> getAutoConfigureClass() {
		return HibernateJpaAutoConfiguration.class;
	}

	@Test
	public void testDataScriptWithMissingDdl() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.data:classpath:/city.sql",
				// Missing:
				"spring.datasource.schema:classpath:/ddl.sql");
		setupTestConfiguration();
		this.context.refresh();
		assertEquals(new Integer(1),
				new JdbcTemplate(this.context.getBean(DataSource.class)).queryForObject(
						"SELECT COUNT(*) from CITY", Integer.class));
	}

	// This can't succeed because the data SQL is executed immediately after the schema
	// and Hibernate hasn't initialized yet at that point
	@Test(expected = BeanCreationException.class)
	public void testDataScript() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.data:classpath:/city.sql");
		setupTestConfiguration();
		this.context.refresh();
		assertEquals(new Integer(1),
				new JdbcTemplate(this.context.getBean(DataSource.class)).queryForObject(
						"SELECT COUNT(*) from CITY", Integer.class));
	}

	@Test
	public void testCustomNamingStrategy() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.hibernate.namingStrategy:"
						+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap().get(
				"hibernate.ejb.naming_strategy");
		assertThat(actual, equalTo("org.hibernate.cfg.EJB3NamingStrategy"));
	}

	@Test
	public void testNamingStrategyThatWorkedInOneDotOhContinuesToWork() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.hibernate.namingstrategy:"
						+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();

		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap().get(
				"hibernate.ejb.naming_strategy");
		assertThat(actual, equalTo("org.hibernate.cfg.EJB3NamingStrategy"));
	}

	@Test
	public void testCustomNamingStrategyViaJpaProperties() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.properties.hibernate.ejb.naming_strategy:"
						+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap().get(
				"hibernate.ejb.naming_strategy");
		// You can't override this one from spring.jpa.properties because it has an
		// opinionated default
		assertThat(actual, not(equalTo("org.hibernate.cfg.EJB3NamingStrategy")));
	}

	@Test
	public void testFlywayPlusValidation() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"flyway.locations:classpath:db/city",
				"spring.jpa.hibernate.ddl-auto:validate");
		setupTestConfiguration();
		this.context.register(FlywayAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void testLiquibasePlusValidation() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"liquibase.changeLog:classpath:db/changelog/db.changelog-city.yaml",
				"spring.jpa.hibernate.ddl-auto:validate");
		setupTestConfiguration();
		this.context.register(LiquibaseAutoConfiguration.class);
		this.context.refresh();
	}

	@Test
	public void testCustomJtaPlatform() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.properties.hibernate.transaction.jta.platform:"
						+ TestJtaPlatform.class.getName());
		this.context.register(JtaProperties.class, JtaAutoConfiguration.class);
		setupTestConfiguration();
		this.context.refresh();
		Map<String, Object> jpaPropertyMap = this.context.getBean(
				LocalContainerEntityManagerFactoryBean.class).getJpaPropertyMap();
		assertThat((String) jpaPropertyMap.get("hibernate.transaction.jta.platform"),
				equalTo(TestJtaPlatform.class.getName()));
	}

	public static class TestJtaPlatform implements JtaPlatform {

		@Override
		public TransactionManager retrieveTransactionManager() {
			throw new UnsupportedOperationException();
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

}
