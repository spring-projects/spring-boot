/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.cassandra.docker.compose;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.cassandra.autoconfigure.CassandraConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.ssl.SslBundle;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link CassandraConnectionDetails} for a {@code Cassandra} service.
 *
 * @author Scott Frederick
 */
class CassandraDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<CassandraConnectionDetails> {

	private static final int CASSANDRA_PORT = 9042;

	CassandraDockerComposeConnectionDetailsFactory() {
		super("cassandra");
	}

	@Override
	protected CassandraConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new CassandraDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link CassandraConnectionDetails} backed by a {@code Cassandra}
	 * {@link RunningService}.
	 */
	static class CassandraDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements CassandraConnectionDetails {

		private final List<Node> contactPoints;

		private final String datacenter;

		private final @Nullable SslBundle sslBundle;

		CassandraDockerComposeConnectionDetails(RunningService service) {
			super(service);
			CassandraEnvironment cassandraEnvironment = new CassandraEnvironment(service.env());
			this.contactPoints = List.of(new Node(service.host(), service.ports().get(CASSANDRA_PORT)));
			this.datacenter = cassandraEnvironment.getDatacenter();
			this.sslBundle = getSslBundle(service);
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return this.sslBundle;
		}

		@Override
		public List<Node> getContactPoints() {
			return this.contactPoints;
		}

		@Override
		public String getLocalDatacenter() {
			return this.datacenter;
		}

	}

}
