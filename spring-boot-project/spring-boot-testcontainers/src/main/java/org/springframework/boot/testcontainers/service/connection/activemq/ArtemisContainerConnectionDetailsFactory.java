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

package org.springframework.boot.testcontainers.service.connection.activemq;

import org.testcontainers.activemq.ArtemisContainer;

import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConnectionDetails;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisMode;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ArtemisConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link ArtemisContainer}.
 *
 * @author Eddú Meléndez
 */
class ArtemisContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ArtemisContainer, ArtemisConnectionDetails> {

	@Override
	protected ArtemisConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ArtemisContainer> source) {
		return new ArtemisContainerConnectionDetails(source);
	}

	private static final class ArtemisContainerConnectionDetails extends ContainerConnectionDetails<ArtemisContainer>
			implements ArtemisConnectionDetails {

		private ArtemisContainerConnectionDetails(ContainerConnectionSource<ArtemisContainer> source) {
			super(source);
		}

		@Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		@Override
		public String getBrokerUrl() {
			return getContainer().getBrokerUrl();
		}

		@Override
		public String getUser() {
			return getContainer().getUser();
		}

		@Override
		public String getPassword() {
			return getContainer().getPassword();
		}

	}

}
