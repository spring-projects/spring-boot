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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link DataSourceProperties}.
 *
 * @author Maciej Walkowiak
 */
public class DataSourcePropertiesTests {

	@Test
	public void correctDriverClassNameFromJdbcUrlWhenDriverClassNameNotDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		String driverClassName = configuration.getDriverClassName();
		assertEquals(driverClassName, "com.mysql.jdbc.Driver");
	}

	@Test
	public void driverClassNameFromDriverClassNamePropertyWhenDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		configuration.setDriverClassName("org.hsqldb.jdbcDriver");
		String driverClassName = configuration.getDriverClassName();
		assertEquals(driverClassName, "org.hsqldb.jdbcDriver");
	}

}
