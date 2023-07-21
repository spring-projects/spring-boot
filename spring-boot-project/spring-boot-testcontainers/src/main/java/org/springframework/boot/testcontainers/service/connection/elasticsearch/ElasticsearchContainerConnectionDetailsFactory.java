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

package org.springframework.boot.testcontainers.service.connection.elasticsearch;

import java.util.List;

import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link ElasticsearchConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link ElasticsearchContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ElasticsearchContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ElasticsearchContainer, ElasticsearchConnectionDetails> {

	private static final int DEFAULT_PORT = 9200;

	@Override
	protected ElasticsearchConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ElasticsearchContainer> source) {
		return new ElasticsearchContainerConnectionDetails(source);
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	private static final class ElasticsearchContainerConnectionDetails
			extends ContainerConnectionDetails<ElasticsearchContainer> implements ElasticsearchConnectionDetails {

		private ElasticsearchContainerConnectionDetails(ContainerConnectionSource<ElasticsearchContainer> source) {
			super(source);
		}

		@Override
		public List<Node> getNodes() {
			String host = getContainer().getHost();
			Integer port = getContainer().getMappedPort(DEFAULT_PORT);
			return List.of(new Node(host, port, Protocol.HTTP, null, null));
		}

	}

}
