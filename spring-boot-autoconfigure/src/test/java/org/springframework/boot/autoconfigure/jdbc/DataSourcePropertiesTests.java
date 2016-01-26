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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for {@link DataSourceProperties}.
 *
 * @author Maciej Walkowiak
 * @author Stephane Nicoll
 */
public class DataSourcePropertiesTests {

	@Test
	public void determineDriver() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		assertNull(properties.getDriverClassName());
		assertEquals("com.mysql.jdbc.Driver", properties.determineDriverClassName());
	}

	@Test
	public void determineDriverWithExplicitConfig() {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.setDriverClassName("org.hsqldb.jdbcDriver");
		assertEquals("org.hsqldb.jdbcDriver", properties.getDriverClassName());
		assertEquals("org.hsqldb.jdbcDriver", properties.determineDriverClassName());
	}

	@Test
	public void determineUrl() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertNull(properties.getUrl());
		assertEquals(EmbeddedDatabaseConnection.H2.getUrl(), properties.determineUrl());
	}

	@Test
	public void determineUrlWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl("jdbc:mysql://mydb");
		properties.afterPropertiesSet();
		assertEquals("jdbc:mysql://mydb", properties.getUrl());
		assertEquals("jdbc:mysql://mydb", properties.determineUrl());
	}

	@Test
	public void determineUsername() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertNull(properties.getUsername());
		assertEquals("sa", properties.determineUsername());
	}

	@Test
	public void determineUsernameWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUsername("foo");
		properties.afterPropertiesSet();
		assertEquals("foo", properties.getUsername());
		assertEquals("foo", properties.determineUsername());
	}

	@Test
	public void determinePassword() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.afterPropertiesSet();
		assertNull(properties.getPassword());
		assertEquals("", properties.determinePassword());
	}

	@Test
	public void determinePasswordWithExplicitConfig() throws Exception {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setPassword("bar");
		properties.afterPropertiesSet();
		assertEquals("bar", properties.getPassword());
		assertEquals("bar", properties.determinePassword());
	}

}
