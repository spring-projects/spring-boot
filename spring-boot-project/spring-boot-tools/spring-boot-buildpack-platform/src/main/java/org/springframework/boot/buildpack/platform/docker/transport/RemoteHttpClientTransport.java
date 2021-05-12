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

package org.springframework.boot.buildpack.platform.docker.transport;

import java.nio.file.Files;
import java.nio.file.Paths;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.boot.buildpack.platform.system.Environment;
import org.springframework.util.Assert;

/**
 * {@link HttpClientTransport} that talks to a remote Docker.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class RemoteHttpClientTransport extends HttpClientTransport {

	private static final String UNIX_SOCKET_PREFIX = "unix://";

	private static final String DOCKER_HOST = "DOCKER_HOST";

	private static final String DOCKER_TLS_VERIFY = "DOCKER_TLS_VERIFY";

	private static final String DOCKER_CERT_PATH = "DOCKER_CERT_PATH";

	private RemoteHttpClientTransport(CloseableHttpClient client, HttpHost host) {
		super(client, host);
	}

	static RemoteHttpClientTransport createIfPossible(Environment environment, DockerHost dockerHost) {
		return createIfPossible(environment, dockerHost, new SslContextFactory());
	}

	static RemoteHttpClientTransport createIfPossible(Environment environment, DockerHost dockerHost,
			SslContextFactory sslContextFactory) {
		DockerHost host = getHost(environment, dockerHost);
		if (host == null || host.getAddress() == null || isLocalFileReference(host.getAddress())) {
			return null;
		}
		return create(host, sslContextFactory, HttpHost.create(host.getAddress()));
	}

	private static boolean isLocalFileReference(String host) {
		String filePath = host.startsWith(UNIX_SOCKET_PREFIX) ? host.substring(UNIX_SOCKET_PREFIX.length()) : host;
		try {
			return Files.exists(Paths.get(filePath));
		}
		catch (Exception ex) {
			return false;
		}
	}

	private static RemoteHttpClientTransport create(DockerHost host, SslContextFactory sslContextFactory,
			HttpHost tcpHost) {
		HttpClientBuilder builder = HttpClients.custom();
		if (host.isSecure()) {
			builder.setSSLSocketFactory(getSecureConnectionSocketFactory(host, sslContextFactory));
		}
		String scheme = host.isSecure() ? "https" : "http";
		HttpHost httpHost = new HttpHost(tcpHost.getHostName(), tcpHost.getPort(), scheme);
		return new RemoteHttpClientTransport(builder.build(), httpHost);
	}

	private static LayeredConnectionSocketFactory getSecureConnectionSocketFactory(DockerHost host,
			SslContextFactory sslContextFactory) {
		String directory = host.getCertificatePath();
		Assert.hasText(directory,
				() -> "Docker host TLS verification requires trust material location to be specified with certificate path");
		SSLContext sslContext = sslContextFactory.forDirectory(directory);
		return new SSLConnectionSocketFactory(sslContext);
	}

	private static DockerHost getHost(Environment environment, DockerHost dockerHost) {
		if (environment.get(DOCKER_HOST) != null) {
			return new EnvironmentDockerHost(environment);
		}
		return dockerHost;
	}

	private static class EnvironmentDockerHost extends DockerHost {

		EnvironmentDockerHost(Environment environment) {
			super(environment.get(DOCKER_HOST), isTrue(environment.get(DOCKER_TLS_VERIFY)),
					environment.get(DOCKER_CERT_PATH));
		}

		private static boolean isTrue(String value) {
			try {
				return (value != null) && (Integer.parseInt(value) == 1);
			}
			catch (NumberFormatException ex) {
				return false;
			}
		}

	}

}
