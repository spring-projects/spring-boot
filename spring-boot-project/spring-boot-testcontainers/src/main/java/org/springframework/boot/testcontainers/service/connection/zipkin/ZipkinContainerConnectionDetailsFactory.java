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

package org.springframework.boot.testcontainers.service.connection.zipkin;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

/**
 * {@link ContainerConnectionDetailsFactory} to create {@link ZipkinConnectionDetails}
 * from a {@link ServiceConnection @ServiceConnection}-annotated {@link GenericContainer}
 * using the {@code "openzipkin/zipkin"} image.
 *
 * @author Eddú Meléndez
 * @author Moritz Halbritter
 */
class ZipkinContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<Container<?>, ZipkinConnectionDetails> {

	private static final int ZIPKIN_PORT = 9411;

	/**
	 * Constructs a new ZipkinContainerConnectionDetailsFactory.
	 * @param groupId the Maven group ID of the Zipkin container
	 * @param artifactId the Maven artifact ID of the Zipkin container
	 * @param autoConfigurationClass the fully qualified class name of the
	 * auto-configuration class for Zipkin
	 */
	ZipkinContainerConnectionDetailsFactory() {
		super("openzipkin/zipkin",
				"org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration");
	}

	/**
	 * Returns the ZipkinConnectionDetails for the given ContainerConnectionSource.
	 * @param source the ContainerConnectionSource to get the connection details from
	 * @return the ZipkinConnectionDetails for the given ContainerConnectionSource
	 */
	@Override
	protected ZipkinConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new ZipkinContainerConnectionDetails(source);
	}

	/**
	 * {@link ZipkinConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static class ZipkinContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements ZipkinConnectionDetails {

		/**
		 * Constructs a new ZipkinContainerConnectionDetails with the specified
		 * ContainerConnectionSource.
		 * @param source the ContainerConnectionSource used to create the connection
		 * details
		 */
		ZipkinContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		/**
		 * Returns the endpoint URL for sending spans to the Zipkin server. The URL is
		 * constructed using the host and mapped port of the container.
		 * @return the endpoint URL for sending spans
		 */
		@Override
		public String getSpanEndpoint() {
			return "http://" + getContainer().getHost() + ":" + getContainer().getMappedPort(ZIPKIN_PORT)
					+ "/api/v2/spans";
		}

	}

}
