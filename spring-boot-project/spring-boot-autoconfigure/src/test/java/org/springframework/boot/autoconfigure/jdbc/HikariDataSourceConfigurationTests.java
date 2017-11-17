/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataSourceAutoConfiguration} with Hikari.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
public class HikariDataSourceConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testDataSourceExists() throws Exception {
		load();
		assertThat(this.context.getBeansOfType(DataSource.class)).hasSize(1);
		assertThat(this.context.getBeansOfType(HikariDataSource.class)).hasSize(1);
	}

	@Test
	public void testDataSourcePropertiesOverridden() throws Exception {
		load("spring.datasource.hikari.jdbc-url=jdbc:foo//bar/spam",
				"spring.datasource.hikari.max-lifetime=1234");
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertThat(ds.getJdbcUrl()).isEqualTo("jdbc:foo//bar/spam");
		assertThat(ds.getMaxLifetime()).isEqualTo(1234);
		// TODO: test JDBC4 isValid()
	}

	@Test
	public void testDataSourceGenericPropertiesOverridden() throws Exception {
		load("spring.datasource.hikari.data-source-properties.dataSourceClassName=org.h2.JDBCDataSource");
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertThat(ds.getDataSourceProperties().getProperty("dataSourceClassName"))
				.isEqualTo("org.h2.JDBCDataSource");
	}

	@Test
	public void testDataSourceDefaultsPreserved() throws Exception {
		load();
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertThat(ds.getMaxLifetime()).isEqualTo(1800000);
	}

	@Test
	public void nameIsAliasedToPoolName() {
		load("spring.datasource.name=myDS");
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertThat(ds.getPoolName()).isEqualTo("myDS");
	}

	@Test
	public void poolNameTakesPrecedenceOverName() {
		load("spring.datasource.name=myDS",
				"spring.datasource.hikari.pool-name=myHikariDS");
		HikariDataSource ds = this.context.getBean(HikariDataSource.class);
		assertThat(ds.getPoolName()).isEqualTo("myHikariDS");
	}

	@SuppressWarnings("unchecked")
	public static <T> T getField(Class<?> target, String name) {
		Field field = ReflectionUtils.findField(target, name, null);
		ReflectionUtils.makeAccessible(field);
		return (T) ReflectionUtils.getField(field, target);
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment)
				.and("spring.datasource.initialization-mode=never")
				.and("spring.datasource.type=" + HikariDataSource.class.getName())
				.applyTo(ctx);
		ctx.register(DataSourceAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

}
