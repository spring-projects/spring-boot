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

package org.springframework.boot.testcontainers.service.connection.elasticsearch;

import java.util.List;

import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchConnectionDetails.Node.Protocol;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactory;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.util.StringUtils;

/**
 * {@link ContainerConnectionDetailsFactory} to create
 * {@link ElasticsearchConnectionDetails} from a
 * {@link ServiceConnection @ServiceConnection}-annotated {@link ElasticsearchContainer}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Piotr Przybyl
 */
class ElasticsearchContainerConnectionDetailsFactory
		extends ContainerConnectionDetailsFactory<ElasticsearchContainer, ElasticsearchConnectionDetails> {

	private static final int DEFAULT_PORT = 9200;

	private static final String DEFAULT_USERNAME = "elastic";

	private static final String DEFAULT_PASSWORD = "changeme";

	private static final String ELASTIC_PASSWORD_ENV_NAME = "ELASTIC_PASSWORD";

	@Override
	protected ElasticsearchConnectionDetails getContainerConnectionDetails(
			ContainerConnectionSource<ElasticsearchContainer> source) {
		return new ElasticsearchContainerConnectionDetails(source);
	}

	/**
	 * {@link ElasticsearchConnectionDetails} backed by a
	 * {@link ContainerConnectionSource}.
	 */
	private static final class ElasticsearchContainerConnectionDetails
			extends ContainerConnectionDetails<ElasticsearchContainer> implements ElasticsearchConnectionDetails {

		private Boolean securityEnabled;

		private Boolean sslEnabled;

		private ElasticsearchContainerConnectionDetails(ContainerConnectionSource<ElasticsearchContainer> source) {
			super(source);
		}

		@Override
		public List<Node> getNodes() {
			String host = getContainer().getHost();
			Integer port = getContainer().getMappedPort(DEFAULT_PORT);
			return List.of(new Node(host, port, isSslEnabled() ? Protocol.HTTPS : Protocol.HTTP, getUsername(), getPassword()));
		}

		@Override
		public String getUsername() {
			if (isSecurityEnabled()) {
				return DEFAULT_USERNAME;
			}
			return ElasticsearchConnectionDetails.super.getUsername();
		}

		@Override
		public String getPassword() {
			if (isSecurityEnabled()) {
				String envPassword = getContainer().getEnvMap().get(ELASTIC_PASSWORD_ENV_NAME);
				if (StringUtils.hasText(envPassword)) {
					return envPassword;
				}
				return DEFAULT_PASSWORD;
			}
			return ElasticsearchConnectionDetails.super.getPassword();
		}

		private boolean isSslEnabled() {
			// this is basic memoization; no synchronization needed as the results don't change over time
			if (this.sslEnabled != null) {
				return this.sslEnabled;
			}
			ExecResult execResult;
			try {
				execResult = getContainer().execInContainer("/usr/bin/curl", "-k", "https://localhost:" + DEFAULT_PORT);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return switch (execResult.getExitCode()) {
				case 0 -> {
					this.sslEnabled = Boolean.TRUE;
					yield true;
				}
				case 35 -> {
					this.sslEnabled = Boolean.FALSE;
					yield false;
				}
				case 7 ->
						throw new IllegalStateException("Elasticsearch isn't listening on port " + DEFAULT_PORT);
				default ->
						throw new IllegalStateException("Unexpected exit code [" + execResult.getExitCode() + "]");
			};
		}

		private boolean isSecurityEnabled() {
			// this is basic memoization; no synchronization needed as the results don't change over time
			if (this.securityEnabled != null) {
				return this.securityEnabled;
			}
			ExecResult execResult;
			try {
				// this call will print the HTTP status code: if security is enabled, it gives 401
				execResult = getContainer().execInContainer(
						"/usr/bin/curl",
						"-s", "-o", "/dev/null/",
						"-I", "-w", "%{http_code}",
						"-k",
						(isSslEnabled() ? "https" : "http") + "://localhost:" + DEFAULT_PORT);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			return switch (execResult.getStdout()) {
				case "200" -> {
					this.securityEnabled = Boolean.FALSE;
					yield false;
				}
				case "401" -> {
					this.securityEnabled = Boolean.TRUE;
					yield true;
				}
				default ->
						throw new IllegalStateException("Cannot determine if security is enabled for Elasticsearch");
			};
		}
	}
}
