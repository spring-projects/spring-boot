/*
 * Copyright 2012-2014 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
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

	@Test
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

}
