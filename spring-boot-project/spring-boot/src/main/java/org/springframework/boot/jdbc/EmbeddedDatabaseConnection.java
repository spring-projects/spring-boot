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

package org.springframework.boot.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Connection details for {@link EmbeddedDatabaseType embedded databases}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Stephane Nicoll
 * @see #get(ClassLoader)
 */
public enum EmbeddedDatabaseConnection {

	/**
	 * No Connection.
	 */
	NONE(null, null, null),

	/**
	 * H2 Database Connection.
	 */
	H2(EmbeddedDatabaseType.H2, "org.h2.Driver",
			"jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"),

	/**
	 * Derby Database Connection.
	 */
	DERBY(EmbeddedDatabaseType.DERBY, "org.apache.derby.jdbc.EmbeddedDriver",
			"jdbc:derby:memory:%s;create=true"),

	/**
	 * HSQL Database Connection.
	 */
	HSQL(EmbeddedDatabaseType.HSQL, "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:%s");

	private static final String DEFAULT_DATABASE_NAME = "testdb";

	private final EmbeddedDatabaseType type;

	private final String driverClass;

	private final String url;

	EmbeddedDatabaseConnection(EmbeddedDatabaseType type, String driverClass,
			String url) {
		this.type = type;
		this.driverClass = driverClass;
		this.url = url;
	}

	/**
	 * Returns the driver class name.
	 * @return the driver class name
	 */
	public String getDriverClassName() {
		return this.driverClass;
	}

	/**
	 * Returns the {@link EmbeddedDatabaseType} for the connection.
	 * @return the database type
	 */
	public EmbeddedDatabaseType getType() {
		return this.type;
	}

	/**
	 * Returns the URL for the connection using the default database name.
	 * @return the connection URL
	 */
	public String getUrl() {
		return getUrl(DEFAULT_DATABASE_NAME);
	}

	/**
	 * Returns the URL for the connection using the specified {@code databaseName}.
	 * @param databaseName the name of the database
	 * @return the connection URL
	 */
	public String getUrl(String databaseName) {
		Assert.hasText(databaseName, "DatabaseName must not be null.");
		return this.url != null ? String.format(this.url, databaseName) : null;
	}

	/**
	 * Convenience method to determine if a given driver class name represents an embedded
	 * database type.
	 * @param driverClass the driver class
	 * @return true if the driver class is one of the embedded types
	 */
	public static boolean isEmbedded(String driverClass) {
		return driverClass != null && (driverClass.equals(HSQL.driverClass)
				|| driverClass.equals(H2.driverClass)
				|| driverClass.equals(DERBY.driverClass));
	}

	/**
	 * Convenience method to determine if a given data source represents an embedded
	 * database type.
	 * @param dataSource the data source to interrogate
	 * @return true if the data source is one of the embedded types
	 */
	public static boolean isEmbedded(DataSource dataSource) {
		try {
			return new JdbcTemplate(dataSource).execute(new IsEmbedded());
		}
		catch (DataAccessException ex) {
			// Could not connect, which means it's not embedded
			return false;
		}
	}

	/**
	 * Returns the most suitable {@link EmbeddedDatabaseConnection} for the given class
	 * loader.
	 * @param classLoader the class loader used to check for classes
	 * @return an {@link EmbeddedDatabaseConnection} or {@link #NONE}.
	 */
	public static EmbeddedDatabaseConnection get(ClassLoader classLoader) {
		for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
			if (candidate != NONE && ClassUtils.isPresent(candidate.getDriverClassName(),
					classLoader)) {
				return candidate;
			}
		}
		return NONE;
	}

	/**
	 * {@link ConnectionCallback} to determine if a connection is embedded.
	 */
	private static class IsEmbedded implements ConnectionCallback<Boolean> {

		@Override
		public Boolean doInConnection(Connection connection)
				throws SQLException, DataAccessException {
			String productName = connection.getMetaData().getDatabaseProductName();
			if (productName == null) {
				return false;
			}
			productName = productName.toUpperCase();
			EmbeddedDatabaseConnection[] candidates = EmbeddedDatabaseConnection.values();
			for (EmbeddedDatabaseConnection candidate : candidates) {
				if (candidate != NONE && productName.contains(candidate.name())) {
					return true;
				}
			}
			return false;
		}

	}

}
