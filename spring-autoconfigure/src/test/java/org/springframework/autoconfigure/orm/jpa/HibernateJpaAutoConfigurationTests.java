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

package org.springframework.autoconfigure.orm.jpa;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.autoconfigure.ComponentScanDetectorConfiguration;
import org.springframework.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.autoconfigure.jdbc.EmbeddedDatabaseConfiguration;
import org.springframework.autoconfigure.orm.jpa.test.City;
import org.springframework.bootstrap.TestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link HibernateJpaAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class HibernateJpaAutoConfigurationTests {

	private ConfigurableApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testEntityManagerCreated() throws Exception {
		((AnnotationConfigApplicationContext) this.context).register(
				ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(JpaTransactionManager.class));
	}

	@Test
	public void testDataSourceTransactionManagerNotCreated() throws Exception {
		((AnnotationConfigApplicationContext) this.context).register(
				ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				DataSourceTransactionManagerAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertTrue(this.context.getBean("transactionManager") instanceof JpaTransactionManager);
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorCreated() throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, TestConfiguration.class);
		this.context = context;
		this.context.refresh();
		assertNotNull(this.context.getBean(OpenEntityManagerInViewInterceptor.class));
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorNotRegisteredWhenFilterPresent()
			throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(TestFilterConfiguration.class,
				ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context = context;
		this.context.refresh();
		assertEquals(0, getInterceptorBeans().length);
	}

	@Test
	public void testOpenEntityManagerInViewInterceptorNotRegisteredWhenExplicitlyOff()
			throws Exception {
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		TestUtils.addEnviroment(context, "spring.jpa.open_in_view:false");
		context.register(TestConfiguration.class,
				ComponentScanDetectorConfiguration.class,
				EmbeddedDatabaseConfiguration.class, HibernateJpaAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context = context;
		this.context.refresh();
		assertEquals(0, getInterceptorBeans().length);
	}

	private String[] getInterceptorBeans() {
		return this.context.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class);
	}

	@ComponentScan(basePackageClasses = { City.class })
	protected static class TestConfiguration {

	}

	@ComponentScan(basePackageClasses = { City.class })
	@Configuration
	protected static class TestFilterConfiguration {
		@Bean
		public OpenEntityManagerInViewFilter openEntityManagerInViewFilter() {
			return new OpenEntityManagerInViewFilter();
		}
	}
}
