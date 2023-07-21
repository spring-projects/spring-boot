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

	ZipkinContainerConnectionDetailsFactory() {
		super("openzipkin/zipkin",
				"org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration");
	}

	@Override
	protected ZipkinConnectionDetails getContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
		return new ZipkinContainerConnectionDetails(source);
	}

	/**
	 * {@link ZipkinConnectionDetails} backed by a {@link ContainerConnectionSource}.
	 */
	private static class ZipkinContainerConnectionDetails extends ContainerConnectionDetails<Container<?>>
			implements ZipkinConnectionDetails {

		ZipkinContainerConnectionDetails(ContainerConnectionSource<Container<?>> source) {
			super(source);
		}

		@Override
		public String getSpanEndpoint() {
			return "http://" + getContainer().getHost() + ":" + getContainer().getMappedPort(ZIPKIN_PORT)
					+ "/api/v2/spans";
		}

	}

}
