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

package org.springframework.boot.testcontainers.service.connection.flyway;

import org.testcontainers.containers.JdbcDatabaseContainer;

import org.springframework.boot.autoconfigure.flyway.FlywayConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link ServiceConnection @ServiceConnection}-annotated {@link JdbcDatabaseContainer}
 * fields that should produce {@link FlywayConnectionDetails}.
 *
 * @author Andy Wilkinson
 */
class FlywayContainerConnectionDetailsFactory extends
		ContainerConnectionDetailsFactory<ServiceConnection, FlywayConnectionDetails, JdbcDatabaseContainer<?>> {

	@Override
	protected FlywayConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ServiceConnection, FlywayConnectionDetails, JdbcDatabaseContainer<?>> source) {
		return new FlywayContainerConnectionDetails(source);
	}

	/**
	 * {@link FlywayConnectionDetails} backed by a {@link JdbcDatabaseContainer}.
	 */
	private static final class FlywayContainerConnectionDetails extends ContainerConnectionDetails
			implements FlywayConnectionDetails {

		private final JdbcDatabaseContainer<?> container;

		private FlywayContainerConnectionDetails(
				ContainerConnectionSource<ServiceConnection, FlywayConnectionDetails, JdbcDatabaseContainer<?>> source) {
			super(source);
			this.container = source.getContainer();
		}

		@Override
		public String getUsername() {
			return this.container.getUsername();
		}

		@Override
		public String getPassword() {
			return this.container.getPassword();
		}

		@Override
		public String getJdbcUrl() {
			return this.container.getJdbcUrl();
		}

		@Override
		public String getDriverClassName() {
			return this.container.getDriverClassName();
		}

	}

}
