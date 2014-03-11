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
import java.sql.Connection;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.Validator;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.*;

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
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.maxWait:1234");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.jdbcInterceptors:SlowQueryReport");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.validationInterval:9999");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.initSQL:SELECT 1");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.jmxEnabled:false");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.fairQueue:false");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.abandonWhenPercentageFull:49");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.maxAge:10");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.useEquals:false");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.suspectTimeout:100");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.rollbackOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.commitOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.alternateUsernameAllowed:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.dataSourceJNDI:jndi://db/bla");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.useDisposableConnectionFacade:false");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.logValidationErrors:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.propagateInterruptState:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.ignoreExceptionOnPreLoad:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.defaultAutoCommit:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.defaultReadOnly:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.defaultTransactionIsolation:SERIALIZABLE");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.defaultCatalog:blah");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.validationQueryTimeout:5544");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.validatorClassName:" + DummyValidator.class.getName());
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.removeAbandoned:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.removeAbandonedTimeout:6666");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.logAbandoned:true");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.maxActive:1000");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.minIdle:100");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.maxIdle:1000");
		EnvironmentTestUtils.addEnvironment(this.context, "spring.datasource.initialSize:100");
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals("jdbc:foo//bar/spam", ds.getUrl());
		assertEquals(true, ds.isTestWhileIdle());
		assertEquals(true, ds.isTestOnBorrow());
		assertEquals(true, ds.isTestOnReturn());
		assertEquals(10000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(12345, ds.getMinEvictableIdleTimeMillis());
		assertEquals(1234, ds.getMaxWait());
		assertEquals(9999L, ds.getValidationInterval());
		assertEquals("SELECT 1", ds.getInitSQL());
		assertFalse(ds.isJmxEnabled());
		assertFalse(ds.isFairQueue());
		assertEquals(49, ds.getAbandonWhenPercentageFull());
		assertEquals(10, ds.getMaxAge());
		assertFalse(ds.isUseEquals());
		assertEquals(100, ds.getSuspectTimeout());
		assertTrue(ds.getRollbackOnReturn());
		assertTrue(ds.getCommitOnReturn());
		assertTrue(ds.isAlternateUsernameAllowed());
		assertEquals("jndi://db/bla", ds.getDataSourceJNDI());
		assertFalse(ds.getUseDisposableConnectionFacade());
		assertTrue(ds.getLogValidationErrors());
		assertTrue(ds.getPropagateInterruptState());
		assertTrue(ds.isIgnoreExceptionOnPreLoad());
		assertTrue(ds.isDefaultAutoCommit());
		assertTrue(ds.isDefaultReadOnly());
		assertEquals(Connection.TRANSACTION_SERIALIZABLE, ds.getDefaultTransactionIsolation());
		assertEquals("blah", ds.getDefaultCatalog());
		assertEquals(5544, ds.getValidationQueryTimeout());
		assertEquals(DummyValidator.class.getName(), ds.getValidatorClassName());
		assertTrue(ds.isRemoveAbandoned());
		assertEquals(6666, ds.getRemoveAbandonedTimeout());
		assertTrue(ds.isLogAbandoned());
		assertEquals(1000, ds.getMaxActive());
		assertEquals(100, ds.getMinIdle());
		assertEquals(1000, ds.getMaxIdle());
		assertEquals(100, ds.getInitialSize());
		assertDataSourceHasInterceptors(ds);
	}

	private void assertDataSourceHasInterceptors(DataSourceProxy ds) throws ClassNotFoundException {
		PoolProperties.InterceptorDefinition[] interceptors = ds.getJdbcInterceptorsAsArray();
		for (PoolProperties.InterceptorDefinition interceptor : interceptors) {
			if (SlowQueryReport.class == interceptor.getInterceptorClass()) {
				return;
			}
		}
		fail("SlowQueryReport interceptor should have been set.");
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals(5000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(60000, ds.getMinEvictableIdleTimeMillis());
		assertEquals(30000, ds.getMaxWait());
		assertEquals(30000L, ds.getValidationInterval());
		assertEquals(0, ds.getAbandonWhenPercentageFull());
		assertNull(ds.getInitSQL());
		assertTrue(ds.isJmxEnabled());
		assertTrue(ds.isFairQueue());
		assertEquals(0, ds.getMaxAge());
		assertTrue(ds.isUseEquals());
		assertEquals(0, ds.getSuspectTimeout());
		assertFalse(ds.getRollbackOnReturn());
		assertFalse(ds.getCommitOnReturn());
		assertFalse(ds.isAlternateUsernameAllowed());
		assertNull(ds.getDataSourceJNDI());
		assertTrue(ds.getUseDisposableConnectionFacade());
		assertFalse(ds.getLogValidationErrors());
		assertFalse(ds.getPropagateInterruptState());
		assertFalse(ds.isIgnoreExceptionOnPreLoad());
		assertNull(ds.isDefaultAutoCommit());
		assertNull(ds.isDefaultReadOnly());
		assertNull(ds.getDefaultCatalog());
		assertEquals(-1, ds.getValidationQueryTimeout());
		assertFalse(ds.isRemoveAbandoned());
		assertEquals(60, ds.getRemoveAbandonedTimeout());
		assertFalse(ds.isLogAbandoned());
		assertEquals(100, ds.getMaxActive());
		assertEquals(10, ds.getMinIdle());
		assertEquals(100, ds.getMaxIdle());
		assertEquals(10, ds.getInitialSize());

	}

	@Test(expected = BeanCreationException.class)
	public void testBadUrl() throws Exception {
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(TomcatDataSourceConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test(expected = BeanCreationException.class)
	public void testBadDriverClass() throws Exception {
		EmbeddedDatabaseConnection.override = EmbeddedDatabaseConnection.NONE;
		this.context.register(TomcatDataSourceConfiguration.class, PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}

	public static class DummyValidator implements Validator {
		@Override
		public boolean validate(Connection connection, int validateAction) {
			return false;
		}
	}
}


