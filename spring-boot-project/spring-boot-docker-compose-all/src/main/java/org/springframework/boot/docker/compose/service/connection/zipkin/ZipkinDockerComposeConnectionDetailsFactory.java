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

package org.springframework.boot.docker.compose.service.connection.zipkin;

import org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinConnectionDetails;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionDetailsFactory;
import org.springframework.boot.docker.compose.service.connection.DockerComposeConnectionSource;

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
		super("openzipkin/zipkin",
				"org.springframework.boot.actuate.autoconfigure.tracing.zipkin.ZipkinAutoConfiguration");
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
