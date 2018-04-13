/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.orm.jpa.vendor.Database;

/**
 * Utility to lookup well known {@link Database Databases} from a {@link DataSource}.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 */
final class DatabaseLookup {

	private static final Log logger = LogFactory.getLog(DatabaseLookup.class);

	private static final Map<DatabaseDriver, Database> LOOKUP;

	static {
		Map<DatabaseDriver, Database> map = new EnumMap<>(DatabaseDriver.class);
		map.put(DatabaseDriver.DERBY, Database.DERBY);
		map.put(DatabaseDriver.H2, Database.H2);
		map.put(DatabaseDriver.HSQLDB, Database.HSQL);
		map.put(DatabaseDriver.MYSQL, Database.MYSQL);
		map.put(DatabaseDriver.ORACLE, Database.ORACLE);
		map.put(DatabaseDriver.POSTGRESQL, Database.POSTGRESQL);
		map.put(DatabaseDriver.SQLSERVER, Database.SQL_SERVER);
		map.put(DatabaseDriver.DB2, Database.DB2);
		map.put(DatabaseDriver.INFORMIX, Database.INFORMIX);
		LOOKUP = Collections.unmodifiableMap(map);
	}

	private DatabaseLookup() {
	}

	/**
	 * Return the most suitable {@link Database} for the given {@link DataSource}.
	 * @param dataSource the source {@link DataSource}
	 * @return the most suitable {@link Database}
	 */
	public static Database getDatabase(DataSource dataSource) {
		if (dataSource == null) {
			return Database.DEFAULT;
		}
		try {
			String url = JdbcUtils.extractDatabaseMetaData(dataSource, "getURL");
			DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
			Database database = LOOKUP.get(driver);
			if (database != null) {
				return database;
			}
		}
		catch (MetaDataAccessException ex) {
			logger.warn("Unable to determine jdbc url from datasource", ex);
		}
		return Database.DEFAULT;
	}

}
