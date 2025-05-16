/*
 * Copyright 2012-2025 the original author or authors.
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

import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;

import org.springframework.boot.buildpack.platform.docker.configuration.DockerHost;
import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link HttpClientTransport} that talks to a remote Docker.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class RemoteHttpClientTransport extends HttpClientTransport {

	private static final Timeout SOCKET_TIMEOUT = Timeout.of(30, TimeUnit.MINUTES);

	private RemoteHttpClientTransport(HttpClient client, HttpHost host) {
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
		try {
			return create(dockerHost, sslContextFactory, HttpHost.create(dockerHost.getAddress()));
		}
		catch (URISyntaxException ex) {
			return null;
		}
	}

	private static RemoteHttpClientTransport create(DockerHost host, SslContextFactory sslContextFactory,
			HttpHost tcpHost) {
		SocketConfig socketConfig = SocketConfig.copy(SocketConfig.DEFAULT).setSoTimeout(SOCKET_TIMEOUT).build();
		PoolingHttpClientConnectionManagerBuilder connectionManagerBuilder = PoolingHttpClientConnectionManagerBuilder
			.create()
			.setDefaultSocketConfig(socketConfig);
		if (host.isSecure()) {
			connectionManagerBuilder.setTlsSocketStrategy(getTlsSocketStrategy(host, sslContextFactory));
		}
		HttpClientBuilder builder = HttpClients.custom();
		builder.setConnectionManager(connectionManagerBuilder.build());
		String scheme = host.isSecure() ? "https" : "http";
		HttpHost httpHost = new HttpHost(scheme, tcpHost.getHostName(), tcpHost.getPort());
		return new RemoteHttpClientTransport(builder.build(), httpHost);
	}

	private static TlsSocketStrategy getTlsSocketStrategy(DockerHost host, SslContextFactory sslContextFactory) {
		String directory = host.getCertificatePath();
		Assert.state(StringUtils.hasText(directory),
				"Docker host TLS verification requires trust material location to be specified with certificate path");
		SSLContext sslContext = sslContextFactory.forDirectory(directory);
		return new DefaultClientTlsStrategy(sslContext);
	}

}
