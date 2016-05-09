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

package org.springframework.boot.autoconfigure.jdbc;

import java.lang.reflect.Field;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.DataSourceProxy;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.apache.tomcat.jdbc.pool.interceptor.SlowQueryReport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests for {@link TomcatDataSourceConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class TomcatDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.tomcat.";

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
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "url:jdbc:h2:mem:testdb");
		this.context.refresh();
		assertThat(this.context.getBean(DataSource.class)).isNotNull();
		assertThat(this.context.getBean(org.apache.tomcat.jdbc.pool.DataSource.class))
				.isNotNull();
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(TomcatDataSourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "url:jdbc:h2:mem:testdb");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testWhileIdle:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnBorrow:true");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "testOnReturn:true");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "timeBetweenEvictionRunsMillis:10000");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "minEvictableIdleTimeMillis:12345");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "maxWait:1234");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "jdbcInterceptors:SlowQueryReport");
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "validationInterval:9999");
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context
				.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(ds.getUrl()).isEqualTo("jdbc:h2:mem:testdb");
		assertThat(ds.isTestWhileIdle()).isTrue();
		assertThat(ds.isTestOnBorrow()).isTrue();
		assertThat(ds.isTestOnReturn()).isTrue();
		assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(10000);
		assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(12345);
		assertThat(ds.getMaxWait()).isEqualTo(1234);
		assertThat(ds.getValidationInterval()).isEqualTo(9999L);
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
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "url:jdbc:h2:mem:testdb");
		this.context.refresh();
		org.apache.tomcat.jdbc.pool.DataSource ds = this.context
				.getBean(org.apache.tomcat.jdbc.pool.DataSource.class);
		assertThat(ds.getTimeBetweenEvictionRunsMillis()).isEqualTo(5000);
		assertThat(ds.getMinEvictableIdleTimeMillis()).isEqualTo(60000);
		assertThat(ds.getMaxWait()).isEqualTo(30000);
		assertThat(ds.getValidationInterval()).isEqualTo(30000L);
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}

	@Configuration
	@EnableConfigurationProperties
	@EnableMBeanExport
	protected static class TomcatDataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = "spring.datasource.tomcat")
		public DataSource dataSource() {
			return DataSourceBuilder.create()
					.type(org.apache.tomcat.jdbc.pool.DataSource.class).build();
		}

	}

}
