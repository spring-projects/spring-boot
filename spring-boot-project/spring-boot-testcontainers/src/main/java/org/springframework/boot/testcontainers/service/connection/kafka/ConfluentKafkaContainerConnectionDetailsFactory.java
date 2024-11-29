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

package org.springframework.boot.testcontainers.service.connection.kafka;

import java.util.List;

import org.testcontainers.kafka.ConfluentKafkaContainer;

import org.springframework.boot.autoconfigure.kafka.KafkaConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link KafkaConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link ConfluentKafkaContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ConfluentKafkaContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ConfluentKafkaContainer, KafkaConnectionDetails> {

	@Override
	protected KafkaConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ConfluentKafkaContainer> source) {
		return new ConfluentKafkaContainerConnectionDetails(source);
	}

	/**
	 * {@link KafkaConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class ConfluentKafkaContainerConnectionDetails
			extends ContainerConnectionDetails<ConfluentKafkaContainer> implements KafkaConnectionDetails {

		private ConfluentKafkaContainerConnectionDetails(ContainerConnectionSource<ConfluentKafkaContainer> source) {
			super(source);
		}

		@Override
		public List<String> getBootstrapServers() {
			return List.of(getContainer().getBootstrapServers());
		}

	}

}
