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

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 *
 * @author Dave Syer
 */
public class TomcatDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.";

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "initialize:false");
	}

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
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "url:jdbc:foo//bar/spam");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testWhileIdle:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnBorrow:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "timeBetweenEvictionRunsMillis:10000");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "minEvictableIdleTimeMillis:12345");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "maxWait:1234");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "jdbcInterceptors:SlowQueryReport");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "validationInterval:9999");
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context
				.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals("jdbc:foo//bar/spam", ds.getUrl());
		assertTrue(ds.isTestWhileIdle());
		assertTrue(ds.isTestOnBorrow());
		assertTrue(ds.isTestOnReturn());
		assertEquals(10000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(12345, ds.getMinEvictableIdleTimeMillis());
		assertEquals(1234, ds.getMaxWait());
		assertEquals(9999L, ds.getValidationInterval());
		assertDataSourceHasInterceptors(ds);
	}

	private void assertDataSourceHasInterceptors(DataSourceProxy ds)
			throws ClassNotFoundException {
		PoolProperties.InterceptorDefinition[] interceptors = ds
				.getJdbcInterceptorsAsArray();
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
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context
				.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertEquals(5000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(60000, ds.getMinEvictableIdleTimeMillis());
		assertEquals(30000, ds.getMaxWait());
		assertEquals(30000L, ds.getValidationInterval());
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}

	@Configuration
	@Import(DataSourceAutoConfiguration.class)
	protected static class TomcatDataSourceConfiguration {

		@Autowired
		private DataSourceProperties properties;

		@Bean
		@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
		public DataSource dataSource() {
			DataSourceBuilder factory = DataSourceBuilder
					.create(this.properties.getClassLoader())
					.driverClassName(this.properties.getDriverClassName())
					.url(this.properties.getUrl())
					.username(this.properties.getUsername())
					.password(this.properties.getPassword())
					.type(org.apache.tomcat.jdbc.pool.DataSource.class);
			return factory.build();
		}

	}

}
