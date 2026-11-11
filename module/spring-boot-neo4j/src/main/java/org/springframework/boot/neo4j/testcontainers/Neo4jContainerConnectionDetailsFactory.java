/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.testcontainers;

import java.net.URI;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.testcontainers.neo4j.Neo4jContainer;

import org.springframework.boot.neo4j.autoconfigure.Neo4jConnectionDetails;
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
		extends ContainerConnectionDetailsFactory<Neo4jContainer, Neo4jConnectionDetails> {

	Neo4jContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "org.neo4j.driver.AuthToken");
	}

	@Override
	protected Neo4jConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Neo4jContainer> source) {
		return new Neo4jContainerConnectionDetails(source);
	}

	/**
	 * {@link Neo4jConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class Neo4jContainerConnectionDetails extends ContainerConnectionDetails<Neo4jContainer>
			implements Neo4jConnectionDetails {

		private Neo4jContainerConnectionDetails(ContainerConnectionSource<Neo4jContainer> source) {
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
