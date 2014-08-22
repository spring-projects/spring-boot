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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Enumeration of common database drivers.
 *
 * @author Phillip Webb
 * @author Maciej Walkowiak
 * @since 1.2.0
 */
enum DatabaseDriver {

	/**
	 * Unknown type.
	 */
	UNKNOWN(null),

	/**
	 * Apache Derby.
	 */
	DERBY("org.apache.derby.jdbc.EmbeddedDriver"),

	/**
	 * H2.
	 */
	H2("org.h2.Driver", "org.h2.jdbcx.JdbcDataSource"),

	/**
	 * HyperSQL DataBase.
	 */
	HSQLDB("org.hsqldb.jdbc.JDBCDriver", "org.hsqldb.jdbc.pool.JDBCXADataSource"),

	/**
	 * SQL Lite.
	 */
	SQLITE("org.sqlite.JDBC"),

	/**
	 * MySQL.
	 */
	MYSQL("com.mysql.jdbc.Driver", "org.mysql.jdbc.MySQLDataSource"),

	/**
	 * Maria DB.
	 */
	MARIADB("org.mariadb.jdbc.Driver", "org.mariadb.jdbc.MySQLDataSource"),

	/**
	 * Google App Engine.
	 */
	GOOGLE("com.google.appengine.api.rdbms.AppEngineDriver"),

	/**
	 * Oracle
	 */
	ORACLE("oracle.jdbc.OracleDriver", "oracle.jdbc.xa.OracleXADataSource"),

	/**
	 * Postres
	 */
	POSTGRESQL("org.postgresql.Driver", "org.postgresql.xa.PGXADataSource"),

	/**
	 * JTDS
	 */
	JTDS("net.sourceforge.jtds.jdbc.Driver"),

	/**
	 * SQL Server
	 */
	SQLSERVER("com.microsoft.sqlserver.jdbc.SQLServerDriver");

	private final String driverClassName;

	private final String xaDataSourceClassName;

	private DatabaseDriver(String driverClassName) {
		this(driverClassName, null);
	}

	private DatabaseDriver(String driverClassName, String xaDataSourceClassName) {
		this.driverClassName = driverClassName;
		this.xaDataSourceClassName = xaDataSourceClassName;
	}

	/**
	 * @return the driverClassName or {@code null}
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
	 * @return the xaDataSourceClassName or {@code null}
	 */
	public String getXaDataSourceClassName() {
		return this.xaDataSourceClassName;
	}

	/**
	 * Find a {@link DatabaseDriver} for the given URL.
	 * @param url JDBC URL
	 * @return driver class name or {@link #UNKNOWN} if not found
	 */
	public static DatabaseDriver fromJdbcUrl(String url) {
		if (StringUtils.hasLength(url)) {
			Assert.isTrue(url.startsWith("jdbc"), "URL must start with 'jdbc'");
			String urlWithoutPrefix = url.substring("jdbc".length()).toLowerCase();
			for (DatabaseDriver driver : values()) {
				String prefix = ":" + driver.name().toLowerCase() + ":";
				if (driver != UNKNOWN && urlWithoutPrefix.startsWith(prefix)) {
					return driver;
				}
			}
		}
		return UNKNOWN;
	}

}
