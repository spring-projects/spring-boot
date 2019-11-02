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

package org.springframework.boot.cloudnativebuildpack.docker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.sun.jna.Platform;
import org.apache.http.HttpHost;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import org.springframework.boot.cloudnativebuildpack.socket.DomainSocket;
import org.springframework.boot.cloudnativebuildpack.socket.NamedPipeSocket;

/**
 * {@link ConnectionSocketFactory} that connects to the Docker domain socket or named
 * pipe.
 *
 * @author Phillip Webb
 */
class DockerConnectionSocketFactory implements ConnectionSocketFactory {

	private static final String DOMAIN_SOCKET_PATH = "/var/run/docker.sock";

	private static final String WINDOWS_NAMED_PIPE_PATH = "//./pipe/docker_engine";

	@Override
	public Socket createSocket(HttpContext context) throws IOException {
		if (Platform.isWindows()) {
			NamedPipeSocket.get(WINDOWS_NAMED_PIPE_PATH);
		}
		return DomainSocket.get(DOMAIN_SOCKET_PATH);
	}

	@Override
	public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host, InetSocketAddress remoteAddress,
			InetSocketAddress localAddress, HttpContext context) throws IOException {
		return sock;
	}

}
