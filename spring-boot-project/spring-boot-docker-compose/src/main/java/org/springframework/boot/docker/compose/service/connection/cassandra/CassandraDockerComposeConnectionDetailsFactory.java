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

package org.springframework.boot.docker.compose.service.connection.cassandra;

import java.util.List;

import org.springframework.boot.autoconfigure.cassandra.CassandraConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

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

		CassandraDockerComposeConnectionDetails(RunningService service) {
			super(service);
			CassandraEnvironment cassandraEnvironment = new CassandraEnvironment(service.env());
			this.contactPoints = List.of(new Node(service.host(), service.ports().get(CASSANDRA_PORT)));
			this.datacenter = cassandraEnvironment.getDatacenter();
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
