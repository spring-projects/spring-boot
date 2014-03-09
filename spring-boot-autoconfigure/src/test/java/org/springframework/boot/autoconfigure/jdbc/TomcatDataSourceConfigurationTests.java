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

package org.springframework.boot.autoconfigure.jdbc;

import java.lang.reflect.Field;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 *
 * @author Dave Syer
 */
public class TomcatDataSourceConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void restore() {
		EmbeddedDatabaseConnection.override = null;
	}

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class));
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.url:jdbc:foo//bar/spam");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.testWhileIdle:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.testOnBorrow:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.testOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.timeBetweenEvictionRunsMillis:10000");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.minEvictableIdleTimeMillis:12345");
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals("jdbc:foo//bar/spam", ds.getUrl());
		assertEquals(true, ds.isTestWhileIdle());
		assertEquals(true, ds.isTestOnBorrow());
		assertEquals(true, ds.isTestOnReturn());
		assertEquals(10000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(12345, ds.getMinEvictableIdleTimeMillis());
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals(5000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(60000, ds.getMinEvictableIdleTimeMillis());
	}

	@Test(expected = BeanCreationException.class)
	public void testBadUrl() throws Exception {
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(TomcatDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test(expected = BeanCreationException.class)
	public void testBadDriverClass() throws Exception {
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(TomcatDataSourceConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}
}
