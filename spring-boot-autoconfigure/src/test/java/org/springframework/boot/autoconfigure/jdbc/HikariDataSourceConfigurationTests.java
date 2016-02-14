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

import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link HikariDataSourceConfiguration}.
 *
 * @author Dave Syer
 */
public class HikariDataSourceConfigurationTests {

	private static final String PREFIX = "spring.datasource.";

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@After
	public void restore() {
		EmbeddedDatabaseConnection.override = null;
	}

	@Test
	public void testDataSourceExists() throws Exception {
		this.context.register(HikariDataSourceConfiguration.class);
		this.context.refresh();
		assertNotNull(this.context.getBean(DataSource.class));
		assertNotNull(this.context.getBean(HikariDataSource.class));
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		this.context.register(HikariDataSourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				PREFIX + "jdbcUrl:jdbc:foo//bar/spam");
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX + "maxLifetime:1234");
		this.context.refresh();
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertEquals("jdbc:foo//bar/spam", ds.getJdbcUrl());
		assertEquals(1234, ds.getMaxLifetime());
		// TODO: test JDBC4 isValid()
	}

	@Test
	public void testDataSourceGenericPropertiesOverridden() throws Exception {
		this.context.register(HikariDataSourceConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, PREFIX
				+ "dataSourceProperties.dataSourceClassName:org.h2.JDBCDataSource");
		this.context.refresh();
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertEquals("org.h2.JDBCDataSource",
				ds.getDataSourceProperties().getProperty("dataSourceClassName"));
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		this.context.register(HikariDataSourceConfiguration.class);
		this.context.refresh();
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertEquals(1800000, ds.getMaxLifetime());
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class HikariDataSourceConfiguration {

		@Bean
		@ConfigurationProperties(prefix = DataSourceProperties.PREFIX)
		public DataSource dataSource() {
			return DataSourceBuilder.create().type(HikariDataSource.class).build();
		}

	}

}
