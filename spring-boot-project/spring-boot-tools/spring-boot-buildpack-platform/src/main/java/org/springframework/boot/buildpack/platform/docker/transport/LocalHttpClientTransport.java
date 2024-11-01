/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.Socket;

import com.sun.jna.Platform;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.DefaultHttpClientConnectionOperator;
import org.apache.hc.client5.http.io.DetachedSocketFactory;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.socket.NamedPipeSocket;
import org.springframework.boot.buildpack.platform.socket.UnixDomainSocket;

/**
 * {@link HttpClientTransport} that talks to local Docker.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
final class LocalHttpClientTransport extends HttpClientTransport {

	private static final String DOCKER_SCHEME = "docker";

	private static final int DEFAULT_DOCKER_PORT = 2376;

	private static final HttpHost LOCAL_DOCKER_HOST = new HttpHost(DOCKER_SCHEME, "localhost", DEFAULT_DOCKER_PORT);

	private LocalHttpClientTransport(HttpClient client, HttpHost host) {
		super(client, host);
	}

	static LocalHttpClientTransport create(ResolvedDockerHost dockerHost) {
		HttpClientBuilder builder = HttpClients.custom()
			.setConnectionManager(new LocalConnectionManager(dockerHost))
			.setRoutePlanner(new LocalRoutePlanner());
		HttpHost host = new HttpHost(DOCKER_SCHEME, dockerHost.getAddress());
		return new LocalHttpClientTransport(builder.build(), host);
	}

	/**
	 * {@link HttpClientConnectionManager} for local Docker.
	 */
	private static class LocalConnectionManager extends BasicHttpClientConnectionManager {

		private static final ConnectionConfig CONNECTION_CONFIG = ConnectionConfig.copy(ConnectionConfig.DEFAULT)
			.setValidateAfterInactivity(TimeValue.NEG_ONE_MILLISECOND)
			.build();

		private static final Lookup<TlsSocketStrategy> NO_TLS_SOCKET = (name) -> null;

		LocalConnectionManager(ResolvedDockerHost dockerHost) {
			super(createhttpClientConnectionOperator(dockerHost), null);
			setConnectionConfig(CONNECTION_CONFIG);
		}

		private static DefaultHttpClientConnectionOperator createhttpClientConnectionOperator(
				ResolvedDockerHost dockerHost) {
			LocalDetachedSocketFactory detachedSocketFactory = new LocalDetachedSocketFactory(dockerHost);
			LocalDnsResolver dnsResolver = new LocalDnsResolver();
			return new DefaultHttpClientConnectionOperator(detachedSocketFactory, null, dnsResolver, NO_TLS_SOCKET);
		}

	}

	/**
	 * {@link DetachedSocketFactory} for local Docker.
	 */
	static class LocalDetachedSocketFactory implements DetachedSocketFactory {

		private static final String NPIPE_PREFIX = "npipe://";

		private final ResolvedDockerHost dockerHost;

		LocalDetachedSocketFactory(ResolvedDockerHost dockerHost) {
			this.dockerHost = dockerHost;
		}

		@Override
		public Socket create(Proxy proxy) throws IOException {
			String address = this.dockerHost.getAddress();
			if (address.startsWith(NPIPE_PREFIX)) {
				return NamedPipeSocket.get(address.substring(NPIPE_PREFIX.length()));
			}
			return (!Platform.isWindows()) ? UnixDomainSocket.get(address) : NamedPipeSocket.get(address);
		}

	}

	/**
	 * {@link DnsResolver} that ensures only the loopback address is used.
	 */
	private static final class LocalDnsResolver implements DnsResolver {

		private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

		@Override
		public InetAddress[] resolve(String host) {
			return new InetAddress[] { LOOPBACK };
		}

		@Override
		public String resolveCanonicalHostname(String host) {
			return LOOPBACK.getCanonicalHostName();
		}

	}

	/**
	 * {@link HttpRoutePlanner} for local Docker.
	 */
	private static final class LocalRoutePlanner implements HttpRoutePlanner {

		@Override
		public HttpRoute determineRoute(HttpHost target, HttpContext context) {
			return new HttpRoute(LOCAL_DOCKER_HOST);
		}

	}

}
