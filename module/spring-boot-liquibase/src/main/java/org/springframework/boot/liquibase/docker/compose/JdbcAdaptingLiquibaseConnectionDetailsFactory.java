/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.liquibase.docker.compose;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;
import org.springframework.boot.jdbc.autoconfigure.JdbcConnectionDetails;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseConnectionDetails;

/**
 * {@link ConnectionDetailsFactory} that produces {@link LiquibaseConnectionDetails} by
 * adapting {@link JdbcConnectionDetails}.
 *
 * @author Andy Wilkinson
 */
class JdbcAdaptingLiquibaseConnectionDetailsFactory
		implements ConnectionDetailsFactory<JdbcConnectionDetails, LiquibaseConnectionDetails> {

	@Override
	public LiquibaseConnectionDetails getConnectionDetails(JdbcConnectionDetails input) {
		return new LiquibaseConnectionDetails() {

			@Override
			public @Nullable String getUsername() {
				return input.getUsername();
			}

			@Override
			public @Nullable String getPassword() {
				return input.getPassword();
			}

			@Override
			public String getJdbcUrl() {
				return input.getJdbcUrl();
			}

			@Override
			public String getDriverClassName() {
				return input.getDriverClassName();
			}

		};
	}

}
