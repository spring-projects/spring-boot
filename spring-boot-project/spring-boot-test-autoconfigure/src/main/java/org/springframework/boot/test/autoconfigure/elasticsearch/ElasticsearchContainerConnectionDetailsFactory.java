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

package org.springframework.boot.test.autoconfigure.elasticsearch;

import java.util.List;

import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.test.autoconfigure.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link ElasticsearchServiceConnection @ElasticsearchServiceConnection}-annotated
 * {@link GenericContainer} fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchContainerConnectionDetailsFactory extends
		ContainerConnectionDetailsFactory<ElasticsearchServiceConnection, ElasticsearchConnectionDetails, GenericContainer<?>> {

	private static final int DEFAULT_PORT = 9200;

	@Override
	protected ElasticsearchConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ElasticsearchServiceConnection, ElasticsearchConnectionDetails, GenericContainer<?>> source) {
		return new ElasticsearchContainerConnectionDetails(source);
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	private static final class ElasticsearchContainerConnectionDetails extends ContainerConnectionDetails
			implements ElasticsearchConnectionDetails {

		private final List<Node> nodes;

		private ElasticsearchContainerConnectionDetails(
				ContainerConnectionSource<ElasticsearchServiceConnection, ElasticsearchConnectionDetails, GenericContainer<?>> source) {
			super(source);
			this.nodes = List.of(new Node(source.getContainer().getHost(),
					source.getContainer().getMappedPort(DEFAULT_PORT), Protocol.HTTP, null, null));
		}

		@Override
		public List<Node> getNodes() {
			return this.nodes;
		}

	}

}
