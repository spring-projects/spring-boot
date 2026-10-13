/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.docker.compose;

import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;
import org.springframework.boot.zipkin.autoconfigure.ZipkinConnectionDetails;

/**
 * {@link DockerComposeConnectionDetailsFactory} to create {@link ZipkinConnectionDetails}
 * for a {@code zipkin} service.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class ZipkinDockerComposeConnectionDetailsFactory
		extends DockerComposeConnectionDetailsFactory<ZipkinConnectionDetails> {

	private static final int ZIPKIN_PORT = 9411;

	ZipkinDockerComposeConnectionDetailsFactory() {
		super("openzipkin/zipkin", "org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration");
	}

	@Override
	protected ZipkinConnectionDetails getDockerComposeConnectionDetails(DockerComposeConnectionSource source) {
		return new ZipkinDockerComposeConnectionDetails(source.getRunningService());
	}

	/**
	 * {@link ZipkinConnectionDetails} backed by a {@code zipkin} {@link RunningService}.
	 */
	static class ZipkinDockerComposeConnectionDetails extends DockerComposeConnectionDetails
			implements ZipkinConnectionDetails {

		private final String host;

		private final int port;

		ZipkinDockerComposeConnectionDetails(RunningService source) {
			super(source);
			this.host = source.host();
			this.port = source.ports().get(ZIPKIN_PORT);
		}

		@Override
		public String getSpanEndpoint() {
			return "http://" + this.host + ":" + this.port + "/api/v2/spans";
		}

	}

}
