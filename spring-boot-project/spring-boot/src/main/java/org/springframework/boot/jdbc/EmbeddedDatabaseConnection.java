/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

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
 * @author Nidhi Desai
 * @since 1.0.0
 * @see #get(ClassLoader)
 */
public enum EmbeddedDatabaseConnection {

	/**
	 * No Connection.
	 */
	NONE(null, null, null, (url) -> false),

	/**
	 * H2 Database Connection.
	 */
	H2(EmbeddedDatabaseType.H2, DatabaseDriver.H2.getDriverClassName(),
			"jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE", (url) -> url.contains(":h2:mem")),

	/**
	 * Derby Database Connection.
	 */
	DERBY(EmbeddedDatabaseType.DERBY, DatabaseDriver.DERBY.getDriverClassName(), "jdbc:derby:memory:%s;create=true",
			(url) -> true),

	/**
	 * HSQL Database Connection.
	 * @deprecated since 2.4.0 for removal in 2.6.0 in favor of
	 * {@link EmbeddedDatabaseConnection#HSQLDB}.
	 */
	@Deprecated
	HSQL(EmbeddedDatabaseType.HSQL, DatabaseDriver.HSQLDB.getDriverClassName(), "org.hsqldb.jdbcDriver",
			"jdbc:hsqldb:mem:%s", (url) -> url.contains(":hsqldb:mem:")),

	/**
	 * HSQL Database Connection.
	 * @since 2.4.0
	 */
	HSQLDB(EmbeddedDatabaseType.HSQL, DatabaseDriver.HSQLDB.getDriverClassName(), "org.hsqldb.jdbcDriver",
			"jdbc:hsqldb:mem:%s", (url) -> url.contains(":hsqldb:mem:"));

	private final EmbeddedDatabaseType type;

	private final String driverClass;

	private final String alternativeDriverClass;

	private final String url;

	private final Predicate<String> embeddedUrl;

	EmbeddedDatabaseConnection(EmbeddedDatabaseType type, String driverClass, String url,
			Predicate<String> embeddedUrl) {
		this(type, driverClass, null, url, embeddedUrl);
	}

	EmbeddedDatabaseConnection(EmbeddedDatabaseType type, String driverClass, String fallbackDriverClass, String url,
			Predicate<String> embeddedUrl) {
		this.type = type;
		this.driverClass = driverClass;
		this.alternativeDriverClass = fallbackDriverClass;
		this.url = url;
		this.embeddedUrl = embeddedUrl;
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
	 * Returns the URL for the connection using the specified {@code databaseName}.
	 * @param databaseName the name of the database
	 * @return the connection URL
	 */
	public String getUrl(String databaseName) {
		Assert.hasText(databaseName, "DatabaseName must not be empty");
		return (this.url != null) ? String.format(this.url, databaseName) : null;
	}

	boolean isEmbeddedUrl(String url) {
		return this.embeddedUrl.test(url);
	}

	boolean isDriverCompatible(String driverClass) {
		return (driverClass != null
				&& (driverClass.equals(this.driverClass) || driverClass.equals(this.alternativeDriverClass)));
	}

	/**
	 * Convenience method to determine if a given driver class name represents an embedded
	 * database type.
	 * @param driverClass the driver class
	 * @return true if the driver class is one of the embedded types
	 * @deprecated since 2.4.0 for removal in 2.6.0 in favor of
	 * {@link #isEmbedded(String, String)}
	 */
	@Deprecated
	public static boolean isEmbedded(String driverClass) {
		return isEmbedded(driverClass, null);
	}

	/**
	 * Convenience method to determine if a given driver class name and url represent an
	 * embedded database type.
	 * @param driverClass the driver class
	 * @param url the jdbc url (can be {@code null})
	 * @return true if the driver class and url refer to an embedded database
	 * @since 2.4.0
	 */
	public static boolean isEmbedded(String driverClass, String url) {
		if (driverClass == null) {
			return false;
		}
		EmbeddedDatabaseConnection connection = getEmbeddedDatabaseConnection(driverClass);
		if (connection == NONE) {
			return false;
		}
		return (url == null || connection.isEmbeddedUrl(url));
	}

	private static EmbeddedDatabaseConnection getEmbeddedDatabaseConnection(String driverClass) {
		return Stream.of(H2, HSQLDB, DERBY).filter((connection) -> connection.isDriverCompatible(driverClass))
				.findFirst().orElse(NONE);
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
			if (candidate != NONE && ClassUtils.isPresent(candidate.getDriverClassName(), classLoader)) {
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
		public Boolean doInConnection(Connection connection) throws SQLException, DataAccessException {
			DatabaseMetaData metaData = connection.getMetaData();
			String productName = metaData.getDatabaseProductName();
			if (productName == null) {
				return false;
			}
			productName = productName.toUpperCase(Locale.ENGLISH);
			EmbeddedDatabaseConnection[] candidates = EmbeddedDatabaseConnection.values();
			for (EmbeddedDatabaseConnection candidate : candidates) {
				if (candidate != NONE && productName.contains(candidate.name())) {
					String url = metaData.getURL();
					return (url == null || candidate.isEmbeddedUrl(url));
				}
			}
			return false;
		}

	}

}
