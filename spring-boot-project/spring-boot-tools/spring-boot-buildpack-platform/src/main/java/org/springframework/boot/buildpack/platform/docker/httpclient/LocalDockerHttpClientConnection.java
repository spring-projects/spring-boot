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
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.util.Assert;

/**
 * A {@link DockerHttpClientConnection} that describes a connection to a local Docker
 * host.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class LocalDockerHttpClientConnection implements DockerHttpClientConnection {

	private HttpHost httpHost;

	private CloseableHttpClient httpClient;

	/**
	 * Indicate that this factory can be used as a default.
	 * @return {@code true} always
	 */
	public boolean accept() {
		this.httpHost = HttpHost.create("docker://localhost");

		HttpClientBuilder builder = HttpClients.custom();
		builder.setConnectionManager(new LocalDockerHttpClientConnectionManager());
		builder.setSchemePortResolver(new LocalDockerSchemePortResolver());
		this.httpClient = builder.build();

		return true;
	}

	/**
	 * Get an {@link HttpHost} describing a local Docker host connection.
	 * @return the {@code HttpHost}
	 */
	@Override
	public HttpHost getHttpHost() {
		Assert.state(this.httpHost != null, "DockerHttpClientConnection was not properly initialized");
		return this.httpHost;
	}

	/**
	 * Get an {@link HttpClient} that can be used to communicate with a local Docker host.
	 * @return the {@code HttpClient}
	 */
	@Override
	public CloseableHttpClient getHttpClient() {
		Assert.state(this.httpClient != null, "DockerHttpClientConnection was not properly initialized");
		return this.httpClient;
	}

}
