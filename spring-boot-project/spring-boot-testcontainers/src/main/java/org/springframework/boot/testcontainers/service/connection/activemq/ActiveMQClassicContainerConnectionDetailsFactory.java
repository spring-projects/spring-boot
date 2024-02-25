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

import org.testcontainers.activemq.ActiveMQContainer;

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ActiveMQConnectionDetails} *
 * from a {@link ServiceConnection @ServiceConnection}-annotated
 * {@link ActiveMQContainer}.
 *
 * @author Eddú Meléndez
 */
class ActiveMQClassicContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ActiveMQContainer, ActiveMQConnectionDetails> {

	/**
     * Returns the ActiveMQConnectionDetails for the given ContainerConnectionSource.
     * 
     * @param source the ContainerConnectionSource for which to retrieve the connection details
     * @return the ActiveMQConnectionDetails for the given ContainerConnectionSource
     */
    @Override
	protected ActiveMQConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ActiveMQContainer> source) {
		return new ActiveMQContainerConnectionDetails(source);
	}

	/**
     * ActiveMQContainerConnectionDetails class.
     */
    private static final class ActiveMQContainerConnectionDetails extends ContainerConnectionDetails<ActiveMQContainer>
			implements ActiveMQConnectionDetails {

		/**
         * Constructs a new ActiveMQContainerConnectionDetails object with the specified ContainerConnectionSource.
         * 
         * @param source the ContainerConnectionSource used to create the ActiveMQContainerConnectionDetails object
         */
        private ActiveMQContainerConnectionDetails(ContainerConnectionSource<ActiveMQContainer> source) {
			super(source);
		}

		/**
         * Returns the broker URL of the ActiveMQ container connection details.
         * 
         * @return the broker URL of the ActiveMQ container connection details
         */
        @Override
		public String getBrokerUrl() {
			return getContainer().getBrokerUrl();
		}

		/**
         * Returns the user associated with this ActiveMQContainerConnectionDetails.
         * 
         * @return the user associated with this ActiveMQContainerConnectionDetails
         */
        @Override
		public String getUser() {
			return getContainer().getUser();
		}

		/**
         * Returns the password associated with the ActiveMQ container connection details.
         * 
         * @return the password associated with the ActiveMQ container connection details
         */
        @Override
		public String getPassword() {
			return getContainer().getPassword();
		}

	}

}
