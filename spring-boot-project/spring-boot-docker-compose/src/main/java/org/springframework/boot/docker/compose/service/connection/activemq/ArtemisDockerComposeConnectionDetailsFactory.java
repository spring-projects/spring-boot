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

package org.springframework.boot.docker.compose.service.connection.activemq;

import org.springframework.boot.autoconfigure.jms.artemis.ArtemisConnectionDetails;
import org.springframework.boot.autoconfigure.jms.artemis.ArtemisMode;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create
 * {@link ArtemisConnectionDetails} for an {@code artemis} service.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ArtemisDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ArtemisConnectionDetails> {

	private static final int ACTIVEMQ_PORT = 61616;

	protected ArtemisDockerComposeConnectionDetailsFactory() {
		super("apache/activemq-artemis");
	}

	@Override
	protected ArtemisConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ArtemisDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ArtemisConnectionDetails} backed by a {@code artemis}
	 * {@link RunningService}.
	 */
	static class ArtemisDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ArtemisConnectionDetails {

		private final ArtemisEnvironment environment;

		private final String brokerUrl;

		protected ArtemisDockerComposeConnectionDetails(RunningService service) {
			super(service);
			this.environment = new ArtemisEnvironment(service.env());
			this.brokerUrl = "tcp://" + service.host() + ":" + service.ports().get(ACTIVEMQ_PORT);
		}

		@Override
		public ArtemisMode getMode() {
			return ArtemisMode.NATIVE;
		}

		@Override
		public String getBrokerUrl() {
			return this.brokerUrl;
		}

		@Override
		public String getUser() {
			return this.environment.getUser();
		}

		@Override
		public String getPassword() {
			return this.environment.getPassword();
		}

	}

}
