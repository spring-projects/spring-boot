/*
 * Copyright 2012-2016 the original author or authors.
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
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class HibernateJpaAutoConfigurationTests
		extends AbstractJpaAutoConfigurationTests {

	@After
	public void cleanup() {
		HibernateVersion.setRunning(null);
	}

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
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForObject("SELECT COUNT(*) from CITY", Integer.class)).isEqualTo(1);
	}

	// This can't succeed because the data SQL is executed immediately after the schema
	// and Hibernate hasn't initialized yet at that point
	@Test(expected = BeanCreationException.class)
	public void testDataScript() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.data:classpath:/city.sql");
		setupTestConfiguration();
		this.context.refresh();
		assertThat(new JdbcTemplate(this.context.getBean(DataSource.class))
				.queryForObject("SELECT COUNT(*) from CITY", Integer.class)).isEqualTo(1);
	}

	@Test
	public void testCustomNamingStrategy() throws Exception {
		HibernateVersion.setRunning(HibernateVersion.V4);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.hibernate.namingStrategy:"
						+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap()
				.get("hibernate.ejb.naming_strategy");
		assertThat(actual).isEqualTo("org.hibernate.cfg.EJB3NamingStrategy");
	}

	@Test
	public void testCustomNamingStrategyViaJpaProperties() throws Exception {
		HibernateVersion.setRunning(HibernateVersion.V4);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.properties.hibernate.ejb.naming_strategy:"
						+ "org.hibernate.cfg.EJB3NamingStrategy");
		setupTestConfiguration();
		this.context.refresh();
		LocalContainerEntityManagerFactoryBean bean = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class);
		String actual = (String) bean.getJpaPropertyMap()
				.get("hibernate.ejb.naming_strategy");
		// You can't override this one from spring.jpa.properties because it has an
		// opinionated default
		assertThat(actual).isNotEqualTo("org.hibernate.cfg.EJB3NamingStrategy");
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
	public void defaultJtaPlatform() throws Exception {
		this.context.register(JtaAutoConfiguration.class);
		setupTestConfiguration();
		this.context.refresh();
		Map<String, Object> jpaPropertyMap = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class)
				.getJpaPropertyMap();
		assertThat(jpaPropertyMap.get("hibernate.transaction.jta.platform"))
				.isInstanceOf(SpringJtaPlatform.class);
	}

	@Test
	public void testCustomJtaPlatform() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.properties.hibernate.transaction.jta.platform:"
						+ TestJtaPlatform.class.getName());
		this.context.register(JtaAutoConfiguration.class);
		setupTestConfiguration();
		this.context.refresh();
		Map<String, Object> jpaPropertyMap = this.context
				.getBean(LocalContainerEntityManagerFactoryBean.class)
				.getJpaPropertyMap();
		assertThat((String) jpaPropertyMap.get("hibernate.transaction.jta.platform"))
				.isEqualTo(TestJtaPlatform.class.getName());
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
