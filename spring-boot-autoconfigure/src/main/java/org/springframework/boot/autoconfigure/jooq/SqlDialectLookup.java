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

package org.springframework.boot.autoconfigure.jooq;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.SQLDialect;

import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Utility to lookup well known {@link SQLDialect SQLDialects} from a {@link DataSource}.
 *
 * @author Michael Simons
 */
final class SqlDialectLookup {

	private static final Log logger = LogFactory.getLog(SqlDialectLookup.class);

	private static final Map<DatabaseDriver, SQLDialect> LOOKUP;

	static {
		Map<DatabaseDriver, SQLDialect> map = new HashMap<>();
		map.put(DatabaseDriver.DERBY, SQLDialect.DERBY);
		map.put(DatabaseDriver.H2, SQLDialect.H2);
		map.put(DatabaseDriver.HSQLDB, SQLDialect.HSQLDB);
		map.put(DatabaseDriver.MARIADB, SQLDialect.MARIADB);
		map.put(DatabaseDriver.MYSQL, SQLDialect.MYSQL);
		map.put(DatabaseDriver.POSTGRESQL, SQLDialect.POSTGRES);
		map.put(DatabaseDriver.SQLITE, SQLDialect.SQLITE);
		LOOKUP = Collections.unmodifiableMap(map);
	}

	private SqlDialectLookup() {
	}

	/**
	 * Return the most suitable {@link SQLDialect} for the given {@link DataSource}.
	 * @param dataSource the source {@link DataSource}
	 * @return the most suitable {@link SQLDialect}
	 */
	public static SQLDialect getDialect(DataSource dataSource) {
		if (dataSource == null) {
			return SQLDialect.DEFAULT;
		}
		try {
			String url = JdbcUtils.extractDatabaseMetaData(dataSource, "getURL");
			DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
			SQLDialect sqlDialect = LOOKUP.get(driver);
			if (sqlDialect != null) {
				return sqlDialect;
			}
		}
		catch (MetaDataAccessException ex) {
			logger.warn("Unable to determine jdbc url from datasource", ex);
		}
		return SQLDialect.DEFAULT;
	}

}
