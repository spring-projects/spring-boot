/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.httpclient;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;

/**
 * A {@code DockerHttpClientConnection} that determines an appropriate connection to a
 * Docker host by detecting whether a remote Docker host is configured or if a default
 * local connection should be used.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public final class DelegatingDockerHttpClientConnection implements DockerHttpClientConnection {

	private static final RemoteEnvironmentDockerHttpClientConnection REMOTE_FACTORY = new RemoteEnvironmentDockerHttpClientConnection();

	private static final LocalDockerHttpClientConnection LOCAL_FACTORY = new LocalDockerHttpClientConnection();

	private final DockerHttpClientConnection delegate;

	private DelegatingDockerHttpClientConnection(DockerHttpClientConnection delegate) {
		this.delegate = delegate;
	}

	/**
	 * Get an {@link HttpHost} describing the Docker host connection.
	 * @return the {@code HttpHost}
	 */
	public HttpHost getHttpHost() {
		return this.delegate.getHttpHost();
	}

	/**
	 * Get an {@link HttpClient} that can be used to communicate with the Docker host.
	 * @return the {@code HttpClient}
	 */
	public CloseableHttpClient getHttpClient() {
		return this.delegate.getHttpClient();
	}

	/**
	 * Create a {@link DockerHttpClientConnection} by detecting the connection
	 * configuration.
	 * @return the {@code DockerHttpClientConnection}
	 */
	public static DockerHttpClientConnection create() {
		if (REMOTE_FACTORY.accept()) {
			return new DelegatingDockerHttpClientConnection(REMOTE_FACTORY);
		}
		if (LOCAL_FACTORY.accept()) {
			return new DelegatingDockerHttpClientConnection(LOCAL_FACTORY);
		}
		return null;
	}

}
