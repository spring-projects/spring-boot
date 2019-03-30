/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.jdbc;

import org.junit.Test;

import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link DataSourceProperties}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class DataSourcePropertiesTests {

	@Test
	public void determineDriver() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		assertThat(properties.getDriverClassName()).isNull();
		assertThat(properties.determineDriverClassName())
				.isEqualTo("com.mysql.cj.jdbc.Driver");
	}

	@Test
	public void determineDriverWithExplicitConfig() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.setDriverClassName("org.hsqldb.jdbcDriver");
		assertThat(properties.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
		assertThat(properties.determineDriverClassName())
				.isEqualTo("org.hsqldb.jdbcDriver");
	}

	@Test
	public void determineUrl() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.getUrl()).isNull();
		assertThat(properties.determineUrl())
				.isEqualTo(EmbeddedDatabaseConnection.H2.getUrl("testdb"));
	}

	@Test
	public void determineUrlWithNoEmbeddedSupport() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setBeanClassLoader(
				new FilteredClassLoader("org.h2", "org.apache.derby", "org.hsqldb"));
		properties.afterPropertiesSet();
		assertThatExceptionOfType(
				DataSourceProperties.DataSourceBeanCreationException.class)
						.isThrownBy(properties::determineUrl)
						.withMessageContaining("Failed to determine suitable jdbc url");
	}

	@Test
	public void determineUrlWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.afterPropertiesSet();
		assertThat(properties.getUrl()).isEqualTo("jdbc:mysql://mydb");
		assertThat(properties.determineUrl()).isEqualTo("jdbc:mysql://mydb");
	}

	@Test
	public void determineUrlWithGenerateUniqueName() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setGenerateUniqueName(true);
		properties.afterPropertiesSet();
		assertThat(properties.determineUrl()).isEqualTo(properties.determineUrl());

		DataSourceProperties properties2 = new DataSourceProperties();
		properties2.setGenerateUniqueName(true);
		properties2.afterPropertiesSet();
		assertThat(properties.determineUrl()).isNotEqualTo(properties2.determineUrl());
	}

	@Test
	public void determineUsername() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isNull();
		assertThat(properties.determineUsername()).isEqualTo("sa");
	}

	@Test
	public void determineUsernameWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("foo");
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isEqualTo("foo");
		assertThat(properties.determineUsername()).isEqualTo("foo");
	}

	@Test
	public void determinePassword() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isNull();
		assertThat(properties.determinePassword()).isEqualTo("");
	}

	@Test
	public void determinePasswordWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setPassword("bar");
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isEqualTo("bar");
		assertThat(properties.determinePassword()).isEqualTo("bar");
	}

	@Test
	public void determineCredentialsForSchemaScripts() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setSchemaUsername("foo");
		properties.setSchemaPassword("bar");
		assertThat(properties.getSchemaUsername()).isEqualTo("foo");
		assertThat(properties.getSchemaPassword()).isEqualTo("bar");
	}

	@Test
	public void determineCredentialsForDataScripts() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setDataUsername("foo");
		properties.setDataPassword("bar");
		assertThat(properties.getDataUsername()).isEqualTo("foo");
		assertThat(properties.getDataPassword()).isEqualTo("bar");
	}

}
