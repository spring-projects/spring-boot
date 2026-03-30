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

import org.testcontainers.containers.Container;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * Base {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated container that is
 * compliant with AMQP 1.0.
 *
 * @param <C> the container type
 * @author Stephane Nicoll
 */
abstract class AbstractAmqpContainerConnectionDetailsFactory<C extends Container<?>>
		extends ContainerConnectionDetailsFactory<C, AmqpConnectionDetails> {

	protected static class AmqpContainerConnectionDetails<C extends Container<?>> extends ContainerConnectionDetails<C>
			implements AmqpConnectionDetails {

		private static final int AMQP_PORT = 5672;

		protected AmqpContainerConnectionDetails(ContainerConnectionSource<C> source) {
			super(source);
		}

		@Override
		public Address getAddress() {
			return new Address(getContainer().getHost(), getContainer().getMappedPort(AMQP_PORT));
		}

	}

}
