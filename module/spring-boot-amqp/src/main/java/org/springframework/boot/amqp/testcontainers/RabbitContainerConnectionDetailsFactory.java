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

import java.net.URI;

import org.jspecify.annotations.Nullable;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link RabbitMQContainer}.
 *
 * @author Eddú Meléndez
 */
class RabbitContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<RabbitMQContainer, AmqpConnectionDetails> {

	@Override
	protected AmqpConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
		return new AmqpMqContainerConnectionDetails(source);
	}

	/**
	 * {@link AmqpConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	static final class AmqpMqContainerConnectionDetails extends ContainerConnectionDetails<RabbitMQContainer>
			implements AmqpConnectionDetails {

		private AmqpMqContainerConnectionDetails(ContainerConnectionSource<RabbitMQContainer> source) {
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
		public Address getAddress() {
			URI uri = URI.create((getSslBundle() != null) ? getContainer().getAmqpsUrl() : getContainer().getAmqpUrl());
			return new Address(uri.getHost(), uri.getPort());
		}

		@Override
		public @Nullable SslBundle getSslBundle() {
			return super.getSslBundle();
		}

	}

}
