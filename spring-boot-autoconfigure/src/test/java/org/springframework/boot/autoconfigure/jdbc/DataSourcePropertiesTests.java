/*
 * Copyright 2012-2015 the original author or authors.
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.Test;

/**
 * Tests for {@link DataSourceProperties}.
 *
 * @author Maciej Walkowiak
 * @author Arnost Havelka
 */
public class DataSourcePropertiesTests {

	@Test
	public void correctDriverClassNameFromJdbcUrlWhenDriverClassNameNotDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		String driverClassName = configuration.getDriverClassName();
		assertThat(driverClassName, equalTo("com.mysql.jdbc.Driver"));
	}

	@Test
	public void driverClassNameFromDriverClassNamePropertyWhenDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setUrl("jdbc:mysql://mydb");
		configuration.setDriverClassName("org.hsqldb.jdbcDriver");
		String driverClassName = configuration.getDriverClassName();
		assertThat(driverClassName, equalTo("org.hsqldb.jdbcDriver"));
	}

	@Test
	public void isolationLevelNotDefined() {
		DataSourceProperties configuration = new DataSourceProperties();
		assertThat(configuration.getIsolationLevel(), equalTo(-1));
	}

	@Test
	public void isolationLevelDefinedByConst() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setIsolationLevel("4");
		assertThat(configuration.getIsolationLevel(), equalTo(4));
	}

	@Test
	public void isolationLevelDefinedByName() {
		DataSourceProperties configuration = new DataSourceProperties();
		configuration.setIsolationLevel("serializable");
		assertThat(configuration.getIsolationLevel(), equalTo(8));
	}

}
