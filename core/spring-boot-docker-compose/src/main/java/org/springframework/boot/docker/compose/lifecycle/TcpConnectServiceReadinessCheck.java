/*
 * Copyright 2012-present the original author or authors.
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
			// Ignore
		}
	}

}
