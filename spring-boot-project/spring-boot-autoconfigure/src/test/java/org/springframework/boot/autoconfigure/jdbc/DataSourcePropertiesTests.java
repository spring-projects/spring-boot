/*
 * Copyright 2012-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

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
 * @author Scott Frederick
 */
class DataSourcePropertiesTests {

	@Test
	void determineDriver() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		assertThat(properties.getDriverClassName()).isNull();
		assertThat(properties.determineDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
	}

	@Test
	void determineDriverWithExplicitConfig() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.setDriverClassName("org.hsqldb.jdbcDriver");
		assertThat(properties.getDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
		assertThat(properties.determineDriverClassName()).isEqualTo("org.hsqldb.jdbcDriver");
	}

	@Test
	void determineUrlWithoutGenerateUniqueName() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setGenerateUniqueName(false);
		properties.afterPropertiesSet();
		assertThat(properties.getUrl()).isNull();
		assertThat(properties.determineUrl()).isEqualTo(EmbeddedDatabaseConnection.H2.getUrl("testdb"));
	}

	@Test
	void determineUrlWithNoEmbeddedSupport() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setBeanClassLoader(new FilteredClassLoader("org.h2", "org.apache.derby", "org.hsqldb"));
		properties.afterPropertiesSet();
		assertThatExceptionOfType(DataSourceProperties.DataSourceBeanCreationException.class)
			.isThrownBy(properties::determineUrl)
			.withMessageContaining("Failed to determine suitable jdbc url");
	}

	@Test
	void determineUrlWithSpecificEmbeddedConnection() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setGenerateUniqueName(false);
		properties.setEmbeddedDatabaseConnection(EmbeddedDatabaseConnection.HSQLDB);
		properties.afterPropertiesSet();
		assertThat(properties.determineUrl()).isEqualTo(EmbeddedDatabaseConnection.HSQLDB.getUrl("testdb"));
	}

	@Test
	void whenEmbeddedConnectionIsNoneAndNoUrlIsConfiguredThenDetermineUrlThrows() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setGenerateUniqueName(false);
		properties.setEmbeddedDatabaseConnection(EmbeddedDatabaseConnection.NONE);
		assertThatExceptionOfType(DataSourceProperties.DataSourceBeanCreationException.class)
			.isThrownBy(properties::determineUrl)
			.withMessageContaining("Failed to determine suitable jdbc url");
	}

	@Test
	void determineUrlWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.afterPropertiesSet();
		assertThat(properties.getUrl()).isEqualTo("jdbc:mysql://mydb");
		assertThat(properties.determineUrl()).isEqualTo("jdbc:mysql://mydb");
	}

	@Test
	void determineUrlWithGenerateUniqueName() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.determineUrl()).isEqualTo(properties.determineUrl());

		DataSourceProperties properties2 = new DataSourceProperties();
		properties2.setGenerateUniqueName(true);
		properties2.afterPropertiesSet();
		assertThat(properties.determineUrl()).isNotEqualTo(properties2.determineUrl());
	}

	@Test
	void determineUsername() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isNull();
		assertThat(properties.determineUsername()).isEqualTo("sa");
	}

	@Test
	void determineUsernameWhenEmpty() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("");
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isEmpty();
		assertThat(properties.determineUsername()).isEqualTo("sa");
	}

	@Test
	void determineUsernameWhenNull() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername(null);
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isNull();
		assertThat(properties.determineUsername()).isEqualTo("sa");
	}

	@Test
	void determineUsernameWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("foo");
		properties.afterPropertiesSet();
		assertThat(properties.getUsername()).isEqualTo("foo");
		assertThat(properties.determineUsername()).isEqualTo("foo");
	}

	@Test
	void determineUsernameWithNonEmbeddedUrl() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:h2:~/test");
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isNull();
		assertThat(properties.determineUsername()).isNull();
	}

	@Test
	void determinePassword() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isNull();
		assertThat(properties.determinePassword()).isEmpty();
	}

	@Test
	void determinePasswordWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setPassword("bar");
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isEqualTo("bar");
		assertThat(properties.determinePassword()).isEqualTo("bar");
	}

	@Test
	void determinePasswordWithNonEmbeddedUrl() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:h2:~/test");
		properties.afterPropertiesSet();
		assertThat(properties.getPassword()).isNull();
		assertThat(properties.determinePassword()).isNull();
	}

}
