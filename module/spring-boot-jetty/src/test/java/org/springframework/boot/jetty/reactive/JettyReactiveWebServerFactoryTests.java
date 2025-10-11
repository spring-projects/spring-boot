/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jetty.reactive;

import java.net.ConnectException;
import java.net.InetAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.awaitility.Awaitility;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.NetworkConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.jetty.JettyAccess;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.boot.jetty.JettyWebServer;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ServerNameSslBundle;
import org.springframework.boot.web.server.reactive.AbstractReactiveWebServerFactoryTests;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyReactiveWebServerFactory} and {@link JettyWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 * @author Moritz Halbritter
 */
class JettyReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected JettyReactiveWebServerFactory getFactory() {
		return new JettyReactiveWebServerFactory(0);
	}

	@Test
	@Override
	@Disabled("Jetty 12 does not support User-Agent-based compression")
	// TODO Is this true with Jetty 12?
	protected void noCompressionForUserAgent() {

	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void setNullServerCustomizersShouldThrowException() {
		JettyReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException().isThrownBy(() -> factory.setServerCustomizers(null))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void addNullServerCustomizersShouldThrowException() {
		JettyReactiveWebServerFactory factory = getFactory();
		assertThatIllegalArgumentException()
			.isThrownBy(() -> factory.addServerCustomizers((JettyServerCustomizer[]) null))
			.withMessageContaining("'customizers' must not be null");
	}

	@Test
	void jettyCustomizersShouldBeInvoked() {
		HttpHandler handler = mock(HttpHandler.class);
		JettyReactiveWebServerFactory factory = getFactory();
		JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
		Arrays.setAll(configurations, (i) -> mock(JettyServerCustomizer.class));
		factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
		factory.addServerCustomizers(configurations[2], configurations[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) configurations);
		for (JettyServerCustomizer configuration : configurations) {
			ordered.verify(configuration).customize(any(Server.class));
		}
	}

	@Test
	void specificIPAddressNotReverseResolved() throws Exception {
		JettyReactiveWebServerFactory factory = getFactory();
		InetAddress localhost = InetAddress.getLocalHost();
		factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		this.webServer.start();
		Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
		assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
	}

	@Test
	void useForwardedHeaders() {
		JettyReactiveWebServerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	void whenServerIsShuttingDownGracefullyThenNewConnectionsCannotBeMade() {
		JettyReactiveWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingHandler blockingHandler = new BlockingHandler();
		this.webServer = factory.getWebServer(blockingHandler);
		this.webServer.start();
		WebClient webClient = getWebClient(this.webServer.getPort()).build();
		this.webServer.shutDownGracefully((result) -> {
		});
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> {
			blockingHandler.stopBlocking();
			try {
				webClient.get().retrieve().toBodilessEntity().block();
				return false;
			}
			catch (RuntimeException ex) {
				return ex.getCause() instanceof ConnectException;
			}
		});
		this.webServer.stop();
	}

	@Test
	void shouldApplyMaxConnections() {
		JettyReactiveWebServerFactory factory = getFactory();
		factory.setMaxConnections(1);
		this.webServer = factory.getWebServer(new EchoHandler());
		Server server = ((JettyWebServer) this.webServer).getServer();
		NetworkConnectionLimit connectionLimit = server.getBean(NetworkConnectionLimit.class);
		assertThat(connectionLimit).isNotNull();
		assertThat(connectionLimit.getMaxNetworkConnectionCount()).isOne();
	}

	@Test
	void sslServerNameBundlesConfigurationThrowsException() {
		Ssl ssl = new Ssl();
		ssl.setBundle("test");
		List<ServerNameSslBundle> bundles = List.of(new ServerNameSslBundle("first", "test1"),
				new ServerNameSslBundle("second", "test2"));
		ssl.setServerNameBundles(bundles);
		JettyReactiveWebServerFactory factory = getFactory();
		factory.setSsl(ssl);
		assertThatIllegalStateException().isThrownBy(() -> this.webServer = factory.getWebServer(new EchoHandler()))
			.withMessageContaining("Server name SSL bundles are not supported with Jetty");
	}

	@Override
	protected String startedLogMessage() {
		return JettyAccess.getStartedLogMessage((JettyWebServer) this.webServer);
	}

	@Override
	protected void addConnector(int port, ConfigurableReactiveWebServerFactory factory) {
		((JettyReactiveWebServerFactory) factory).addServerCustomizers((server) -> {
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(port);
			server.addConnector(connector);
		});
	}

}
