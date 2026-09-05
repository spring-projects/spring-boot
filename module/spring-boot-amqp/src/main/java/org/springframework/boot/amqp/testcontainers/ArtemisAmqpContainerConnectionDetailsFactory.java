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

import org.jspecify.annotations.Nullable;
import org.testcontainers.activemq.ArtemisContainer;

import org.springframework.boot.amqp.autoconfigure.AmqpConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link AmqpConnectionDetails} from
 * a {@link ServiceConnection @ServiceConnection}-annotated {@link ArtemisContainer}.
 *
 * @author Stephane Nicoll
 */
class ArtemisAmqpContainerConnectionDetailsFactory
		extends AbstractAmqpContainerConnectionDetailsFactory<ArtemisContainer> {

	@Override
	protected @Nullable AmqpConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ArtemisContainer> source) {
		return new ArtemisAmqpContainerConnectionDetails(source);
	}

	static class ArtemisAmqpContainerConnectionDetails extends AmqpContainerConnectionDetails<ArtemisContainer> {

		ArtemisAmqpContainerConnectionDetails(ContainerConnectionSource<ArtemisContainer> source) {
			super(source);
		}

		@Override
		public @Nullable String getUsername() {
			return getContainer().getUser();
		}

		@Override
		public @Nullable String getPassword() {
			return getContainer().getPassword();
		}

	}

}
