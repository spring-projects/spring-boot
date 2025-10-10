/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.elasticsearch.docker.compose;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchConnectionDetails.Node.Protocol;

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

	private static final int ELASTICSEARCH_PORT = 9200;

	protected ElasticsearchDockerComposeConnectionDetailsFactory() {
		super("elasticsearch");
	}

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

		ElasticsearchDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ElasticsearchEnvironment(service.env());
			this.nodes = List.of(new Node(service.host(), service.ports().get(ELASTICSEARCH_PORT), Protocol.HTTP,
					getUsername(), getPassword()));
		}

		@Override
		public String getUsername() {
			return "elastic";
		}

		@Override
		public @Nullable String getPassword() {
			return this.environment.getPassword();
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

}
