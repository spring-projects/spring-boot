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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.sun.jna.Platform;
import org.apache.http.HttpHost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.DnsResolver;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.conn.UnsupportedSchemeException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

import org.springframework.boot.buildpack.platform.socket.DomainSocket;
import org.springframework.boot.buildpack.platform.socket.NamedPipeSocket;
import org.springframework.boot.buildpack.platform.system.Environment;

/**
 * {@link HttpClientTransport} that talks to local Docker.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
final class LocalHttpClientTransport extends HttpClientTransport {

	private static final String UNIX_SOCKET_PREFIX = "unix://";

	private static final String DOCKER_HOST = "DOCKER_HOST";

	private static final HttpHost LOCAL_DOCKER_HOST = HttpHost.create("docker://localhost");

	private LocalHttpClientTransport(CloseableHttpClient client) {
		super(client, LOCAL_DOCKER_HOST);
	}

	static LocalHttpClientTransport create(Environment environment) {
		HttpClientBuilder builder = HttpClients.custom();
		builder.setConnectionManager(new LocalConnectionManager(socketFilePath(environment)));
		builder.setSchemePortResolver(new LocalSchemePortResolver());
		return new LocalHttpClientTransport(builder.build());
	}

	private static String socketFilePath(Environment environment) {
		String host = environment.get(DOCKER_HOST);
		if (host != null && host.startsWith(UNIX_SOCKET_PREFIX)) {
			return host.substring(UNIX_SOCKET_PREFIX.length());
		}
		return host;
	}

	/**
	 * {@link HttpClientConnectionManager} for local Docker.
	 */
	private static class LocalConnectionManager extends BasicHttpClientConnectionManager {

		LocalConnectionManager(String host) {
			super(getRegistry(host), null, null, new LocalDnsResolver());
		}

		private static Registry<ConnectionSocketFactory> getRegistry(String host) {
			RegistryBuilder<ConnectionSocketFactory> builder = RegistryBuilder.create();
			builder.register("docker", new LocalConnectionSocketFactory(host));
			return builder.build();
		}

	}

	/**
	 * {@link DnsResolver} that ensures only the loopback address is used.
	 */
	private static class LocalDnsResolver implements DnsResolver {

		private static final InetAddress[] LOOPBACK = new InetAddress[] { InetAddress.getLoopbackAddress() };

		@Override
		public InetAddress[] resolve(String host) throws UnknownHostException {
			return LOOPBACK;
		}

	}

	/**
	 * {@link ConnectionSocketFactory} that connects to the local Docker domain socket or
	 * named pipe.
	 */
	private static class LocalConnectionSocketFactory implements ConnectionSocketFactory {

		private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

		private static final String WINDOWS_NAMED_PIPE_PATH = "//./pipe/docker_engine";

		private final String host;

		LocalConnectionSocketFactory(String host) {
			this.host = host;
		}

		@Override
		public Socket createSocket(HttpContext context) throws IOException {
			if (Platform.isWindows()) {
				return NamedPipeSocket.get((this.host != null) ? this.host : WINDOWS_NAMED_PIPE_PATH);
			}
			return DomainSocket.get((this.host != null) ? this.host : DOMAIN_SOCKET_PATH);
		}

		@Override
		public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress,
				InetSocketAddress localAddress, HttpContext context) throws IOException {
			return sock;
		}

	}

	/**
	 * {@link SchemePortResolver} for local Docker.
	 */
	private static class LocalSchemePortResolver implements SchemePortResolver {

		private static final int DEFAULT_DOCKER_PORT = 2376;

		@Override
		public int resolve(HttpHost host) throws UnsupportedSchemeException {
			Args.notNull(host, "HTTP host");
			String name = host.getSchemeName();
			if ("docker".equals(name)) {
				return DEFAULT_DOCKER_PORT;
			}
			throw new UnsupportedSchemeException(name + " protocol is not supported");
		}

	}

}
