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

package org.springframework.boot.autoconfigure.jdbc;

import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.jndi.JndiPropertiesHidingClassLoader;
import org.springframework.boot.autoconfigure.jndi.TestableInitialContextFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.MBeanExporter;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link JndiDataSourceAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class JndiDataSourceAutoConfigurationTests {

	private ClassLoader threadContextClassLoader;

	private String initialContextFactory;

	private AnnotationConfigApplicationContext context;

	@Before
	public void setupJndi() {
		this.initialContextFactory = System.getProperty(Context.INITIAL_CONTEXT_FACTORY);
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
				TestableInitialContextFactory.class.getName());
	}

	@Before
	public void setupThreadContextClassLoader() {
		this.threadContextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(
				new JndiPropertiesHidingClassLoader(getClass().getClassLoader()));
	}

	@After
	public void close() {
		TestableInitialContextFactory.clearAll();
		if (this.initialContextFactory != null) {
			System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
					this.initialContextFactory);
		}
		else {
			System.clearProperty(Context.INITIAL_CONTEXT_FACTORY);
		}
		if (this.context != null) {
			this.context.close();
		}
		Thread.currentThread().setContextClassLoader(this.threadContextClassLoader);
	}

	@Test
	public void dataSourceIsAvailableFromJndi() throws IllegalStateException,
			NamingException {
		DataSource dataSource = new BasicDataSource();
		configureJndi("foo", dataSource);

		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jndi-name:foo");
		this.context.register(JndiDataSourceAutoConfiguration.class);
		this.context.refresh();

		assertEquals(dataSource, this.context.getBean(DataSource.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void mbeanDataSourceIsExcludedFromExport() throws IllegalStateException,
			NamingException {
		DataSource dataSource = new BasicDataSource();
		configureJndi("foo", dataSource);

		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jndi-name:foo");
		this.context.register(JndiDataSourceAutoConfiguration.class,
				MBeanExporterConfiguration.class);
		this.context.refresh();

		assertEquals(dataSource, this.context.getBean(DataSource.class));
		MBeanExporter exporter = this.context.getBean(MBeanExporter.class);
		Set<String> excludedBeans = (Set<String>) new DirectFieldAccessor(exporter)
				.getPropertyValue("excludedBeans");
		assertThat(excludedBeans, contains("dataSource"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void standardDataSourceIsNotExcludedFromExport() throws IllegalStateException,
			NamingException {
		DataSource dataSource = new org.apache.commons.dbcp.BasicDataSource();
		configureJndi("foo", dataSource);

		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jndi-name:foo");
		this.context.register(JndiDataSourceAutoConfiguration.class,
				MBeanExporterConfiguration.class);
		this.context.refresh();

		assertEquals(dataSource, this.context.getBean(DataSource.class));
		MBeanExporter exporter = this.context.getBean(MBeanExporter.class);
		Set<String> excludedBeans = (Set<String>) new DirectFieldAccessor(exporter)
				.getPropertyValue("excludedBeans");
		assertThat(excludedBeans, hasSize(0));
	}

	private void configureJndi(String name, DataSource dataSource)
			throws IllegalStateException, NamingException {
		TestableInitialContextFactory.bind(name, dataSource);
	}

	private static class MBeanExporterConfiguration {

		@Bean
		MBeanExporter mbeanExporter() {
			return new MBeanExporter();
		}
	}

}
