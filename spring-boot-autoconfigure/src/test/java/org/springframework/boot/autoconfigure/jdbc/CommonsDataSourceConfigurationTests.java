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

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.junit.Test;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link CommonsDataSourceConfiguration}.
 * 
 * @author Dave Syer
 */
public class CommonsDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.";

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(CommonsDataSourceConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		this.context.close();
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(CommonsDataSourceConfiguration.class);
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
		this.context.refresh();
		BasicDataSource ds = this.context.getBean(BasicDataSource.class);
		assertEquals("jdbc:foo//bar/spam", ds.getUrl());
		assertTrue(ds.getTestWhileIdle());
		assertTrue(ds.getTestOnBorrow());
		assertTrue(ds.getTestOnReturn());
		assertEquals(10000, ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(12345, ds.getMinEvictableIdleTimeMillis());
		assertEquals(1234, ds.getMaxWait());
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		this.context.register(CommonsDataSourceConfiguration.class);
		this.context.refresh();
		BasicDataSource ds = this.context.getBean(BasicDataSource.class);
		assertEquals(GenericObjectPool.DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS,
				ds.getTimeBetweenEvictionRunsMillis());
		assertEquals(GenericObjectPool.DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS,
				ds.getMinEvictableIdleTimeMillis());
		assertEquals(GenericObjectPool.DEFAULT_MAX_WAIT, ds.getMaxWait());
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class CommonsDataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = DataSourceAutoConfiguration.CONFIGURATION_PREFIX)
		public DataSource dataSource() {
			return DataSourceFactory.create().type(BasicDataSource.class).build();
		}

	}

}
