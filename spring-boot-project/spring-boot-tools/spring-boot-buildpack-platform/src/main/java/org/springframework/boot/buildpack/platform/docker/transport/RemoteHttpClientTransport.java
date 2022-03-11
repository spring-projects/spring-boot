/*
 * Copyright 2012-2022 the original author or authors.
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

import javax.net.ssl.SSLContext;

import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.util.Assert;

/**
 * {@link HttpClientTransport} that talks to a remote Docker.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class RemoteHttpClientTransport extends HttpClientTransport {

	private RemoteHttpClientTransport(CloseableHttpClient client, HttpHost host) {
		super(client, host);
	}

	static RemoteHttpClientTransport createIfPossible(ResolvedDockerHost dockerHost) {
		return createIfPossible(dockerHost, new SslContextFactory());
	}

	static RemoteHttpClientTransport createIfPossible(ResolvedDockerHost dockerHost,
			SslContextFactory sslContextFactory) {
		if (!dockerHost.isRemote()) {
			return null;
		}
		return create(dockerHost, sslContextFactory, HttpHost.create(dockerHost.getAddress()));
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

}
