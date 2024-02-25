/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.elasticsearch;

import java.util.List;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ElasticsearchConnectionDetails} for an {@code elasticsearch} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class ElasticsearchDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ElasticsearchConnectionDetails> {

	private static final String[] ELASTICSEARCH_CONTAINER_NAMES = { "elasticsearch", "bitnami/elasticsearch" };

	private static final int ELASTICSEARCH_PORT = 9200;

	/**
	 * Constructs a new ElasticsearchDockerComposeConnectionDetailsFactory.
	 * @param elasticsearchContainerNames the names of the Elasticsearch containers
	 */
	protected ElasticsearchDockerComposeConnectionDetailsFactory() {
		super(ELASTICSEARCH_CONTAINER_NAMES);
	}

	/**
	 * Returns the Elasticsearch connection details for a Docker Compose connection.
	 * @param source the Docker Compose connection source
	 * @return the Elasticsearch connection details
	 */
	@Override
	protected ElasticsearchConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ElasticsearchDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by an {@code elasticsearch}
	 * {@link RunningService}.
	 */
	static class ElasticsearchDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ElasticsearchConnectionDetails {

		private final ElasticsearchEnvironment environment;

		private final List<Node> nodes;

		/**
		 * Constructs a new ElasticsearchDockerComposeConnectionDetails object with the
		 * provided RunningService.
		 * @param service the RunningService object representing the Elasticsearch service
		 */
		ElasticsearchDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ElasticsearchEnvironment(service.env());
			this.nodes = List.of(new Node(service.host(), service.ports().get(ELASTICSEARCH_PORT), Protocol.HTTP,
					getUsername(), getPassword()));
		}

		/**
		 * Returns the username for the Elasticsearch Docker Compose connection details.
		 * @return the username for the Elasticsearch Docker Compose connection details
		 */
		@Override
		public String getUsername() {
			return "elastic";
		}

		/**
		 * Returns the password for the Elasticsearch Docker Compose connection.
		 * @return the password for the Elasticsearch Docker Compose connection
		 */
		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

		/**
		 * Returns the list of nodes in the Elasticsearch Docker Compose connection
		 * details.
		 * @return the list of nodes
		 */
		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

}
