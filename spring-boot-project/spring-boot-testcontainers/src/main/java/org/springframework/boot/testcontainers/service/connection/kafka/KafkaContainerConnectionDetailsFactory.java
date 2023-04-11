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

package org.springframework.boot.testcontainers.service.connection.kafka;

import java.net.URI;
import java.util.List;

import org.testcontainers.containers.KafkaContainer;

import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;

/**
 * {@link ContainerConnectionDetailsFactory} for
 * {@link KafkaServiceConnection @KafkaServiceConnection}-annotated {@link KafkaContainer}
 * fields.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class KafkaContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<KafkaServiceConnection, KafkaConnectionDetails, KafkaContainer> {

	@Override
	protected KafkaConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<KafkaServiceConnection, KafkaConnectionDetails, KafkaContainer> source) {
		return new KafkaContainerConnectionDetails(source);
	}

	/**
	 * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class KafkaContainerConnectionDetails extends ContainerConnectionDetails
			implements KafkaConnectionDetails {

		private final KafkaContainer container;

		private KafkaContainerConnectionDetails(
				ContainerConnectionSource<KafkaServiceConnection, KafkaConnectionDetails, KafkaContainer> source) {
			super(source);
			this.container = source.getContainer();
		}

		@Override
		public List<Node> getBootstrapNodes() {
			URI uri = URI.create(this.container.getBootstrapServers());
			return List.of(new Node(uri.getHost(), uri.getPort()));
		}

	}

}
