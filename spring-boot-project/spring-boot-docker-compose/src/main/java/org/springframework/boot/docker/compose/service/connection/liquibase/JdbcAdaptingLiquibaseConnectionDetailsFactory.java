/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.liquibase;

import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseConnectionDetails;
import org.springframework.boot.autoconfigure.service.connection.ConnectionDetailsFactory;

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
			public String getUsername() {
				return input.getUsername();
			}

			@Override
			public String getPassword() {
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
