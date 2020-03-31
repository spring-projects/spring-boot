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

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.util.Assert;

/**
 * A {@link DockerHttpClientConnection} that describes a connection to a remote Docker
 * host specified by environment variables.
 *
 * This implementation looks for the following environment variables:
 *
 * <p>
 * <ul>
 * <li>{@code DOCKER_HOST} - the URL to a Docker daemon host, such as
 * {@code tcp://localhost:2376}</li>
 * <li>{@code DOCKER_TLS_VERIFY} - set to {@code 1} to enable secure connection to the
 * Docker host via TLS (optional)</li>
 * <li>{@code DOCKER_CERT_PATH} - the path to certificate and key files needed for TLS
 * verification (required if {@code DOCKER_TLS_VERIFY=1})</li>
 * </ul>
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class RemoteEnvironmentDockerHttpClientConnection implements DockerHttpClientConnection {

	private static final String DOCKER_HOST_KEY = "DOCKER_HOST";

	private static final String DOCKER_TLS_VERIFY_KEY = "DOCKER_TLS_VERIFY";

	private static final String DOCKER_CERT_PATH_KEY = "DOCKER_CERT_PATH";

	private final EnvironmentAccessor environment;

	private final SslContextFactory sslContextFactory;

	private HttpHost httpHost;

	private CloseableHttpClient httpClient;

	RemoteEnvironmentDockerHttpClientConnection() {
		this.environment = new SystemEnvironmentAccessor();
		this.sslContextFactory = new SslContextFactory();
	}

	RemoteEnvironmentDockerHttpClientConnection(EnvironmentAccessor environmentAccessor,
			SslContextFactory sslContextFactory) {
		this.environment = environmentAccessor;
		this.sslContextFactory = sslContextFactory;
	}

	/**
	 * Indicate whether this factory can create be used to create a connection.
	 * @return {@code true} if the environment variable {@code DOCKER_HOST} is set,
	 * {@code false} otherwise
	 */
	public boolean accept() {
		if (this.environment.getProperty("DOCKER_HOST") != null) {
			initHttpHost();
			initHttpClient();
			return true;
		}
		return false;
	}

	/**
	 * Get an {@link HttpHost} from the Docker host specified in the environment.
	 * @return the {@code HttpHost}
	 */
	@Override
	public HttpHost getHttpHost() {
		Assert.state(this.httpHost != null, "DockerHttpClientConnection was not properly initialized");
		return this.httpHost;
	}

	/**
	 * Get an {@link HttpClient} from the Docker connection information specified in the
	 * environment.
	 * @return the {@code HttpClient}
	 */
	@Override
	public CloseableHttpClient getHttpClient() {
		Assert.state(this.httpClient != null, "DockerHttpClientConnection was not properly initialized");
		return this.httpClient;
	}

	private void initHttpHost() {
		String dockerHost = this.environment.getProperty(DOCKER_HOST_KEY);
		Assert.hasText(dockerHost, "DOCKER_HOST must be set");

		this.httpHost = HttpHost.create(dockerHost);
		if ("tcp".equals(this.httpHost.getSchemeName())) {
			String scheme = (isSecure()) ? "https" : "http";
			this.httpHost = new HttpHost(this.httpHost.getHostName(), this.httpHost.getPort(), scheme);
		}
	}

	private void initHttpClient() {
		HttpClientBuilder builder = HttpClients.custom();

		if (isSecure()) {
			String certPath = this.environment.getProperty(DOCKER_CERT_PATH_KEY);
			Assert.hasText(certPath, DOCKER_TLS_VERIFY_KEY + " requires trust material location to be specified with "
					+ DOCKER_CERT_PATH_KEY);

			SSLContext sslContext = this.sslContextFactory.forPath(certPath);
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

			builder.setSSLSocketFactory(sslSocketFactory).setSSLContext(sslContext);
		}

		this.httpClient = builder.build();
	}

	private boolean isSecure() {
		String tlsVerify = this.environment.getProperty(DOCKER_TLS_VERIFY_KEY);
		if (tlsVerify != null) {
			try {
				return Integer.parseInt(tlsVerify) == 1;
			}
			catch (NumberFormatException ex) {
				return false;
			}
		}
		return false;
	}

	interface EnvironmentAccessor {

		String getProperty(String key);

	}

	public static class SystemEnvironmentAccessor implements EnvironmentAccessor {

		public String getProperty(String key) {
			return System.getenv(key);
		}

	}

}
