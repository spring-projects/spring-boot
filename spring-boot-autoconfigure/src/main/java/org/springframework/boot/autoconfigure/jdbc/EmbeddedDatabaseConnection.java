/*
 * Copyright 2012-2013 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.util.ClassUtils;

/**
 * Connection details for {@link EmbeddedDatabaseType embedded databases}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
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
			"jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"),

	/**
	 * Derby Database Connection.
	 */
	DERBY(EmbeddedDatabaseType.DERBY, "org.apache.derby.jdbc.EmbeddedDriver",
			"jdbc:derby:memory:testdb;create=true"),

	/**
	 * HSQL Database Connection.
	 */
	HSQL(EmbeddedDatabaseType.HSQL, "org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:testdb");

	private final EmbeddedDatabaseType type;

	private final String driverClass;

	private final String url;

	private EmbeddedDatabaseConnection(EmbeddedDatabaseType type, String driverClass,
			String url) {
		this.type = type;
		this.driverClass = driverClass;
		this.url = url;
	}

	/**
	 * Returns the driver class name.
	 */
	public String getDriverClassName() {
		return this.driverClass;
	}

	/**
	 * Returns the {@link EmbeddedDatabaseType} for the connection.
	 */
	public EmbeddedDatabaseType getType() {
		return this.type;
	}

	/**
	 * Returns the URL for the connection.
	 */
	public String getUrl() {
		return this.url;
	}

	/**
	 * Override for testing.
	 */
	static EmbeddedDatabaseConnection override;

	/**
	 * Convenience method to determine if a given driver class name represents an embedded
	 * database type.
	 * 
	 * @param driverClass the driver class
	 * @return true if the driver class is one of the embedded types
	 */
	public static boolean isEmbedded(String driverClass) {
		return driverClass != null
				&& (driverClass.equals(HSQL.driverClass)
						|| driverClass.equals(H2.driverClass) || driverClass
							.equals(DERBY.driverClass));
	}

	/**
	 * Convenience method to determine if a given data source represents an embedded
	 * database type.
	 * 
	 * @param dataSource the data source to interrogate
	 * @return true if the data sourceis one of the embedded types
	 */
	public static boolean isEmbedded(DataSource dataSource) {
		boolean embedded = false;
		try {
			embedded = new JdbcTemplate(dataSource)
					.execute(new ConnectionCallback<Boolean>() {
						@Override
						public Boolean doInConnection(Connection con)
								throws SQLException, DataAccessException {
							String productName = con.getMetaData()
									.getDatabaseProductName();
							if (productName == null) {
								return false;
							}
							productName = productName.toUpperCase();
							if (productName.contains(H2.name())) {
								return true;
							}
							if (productName.contains(HSQL.name())) {
								return true;
							}
							if (productName.contains(DERBY.name())) {
								return true;
							}
							return false;
						}
					});
		}
		catch (DataAccessException e) {
			// Could not connect, which means it's not embedded
		}
		return embedded;
	}

	/**
	 * Returns the most suitable {@link EmbeddedDatabaseConnection} for the given class
	 * loader.
	 * @param classLoader the class loader used to check for classes
	 * @return an {@link EmbeddedDatabaseConnection} or {@link #NONE}.
	 */
	public static EmbeddedDatabaseConnection get(ClassLoader classLoader) {
		if (override != null) {
			return override;
		}
		for (EmbeddedDatabaseConnection candidate : EmbeddedDatabaseConnection.values()) {
			if (candidate != NONE
					&& ClassUtils.isPresent(candidate.getDriverClassName(), classLoader)) {
				return candidate;
			}
		}
		return NONE;
	}

}
