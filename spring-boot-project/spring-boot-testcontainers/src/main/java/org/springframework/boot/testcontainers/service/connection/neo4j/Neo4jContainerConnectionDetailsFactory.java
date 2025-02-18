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

package org.springframework.boot.testcontainers.service.connection.neo4j;

import java.net.URI;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.testcontainers.containers.Neo4jContainer;

import org.springframework.boot.autoconfigure.neo4j.Neo4jConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link Neo4jConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link Neo4jContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class Neo4jContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Neo4jContainer<?>, Neo4jConnectionDetails> {

	Neo4jContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "org.neo4j.driver.AuthToken");
	}

	@Override
	protected Neo4jConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<Neo4jContainer<?>> source) {
		return new Neo4jContainerConnectionDetails(source);
	}

	/**
	 * {@link Neo4jConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class Neo4jContainerConnectionDetails extends ContainerConnectionDetails<Neo4jContainer<?>>
			implements Neo4jConnectionDetails {

		private Neo4jContainerConnectionDetails(ContainerConnectionSource<Neo4jContainer<?>> source) {
			super(source);
		}

		@Override
		public URI getUri() {
			return URI.create(getContainer().getBoltUrl());
		}

		@Override
		public AuthToken getAuthToken() {
			String password = getContainer().getAdminPassword();
			return (password != null) ? AuthTokens.basic("neo4j", password) : AuthTokens.none();
		}

	}

}
