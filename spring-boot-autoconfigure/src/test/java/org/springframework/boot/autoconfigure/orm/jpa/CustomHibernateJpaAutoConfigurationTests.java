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

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.test.City;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class CustomHibernateJpaAutoConfigurationTests {

	protected AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		this.context.close();
	}

	@Test
	public void testDefaultDdlAutoForMySql() throws Exception {
		// Set up environment so we get a MySQL database but don't require server to be
		// running...
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.driverClassName:com.mysql.jdbc.Driver",
				"spring.datasource.url:jdbc:mysql://localhost/nonexistent",
				"spring.datasource.initialize:false", "spring.jpa.database:MYSQL");
		this.context.register(TestConfiguration.class, DataSourceAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class);
		this.context.refresh();
		JpaProperties bean = this.context.getBean(JpaProperties.class);
		DataSource dataSource = this.context.getBean(DataSource.class);
		String actual = bean.getHibernateProperties(dataSource)
				.get("hibernate.hbm2ddl.auto");
		// Default is generic and safe
		assertThat(actual, nullValue());
	}

	@Test
	public void testDefaultDdlAutoForEmbedded() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class);
		this.context.refresh();
		JpaProperties bean = this.context.getBean(JpaProperties.class);
		DataSource dataSource = this.context.getBean(DataSource.class);
		String actual = bean.getHibernateProperties(dataSource)
				.get("hibernate.hbm2ddl.auto");
		assertThat(actual, equalTo("create-drop"));
	}

	@Test
	public void testNamingStrategyDelegatorTakesPrecedence() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.jpa.properties.hibernate.ejb.naming_strategy_delegator:"
						+ "org.hibernate.cfg.naming.ImprovedNamingStrategyDelegator");
		this.context.register(TestConfiguration.class,
				EmbeddedDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				HibernateJpaAutoConfiguration.class);
		this.context.refresh();
		JpaProperties bean = this.context.getBean(JpaProperties.class);
		DataSource dataSource = this.context.getBean(DataSource.class);
		Map<String, String> hibernateProperties = bean.getHibernateProperties(dataSource);
		assertThat(hibernateProperties.get("hibernate.ejb.naming_strategy"), nullValue());
	}

	@Configuration
	@TestAutoConfigurationPackage(City.class)
	protected static class TestConfiguration {

	}
}
