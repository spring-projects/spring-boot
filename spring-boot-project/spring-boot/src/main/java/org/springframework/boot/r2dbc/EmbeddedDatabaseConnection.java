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

package org.springframework.boot.r2dbc;

import java.util.function.Predicate;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Connection details for embedded databases compatible with r2dbc.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 * @since 2.5.0
 */
public enum EmbeddedDatabaseConnection {

	/**
	 * No Connection.
	 */
	NONE(null, null, (options) -> false),

	/**
	 * H2 Database Connection.
	 */
	H2("io.r2dbc.h2.H2ConnectionFactoryProvider", "r2dbc:h2:mem:///%s?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
			(options) -> options.getValue(ConnectionFactoryOptions.DRIVER).equals("h2")
					&& options.getValue(ConnectionFactoryOptions.PROTOCOL).equals("mem"));

	private final String driverClassName;

	private final String url;

	private Predicate<ConnectionFactoryOptions> embedded;

	EmbeddedDatabaseConnection(String driverClassName, String url, Predicate<ConnectionFactoryOptions> embedded) {
		this.driverClassName = driverClassName;
		this.url = url;
		this.embedded = embedded;
	}

	/**
	 * Returns the driver class name.
	 * @return the driver class name
	 */
	public String getDriverClassName() {
		return this.driverClassName;
	}

	/**
	 * Returns the R2DBC URL for the connection using the specified {@code databaseName}.
	 * @param databaseName the name of the database
	 * @return the connection URL
	 */
	public String getUrl(String databaseName) {
		Assert.hasText(databaseName, "DatabaseName must not be empty");
		return (this.url != null) ? String.format(this.url, databaseName) : null;
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
	 * Convenience method to determine if a given connection factory represents an
	 * embedded database type.
	 * @param connectionFactory the connection factory to interrogate
	 * @return true if the connection factory represents an embedded database
	 * @since 2.5.1
	 */
	public static boolean isEmbedded(ConnectionFactory connectionFactory) {
		OptionsCapableConnectionFactory optionsCapable = OptionsCapableConnectionFactory.unwrapFrom(connectionFactory);
		if (optionsCapable == null) {
			throw new IllegalArgumentException(
					"Cannot determine database's type as ConnectionFactory is not options-capable. To be "
							+ "options-capable, a ConnectionFactory should be created with "
							+ ConnectionFactoryBuilder.class.getName());
		}
		ConnectionFactoryOptions options = optionsCapable.getOptions();
		for (EmbeddedDatabaseConnection candidate : values()) {
			if (candidate.embedded.test(options)) {
				return true;
			}
		}
		return false;

	}

}
