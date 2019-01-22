/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.embedded.jetty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.Test;

import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 */
public class SslServerCustomizerTests {

	@Test
	@SuppressWarnings("rawtypes")
	public void whenHttp2IsNotEnabledServerConnectorHasSslAndHttpConnectionFactories() {
		Server server = createCustomizedServer();
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(
				server.getConnectors()[0].getConnectionFactories());
		assertThat(factories).extracting((factory) -> (Class) factory.getClass())
				.containsExactly(SslConnectionFactory.class, HttpConnectionFactory.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void whenHttp2IsEnabledServerConnectorsHasSslAlpnH2AndHttpConnectionFactories() {
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		Server server = createCustomizedServer(http2);
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(
				server.getConnectors()[0].getConnectionFactories());
		assertThat(factories).extracting((factory) -> (Class) factory.getClass())
				.containsExactly(SslConnectionFactory.class,
						ALPNServerConnectionFactory.class,
						HTTP2ServerConnectionFactory.class, HttpConnectionFactory.class);
	}

	@Test
	public void alpnConnectionFactoryHasNullDefaultProtocolToAllowNegotiationToHttp11() {
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		Server server = createCustomizedServer(http2);
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(
				server.getConnectors()[0].getConnectionFactories());
		assertThat(((ALPNServerConnectionFactory) factories.get(1)).getDefaultProtocol())
				.isNull();
	}

	@Test
	public void configureSslWhenSslIsEnabledWithNoKeyStoreThrowsWebServerException()
			throws Exception {
		Ssl ssl = new Ssl();
		SslServerCustomizer customizer = new SslServerCustomizer(null, ssl, null, null);
		try {
			customizer.configureSsl(new SslContextFactory(), ssl, null);
		}
		catch (Exception ex) {
			assertThat(ex).isInstanceOf(WebServerException.class);
			assertThat(ex).hasMessageContaining("Could not load key store 'null'");
		}
	}

	private Server createCustomizedServer() {
		return createCustomizedServer(new Http2());
	}

	private Server createCustomizedServer(Http2 http2) {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("classpath:test.jks");
		return createCustomizedServer(ssl, http2);
	}

	private Server createCustomizedServer(Ssl ssl, Http2 http2) {
		Server server = new Server();
		new SslServerCustomizer(new InetSocketAddress(0), ssl, null, http2)
				.customize(server);
		return server;
	}

}
