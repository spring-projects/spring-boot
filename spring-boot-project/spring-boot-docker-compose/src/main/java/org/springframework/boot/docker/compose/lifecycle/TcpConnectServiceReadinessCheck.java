/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.springframework.boot.docker.compose.core.RunningService;

/**
 * Checks readiness by connecting to the exposed TCP ports.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class TcpConnectServiceReadinessCheck {

	private static final String DISABLE_LABEL = "org.springframework.boot.readiness-check.tcp.disable";

	private final DockerComposeProperties.Readiness.Tcp properties;

	TcpConnectServiceReadinessCheck(DockerComposeProperties.Readiness.Tcp properties) {
		this.properties = properties;
	}

	void check(RunningService service) {
		if (service.labels().containsKey(DISABLE_LABEL)) {
			return;
		}
		for (int port : service.ports().getAll("tcp")) {
			check(service, port);
		}
	}

	private void check(RunningService service, int port) {
		int connectTimeout = (int) this.properties.getConnectTimeout().toMillis();
		int readTimeout = (int) this.properties.getReadTimeout().toMillis();
		try (Socket socket = new Socket()) {
			socket.setSoTimeout(readTimeout);
			socket.connect(new InetSocketAddress(service.host(), port), connectTimeout);
			check(service, port, socket);
		}
		catch (IOException ex) {
			throw new ServiceNotReadyException(service, "IOException while connecting to port %s".formatted(port), ex);
		}
	}

	private void check(RunningService service, int port, Socket socket) throws IOException {
		try {
			// -1 indicates the socket has been closed immediately
			// Other responses or a timeout are considered as success
			if (socket.getInputStream().read() == -1) {
				throw new ServiceNotReadyException(service,
						"Immediate disconnect while connecting to port %s".formatted(port));
			}
		}
		catch (SocketTimeoutException ex) {
		}
	}

}
