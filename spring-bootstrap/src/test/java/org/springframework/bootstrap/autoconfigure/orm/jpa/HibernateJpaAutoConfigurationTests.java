/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.Test;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.bootstrap.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.bootstrap.autoconfigure.orm.jpa.test.City;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.orm.jpa.JpaTransactionManager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
public class HibernateJpaAutoConfigurationTests {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testEntityManagerCreated() throws Exception {
		this.context.register(EmbeddedDatabaseConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(JpaTransactionManager.class));
	}

	@Test
	public void testDataSourceTransactionManagerNotCreated() throws Exception {
		this.context.register(EmbeddedDatabaseConfiguration.class,
				HibernateJpaAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertTrue(this.context.getBean("transactionManager") instanceof JpaTransactionManager);
	}

	@ComponentScan(basePackageClasses = { City.class })
	protected static class TestConfiguration {

	}
}
