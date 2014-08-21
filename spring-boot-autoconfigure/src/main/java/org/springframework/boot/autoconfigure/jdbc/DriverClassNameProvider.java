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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Provides JDBC driver class name for given JDBC URL.
 *
 * @author Maciej Walkowiak
 * @since 1.1.0
 */
class DriverClassNameProvider {

	private static final String JDBC_URL_PREFIX = "jdbc";

	private static final Map<String, String> DRIVERS;
	static {
		Map<String, String> drivers = new HashMap<String, String>();
		drivers.put("derby", "org.apache.derby.jdbc.EmbeddedDriver");
		drivers.put("h2", "org.h2.Driver");
		drivers.put("hsqldb", "org.hsqldb.jdbc.JDBCDriver");
		drivers.put("sqlite", "org.sqlite.JDBC");
		drivers.put("mysql", "com.mysql.jdbc.Driver");
		drivers.put("mariadb", "org.mariadb.jdbc.Driver");
		drivers.put("google", "com.google.appengine.api.rdbms.AppEngineDriver");
		drivers.put("oracle", "oracle.jdbc.OracleDriver");
		drivers.put("postgresql", "org.postgresql.Driver");
		drivers.put("jtds", "net.sourceforge.jtds.jdbc.Driver");
		drivers.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
		DRIVERS = Collections.unmodifiableMap(drivers);
	}

	/**
	 * Find a JDBC driver class name based on given JDBC URL
	 * @param jdbcUrl JDBC URL
	 * @return driver class name or null if not found
	 */
	String getDriverClassName(final String jdbcUrl) {
		Assert.notNull(jdbcUrl, "JdbcUrl must not be null");
		Assert.isTrue(jdbcUrl.startsWith(JDBC_URL_PREFIX), "JdbcUrl must start with '"
				+ JDBC_URL_PREFIX + "'");
		String urlWithoutPrefix = jdbcUrl.substring(JDBC_URL_PREFIX.length());
		for (Map.Entry<String, String> driver : DRIVERS.entrySet()) {
			if (urlWithoutPrefix.startsWith(":" + driver.getKey() + ":")) {
				return driver.getValue();
			}
		}
		return null;
	}

}
