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

package org.springframework.boot.testcontainers.service.connection.activemq;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ActiveMQConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer}
 * using the {@code "symptoma/activemq"} image.
 *
 * @author Eddú Meléndez
 */
class ActiveMQContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, ActiveMQConnectionDetails> {

	/**
     * Constructs a new ActiveMQContainerConnectionDetailsFactory with the specified connection details.
     * 
     * @param connectionDetails the connection details for the ActiveMQ container
     */
    ActiveMQContainerConnectionDetailsFactory() {
		super("symptoma/activemq");
	}

	/**
     * Returns the ActiveMQConnectionDetails for the specified ContainerConnectionSource.
     * 
     * @param source the ContainerConnectionSource to get the connection details for
     * @return the ActiveMQConnectionDetails for the specified ContainerConnectionSource
     */
    @Override
	protected ActiveMQConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new ActiveMQContainerConnectionDetails(source);
	}

	/**
     * ActiveMQContainerConnectionDetails class.
     */
    private static final class ActiveMQContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements ActiveMQConnectionDetails {

		/**
         * Constructs a new ActiveMQContainerConnectionDetails object with the specified ContainerConnectionSource.
         *
         * @param source the ContainerConnectionSource used to create the ActiveMQContainerConnectionDetails
         */
        private ActiveMQContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		/**
         * Returns the broker URL for the ActiveMQ container connection.
         * The broker URL is constructed by concatenating the TCP protocol, the host of the container, and the first mapped port of the container.
         * 
         * @return the broker URL in the format "tcp://<host>:<port>"
         */
        @Override
		public String getBrokerUrl() {
			return "tcp://" + getContainer().getHost() + ":" + getContainer().getFirstMappedPort();
		}

		/**
         * Returns the username of the user associated with the ActiveMQ container connection.
         * 
         * @return the username of the user
         */
        @Override
		public String getUser() {
			return getContainer().getEnvMap().get("ACTIVEMQ_USERNAME");
		}

		/**
         * Returns the password for the ActiveMQ connection.
         * 
         * @return the password for the ActiveMQ connection
         */
        @Override
		public String getPassword() {
			return getContainer().getEnvMap().get("ACTIVEMQ_PASSWORD");
		}

	}

}
