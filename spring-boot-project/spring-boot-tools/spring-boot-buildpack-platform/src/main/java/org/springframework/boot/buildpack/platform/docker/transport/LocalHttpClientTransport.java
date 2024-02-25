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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.sun.jna.Platform;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;

import org.springframework.boot.buildpack.platform.docker.configuration.ResolvedDockerHost;
import org.springframework.boot.buildpack.platform.socket.DomainSocket;
import org.springframework.boot.buildpack.platform.socket.NamedPipeSocket;

/**
 * {@link HttpClientTransport} that talks to local Docker.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
final class LocalHttpClientTransport extends HttpClientTransport {

	private static final String DOCKER_SCHEME = "docker";

	private static final int DEFAULT_DOCKER_PORT = 2376;

	private static final HttpHost LOCAL_DOCKER_HOST = new HttpHost(DOCKER_SCHEME, "localhost", DEFAULT_DOCKER_PORT);

	/**
	 * Constructs a new LocalHttpClientTransport with the specified HttpClient and
	 * HttpHost.
	 * @param client the HttpClient to be used for the transport
	 * @param host the HttpHost representing the target host
	 */
	private LocalHttpClientTransport(HttpClient client, HttpHost host) {
		super(client, host);
	}

	/**
	 * Creates a new instance of LocalHttpClientTransport with the given
	 * ResolvedDockerHost.
	 * @param dockerHost the ResolvedDockerHost representing the Docker host to connect to
	 * @return a new instance of LocalHttpClientTransport
	 */
	static LocalHttpClientTransport create(ResolvedDockerHost dockerHost) {
		HttpClientBuilder builder = HttpClients.custom();
		builder.setConnectionManager(new LocalConnectionManager(dockerHost.getAddress()));
		builder.setRoutePlanner(new LocalRoutePlanner());
		HttpHost host = new HttpHost(DOCKER_SCHEME, dockerHost.getAddress());
		return new LocalHttpClientTransport(builder.build(), host);
	}

	/**
	 * {@link HttpClientConnectionManager} for local Docker.
	 */
	private static class LocalConnectionManager extends BasicHttpClientConnectionManager {

		/**
		 * Constructs a new LocalConnectionManager with the specified host.
		 * @param host the host to connect to
		 */
		LocalConnectionManager(String host) {
			super(getRegistry(host), null, null, new LocalDnsResolver());
		}

		/**
		 * Returns the registry of connection socket factories for the specified host.
		 * @param host the host for which the registry is to be retrieved
		 * @return the registry of connection socket factories
		 */
		private static Registry<ConnectionSocketFactory> getRegistry(String host) {
			RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
			builder.register(DOCKER_SCHEME, new LocalConnectionSocketFactory(host));
			return builder.build();
		}

	}

	/**
	 * {@link DnsResolver} that ensures only the loopback address is used.
	 */
	private static final class LocalDnsResolver implements DnsResolver {

		private static final InetAddress LOOPBACK = InetAddress.getLoopbackAddress();

		/**
		 * Resolves the given host name to an array of InetAddress objects.
		 * @param host the host name to be resolved
		 * @return an array of InetAddress objects representing the IP addresses of the
		 * host
		 * @throws UnknownHostException if no IP address for the host could be found
		 */
		@Override
		public InetAddress[] resolve(String host) throws UnknownHostException {
			return new InetAddress[] { LOOPBACK };
		}

		/**
		 * Resolves the canonical hostname for the given host.
		 * @param host the host for which to resolve the canonical hostname
		 * @return the canonical hostname for the given host
		 * @throws UnknownHostException if the hostname cannot be resolved
		 */
		@Override
		public String resolveCanonicalHostname(String host) throws UnknownHostException {
			return LOOPBACK.getCanonicalHostName();
		}

	}

	/**
	 * {@link ConnectionSocketFactory} that connects to the local Docker domain socket or
	 * named pipe.
	 */
	private static class LocalConnectionSocketFactory implements ConnectionSocketFactory {

		private final String host;

		/**
		 * Constructs a new LocalConnectionSocketFactory with the specified host.
		 * @param host the host to connect to
		 */
		LocalConnectionSocketFactory(String host) {
			this.host = host;
		}

		/**
		 * Creates a socket for the given HttpContext.
		 * @param context the HttpContext for which the socket is created
		 * @return the created socket
		 * @throws IOException if an I/O error occurs while creating the socket
		 */
		@Override
		public Socket createSocket(HttpContext context) throws IOException {
			if (Platform.isWindows()) {
				return NamedPipeSocket.get(this.host);
			}
			return DomainSocket.get(this.host);
		}

		/**
		 * Connects a socket to the specified remote address with the given timeout.
		 * @param connectTimeout The timeout for establishing the connection.
		 * @param socket The socket to be connected.
		 * @param host The HTTP host to which the socket is being connected.
		 * @param remoteAddress The remote address to connect to.
		 * @param localAddress The local address to bind the socket to.
		 * @param context The context for the connection.
		 * @return The connected socket.
		 * @throws IOException If an I/O error occurs while connecting the socket.
		 */
		@Override
		public Socket connectSocket(TimeValue connectTimeout, Socket socket, HttpHost host,
				InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context)
				throws IOException {
			return socket;
		}

	}

	/**
	 * {@link HttpRoutePlanner} for local Docker.
	 */
	private static final class LocalRoutePlanner implements HttpRoutePlanner {

		/**
		 * Determines the route for the given target and context.
		 * @param target the target HTTP host
		 * @param context the HTTP context
		 * @return the determined HTTP route
		 */
		@Override
		public HttpRoute determineRoute(HttpHost target, HttpContext context) {
			return new HttpRoute(LOCAL_DOCKER_HOST);
		}

	}

}
