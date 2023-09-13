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

package org.springframework.boot.testcontainers.service.connection.amqp;

import java.net.URI;
import java.util.List;

import org.testcontainers.containers.RabbitMQContainer;

import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link RabbitConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link RabbitMQContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RabbitContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RabbitMQContainer, RabbitConnectionDetails> {

	@Override
	protected RabbitConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<RabbitMQContainer> source) {
		return new RabbitMqContainerConnectionDetails(source);
	}

	/**
	 * {@link RabbitConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static final class RabbitMqContainerConnectionDetails extends ContainerConnectionDetails<RabbitMQContainer>
			implements RabbitConnectionDetails {

		private RabbitMqContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
			super(source);
		}

		@Override
		public String getUsername() {
			return getContainer().getAdminUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getAdminPassword();
		}

		@Override
		public List<Address> getAddresses() {
			URI uri = URI.create(getContainer().getAmqpUrl());
			return List.of(new Address(uri.getHost(), uri.getPort()));
		}

	}

}
