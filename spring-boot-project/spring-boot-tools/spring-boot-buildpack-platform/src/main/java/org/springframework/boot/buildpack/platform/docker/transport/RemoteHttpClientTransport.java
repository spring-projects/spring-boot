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
import java.util.Collection;
import java.util.Collections;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import org.springframework.boot.buildpack.platform.docker.ssl.SslContextFactory;
import org.springframework.boot.buildpack.platform.system.Environment;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

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

	static RemoteHttpClientTransport createIfPossible(Environment environment) {
		return createIfPossible(environment, Collections.emptyList());
	}

	static RemoteHttpClientTransport createIfPossible(Environment environment,
			Collection<Header> dockerEngineAuthenticationHeaders) {
		return createIfPossible(environment, new SslContextFactory(), dockerEngineAuthenticationHeaders);
	}

	static RemoteHttpClientTransport createIfPossible(Environment environment, SslContextFactory sslContextFactory) {
		return createIfPossible(environment, sslContextFactory, Collections.emptyList());
	}

	static RemoteHttpClientTransport createIfPossible(Environment environment, SslContextFactory sslContextFactory,
			Collection<Header> dockerEngineAuthenticationHeaders) {
		String host = environment.get(DOCKER_HOST);
		if (host == null || isLocalFileReference(host)) {
			return null;
		}
		return create(environment, sslContextFactory, HttpHost.create(host), dockerEngineAuthenticationHeaders);
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

	private static RemoteHttpClientTransport create(Environment environment, SslContextFactory sslContextFactory,
			HttpHost tcpHost, Collection<Header> dockerEngineAuthenticationHeaders) {
		HttpClientBuilder builder = HttpClients.custom();
		boolean secure = isSecure(environment);
		if (secure) {
			builder.setSSLSocketFactory(getSecureConnectionSocketFactory(environment, sslContextFactory));
		}
		if (!CollectionUtils.isEmpty(dockerEngineAuthenticationHeaders)) {
			builder.setDefaultHeaders(dockerEngineAuthenticationHeaders);
		}
		String scheme = secure ? "https" : "http";
		HttpHost httpHost = new HttpHost(tcpHost.getHostName(), tcpHost.getPort(), scheme);
		return new RemoteHttpClientTransport(builder.build(), httpHost);
	}

	private static LayeredConnectionSocketFactory getSecureConnectionSocketFactory(Environment environment,
			SslContextFactory sslContextFactory) {
		String directory = environment.get(DOCKER_CERT_PATH);
		Assert.hasText(directory,
				() -> DOCKER_TLS_VERIFY + " requires trust material location to be specified with " + DOCKER_CERT_PATH);
		SSLContext sslContext = sslContextFactory.forDirectory(directory);
		return new SSLConnectionSocketFactory(sslContext);
	}

	private static boolean isSecure(Environment environment) {
		String secure = environment.get(DOCKER_TLS_VERIFY);
		try {
			return (secure != null) && (Integer.parseInt(secure) == 1);
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

}
