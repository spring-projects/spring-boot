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

package org.springframework.boot.amqp.testcontainers;

import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.amqp.autoconfigure.RabbitStreamConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link RabbitStreamConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link RabbitMQContainer}.
 *
 * @author Eddú Meléndez
 */
class RabbitStreamContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RabbitMQContainer, RabbitStreamConnectionDetails> {

	RabbitStreamContainerConnectionDetailsFactory() {
		super(ANY_CONNECTION_NAME, "org.springframework.rabbit.stream.producer.RabbitStreamTemplate");
	}

	@Override
	protected boolean sourceAccepts(ContainerConnectionSource<RabbitMQContainer> source, Class<?> requiredContainerType,
			Class<?> requiredConnectionDetailsType) {
		return source.getConnectionDetailsTypes().contains(requiredConnectionDetailsType)
				&& super.sourceAccepts(source, requiredContainerType, requiredConnectionDetailsType);
	}

	@Override
	protected RabbitStreamConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<RabbitMQContainer> source) {
		return new RabbitMqStreamContainerConnectionDetails(source);
	}

	/**
	 * {@link RabbitStreamConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	static final class RabbitMqStreamContainerConnectionDetails extends ContainerConnectionDetails<RabbitMQContainer>
			implements RabbitStreamConnectionDetails {

		private RabbitMqStreamContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
			super(source);
		}

		@Override
		public String getHost() {
			return getContainer().getHost();
		}

		@Override
		public int getPort() {
			return getContainer().getMappedPort(5552);
		}

		@Override
		public String getUsername() {
			return getContainer().getAdminUsername();
		}

		@Override
		public String getPassword() {
			return getContainer().getAdminPassword();
		}

	}

}
