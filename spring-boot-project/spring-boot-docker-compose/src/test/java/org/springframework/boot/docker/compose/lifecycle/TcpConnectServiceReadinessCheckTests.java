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
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.List;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.docker.compose.core.ConnectionPorts;
import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TcpConnectServiceReadinessCheck}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class TcpConnectServiceReadinessCheckTests {

	private static final int EPHEMERAL_PORT = 0;

	private TcpConnectServiceReadinessCheck readinessCheck;

	@BeforeEach
	void setup() {
		DockerComposeProperties.Readiness.Tcp tcpProperties = new DockerComposeProperties.Readiness.Tcp();
		tcpProperties.setConnectTimeout(Duration.ofMillis(100));
		tcpProperties.setReadTimeout(Duration.ofMillis(100));
		this.readinessCheck = new TcpConnectServiceReadinessCheck(tcpProperties);
	}

	@Test
	void checkWhenServerWritesData() throws Exception {
		withServer((socket) -> socket.getOutputStream().write('!'), (port) -> check(port));
	}

	@Test
	void checkWhenNoSocketOutput() throws Exception {
		// Simulate waiting for traffic from client to server. The sleep duration must
		// be longer than the read timeout of the ready check!
		withServer((socket) -> sleep(Duration.ofSeconds(10)), (port) -> check(port));
	}

	@Test
	void checkWhenImmediateDisconnect() throws IOException {
		withServer(Socket::close,
				(port) -> assertThatExceptionOfType(ServiceNotReadyException.class).isThrownBy(() -> check(port))
					.withMessage("Immediate disconnect while connecting to port %d".formatted(port)));
	}

	@Test
	void checkWhenNoServerListening() {
		assertThatExceptionOfType(ServiceNotReadyException.class).isThrownBy(() -> check(12345))
			.withMessage("IOException while connecting to port 12345");
	}

	private void withServer(ThrowingConsumer<Socket> socketAction, ThrowingConsumer<Integer> portAction)
			throws IOException {
		try (ServerSocket serverSocket = new ServerSocket()) {
			serverSocket.bind(new InetSocketAddress("127.0.0.1", EPHEMERAL_PORT));
			Thread thread = new Thread(() -> {
				try (Socket socket = serverSocket.accept()) {
					socketAction.accept(socket);
				}
				catch (IOException ex) {
					throw new UncheckedIOException(ex);
				}
			});
			thread.setName("Acceptor-%d".formatted(serverSocket.getLocalPort()));
			thread.setUncaughtExceptionHandler((ignored, ex) -> ex.printStackTrace());
			thread.setDaemon(true);
			thread.start();
			portAction.accept(serverSocket.getLocalPort());
		}
	}

	private void check(Integer port) {
		this.readinessCheck.check(mockRunningService(port));
	}

	private RunningService mockRunningService(Integer port) {
		RunningService runningService = mock(RunningService.class);
		ConnectionPorts ports = mock(ConnectionPorts.class);
		given(ports.getAll("tcp")).willReturn(List.of(port));
		given(runningService.host()).willReturn("localhost");
		given(runningService.ports()).willReturn(ports);
		return runningService;
	}

	private void sleep(Duration duration) {
		try {
			Thread.sleep(duration.toMillis());
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

}
