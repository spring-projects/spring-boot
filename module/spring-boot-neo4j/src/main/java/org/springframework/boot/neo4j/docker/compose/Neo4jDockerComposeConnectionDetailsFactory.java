/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.docker.compose;

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.neo4j.autoconfigure.Neo4jConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link Neo4jConnectionDetails}
 * for a {@code Neo4j} service.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
class Neo4jDockerComposeConnectionDetailsFactory extends DockerComposeConnectionDetailsFactory<Neo4jConnectionDetails> {

	Neo4jDockerComposeConnectionDetailsFactory() {
		super("neo4j");
	}

	@Override
	protected Neo4jConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new Neo4jDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link Neo4jConnectionDetails} backed by a {@code Neo4j} {@link RunningService}.
	 */
	static class Neo4jDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements Neo4jConnectionDetails {

		private static final int BOLT_PORT = 7687;

		private final @Nullable AuthToken authToken;

		private final URI uri;

		Neo4jDockerComposeConnectionDetails(RunningService service) {
			super(service);
			Neo4jEnvironment neo4jEnvironment = new Neo4jEnvironment(service.env());
			this.authToken = neo4jEnvironment.getAuthToken();
			this.uri = URI.create("neo4j://%s:%d".formatted(service.host(), service.ports().get(BOLT_PORT)));
		}

		@Override
		public URI getUri() {
			return this.uri;
		}

		@Override
		public AuthToken getAuthToken() {
			return (this.authToken != null) ? this.authToken : AuthTokens.none();
		}

	}

}
