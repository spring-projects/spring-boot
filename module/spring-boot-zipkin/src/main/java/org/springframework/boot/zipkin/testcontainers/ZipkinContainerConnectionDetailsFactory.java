/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.testcontainers;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.zipkin.autoconfigure.ZipkinConnectionDetails;

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
		super("openzipkin/zipkin", "org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration");
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
