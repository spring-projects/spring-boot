/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyWebServer}.
 *
 * @author Michael Schneider
 */
class JettyWebServerTest {

	private JettyWebServer webServer;

	@AfterEach
	void tearDown() {
		if (this.webServer != null) {
			try {
				this.webServer.stop();
			}
			catch (Exception exception) {
				// ignore
			}
		}
	}

	private Server createServer() {
		return new Server(SocketUtils.findAvailableTcpPort());
	}

	@Test
	void startManually() {
		final Server server = createServer();
		this.webServer = new JettyWebServer(server, false);
		assertThat(this.webServer.getServer().isStarted()).isFalse();
		this.webServer.start();
		assertThat(this.webServer.getServer().isStarted()).isTrue();
	}

	@Test
	void startAutomatically() {
		final Server server = createServer();
		this.webServer = new JettyWebServer(server, true);
		assertThat(this.webServer.getServer().isStarted()).isTrue();
	}

}
