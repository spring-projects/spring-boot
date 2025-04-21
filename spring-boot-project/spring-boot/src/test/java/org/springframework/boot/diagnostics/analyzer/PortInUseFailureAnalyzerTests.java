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

package org.springframework.boot.diagnostics.analyzer;

import java.net.ServerSocket;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.boot.web.server.PortInUseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PortInUseFailureAnalyzer}.
 *
 * @author Yonatan Graber
 */
class PortInUseFailureAnalyzerTests {

	private final PortInUseFailureAnalyzer analyzer = new PortInUseFailureAnalyzer();

	@Test
	void analyzeNoProcessInfo() throws Exception {
		// Best effort attempt to get a port that is not bound to any process
		ServerSocket serverSocket = new ServerSocket(0);
		int port = serverSocket.getLocalPort();
		serverSocket.close();

		PortInUseException exception = new PortInUseException(port, null);
		FailureAnalysis analysis = this.analyzer.analyze(new RuntimeException("Connection failed", exception),
				exception);

		assertThat(analysis.getDescription())
			.contains("Web server failed to start. Port " + port + " was already in use.");
		assertThat(analysis.getAction()).contains("Identify and stop the process that's listening on port " + port
				+ " or configure this application to listen on another port.");
		assertThat(analysis.getCause()).isSameAs(exception);
	}

	@Test
	void analyzeWithProcessInfo() throws Exception {
		// bind a port to this process and check if the analyzer can find it
		long pid = new ApplicationPid().toLong();
		try (ServerSocket serverSocket = new ServerSocket(0)) {
			int port = serverSocket.getLocalPort();
			PortInUseException exception = new PortInUseException(port, null);
			FailureAnalysis analysis = this.analyzer.analyze(new RuntimeException("Po", exception), exception);
			assertThat(analysis.getDescription())
				.contains("Web server failed to start. Port " + port + " was already in use ")
				.contains("(PID: " + pid + ")");
			assertThat(analysis.getAction()).contains("Stop the process ").contains("(PID: " + pid + ")");
		}
	}

}
