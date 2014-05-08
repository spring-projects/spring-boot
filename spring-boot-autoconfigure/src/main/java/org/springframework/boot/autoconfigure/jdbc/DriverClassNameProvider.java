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

	private static final Map<String, String> driverMap = new HashMap<String, String>() {
		{
			put("db2", "com.ibm.db2.jcc.DB2Driver");
			put("derby", "org.apache.derby.jdbc.EmbeddedDriver");
			put("h2", "org.h2.Driver");
			put("hsqldb", "org.hsqldb.jdbcDriver");
			put("sqlite", "org.sqlite.JDBC");
			put("mysql", "com.mysql.jdbc.Driver");
			put("mariadb", "org.mariadb.jdbc.Driver");
			put("google", "com.google.appengine.api.rdbms.AppEngineDriver");
			put("oracle", "oracle.jdbc.OracleDriver");
			put("postgresql", "org.postgresql.Driver");
			put("jtds", "net.sourceforge.jtds.jdbc.Driver");
			put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");

		}
	};

	/**
	 * Used to find JDBC driver class name based on given JDBC URL
	 *
	 * @param jdbcUrl JDBC URL
	 * @return driver class name or null if not found
	 */
	String getDriverClassName(final String jdbcUrl) {
		Assert.notNull(jdbcUrl, "JDBC URL cannot be null");

		if (!jdbcUrl.startsWith(JDBC_URL_PREFIX)) {
			throw new IllegalArgumentException("JDBC URL should start with '"
					+ JDBC_URL_PREFIX + "'");
		}

		String urlWithoutPrefix = jdbcUrl.substring(JDBC_URL_PREFIX.length());
		String result = null;

		for (Map.Entry<String, String> driver : driverMap.entrySet()) {
			if (urlWithoutPrefix.startsWith(":" + driver.getKey() + ":")) {
				result = driver.getValue();

				break;
			}
		}

		return result;
	}

}
