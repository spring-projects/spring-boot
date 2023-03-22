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

package org.springframework.boot.test.autoconfigure.cassandra;

import java.util.List;

import org.testcontainers.containers.CassandraContainer;

import org.springframework.boot.autoconfigure.cassandra.CassandraConnectionDetails;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link CassandraServiceConnection @CassandraServiceConnection}-annotated
 * {@link CassandraContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class CassandraContainerConnectionDetailsFactory extends
		ContainerConnectionDetailsFactory<CassandraServiceConnection, CassandraConnectionDetails, CassandraContainer<?>> {

	@Override
	protected CassandraConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<CassandraServiceConnection, CassandraConnectionDetails, CassandraContainer<?>> source) {
		return new CassandraContainerConnectionDetails(source);
	}

	/**
	 * {@link CassandraConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class CassandraContainerConnectionDetails extends ContainerConnectionDetails
			implements CassandraConnectionDetails {

		private final CassandraContainer<?> container;

		private CassandraContainerConnectionDetails(
				ContainerConnectionSource<CassandraServiceConnection, CassandraConnectionDetails, CassandraContainer<?>> source) {
			super(source);
			this.container = source.getContainer();
		}

		@Override
		public List<Node> getContactPoints() {
			return List.of(new Node(this.container.getContactPoint().getHostString(),
					this.container.getContactPoint().getPort()));
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
		public String getLocalDatacenter() {
			return this.container.getLocalDatacenter();
		}

	}

}
