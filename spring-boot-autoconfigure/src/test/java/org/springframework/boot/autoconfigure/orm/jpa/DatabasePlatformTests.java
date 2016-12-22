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

package org.springframework.boot.autoconfigure.orm.jpa;

import org.junit.Test;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.orm.jpa.vendor.Database;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DatabasePlatform}.
 *
 * @author Eddú Meléndez
 */
public class DatabasePlatformTests {

	@Test
	public void databaseDriverLookups() {
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.DB2))
				.isEqualTo(DatabasePlatform.DB2);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.DERBY))
				.isEqualTo(DatabasePlatform.DERBY);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.H2))
				.isEqualTo(DatabasePlatform.H2);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.HSQLDB))
				.isEqualTo(DatabasePlatform.HSQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.INFORMIX))
				.isEqualTo(DatabasePlatform.INFORMIX);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.MYSQL))
				.isEqualTo(DatabasePlatform.MYSQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.ORACLE))
				.isEqualTo(DatabasePlatform.ORACLE);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.POSTGRESQL))
				.isEqualTo(DatabasePlatform.POSTGRESQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.SQLSERVER))
				.isEqualTo(DatabasePlatform.SQL_SERVER);
	}

	@Test
	public void databaseLookups() {
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.DB2)
				.getDatabase())
				.isEqualTo(Database.DB2);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.DERBY)
				.getDatabase())
				.isEqualTo(Database.DERBY);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.H2)
				.getDatabase())
				.isEqualTo(Database.H2);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.HSQLDB)
				.getDatabase())
				.isEqualTo(Database.HSQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.INFORMIX)
				.getDatabase())
				.isEqualTo(Database.INFORMIX);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.MYSQL)
				.getDatabase())
				.isEqualTo(Database.MYSQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.ORACLE)
				.getDatabase())
				.isEqualTo(Database.ORACLE);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.POSTGRESQL)
				.getDatabase())
				.isEqualTo(Database.POSTGRESQL);
		assertThat(DatabasePlatform.fromDatabaseDriver(DatabaseDriver.SQLSERVER)
				.getDatabase())
				.isEqualTo(Database.SQL_SERVER);
	}

}
