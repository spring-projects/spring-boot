/*
 * Copyright 2012-2022 the original author or authors.
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

import java.net.InetSocketAddress;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import org.springframework.boot.testsupport.junit.DisabledOnOs;
import org.springframework.boot.web.embedded.netty.MockPkcs11SecurityProvider;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Tests for {@link SslServerCustomizer}.
 *
 * @author Andy Wilkinson
 * @author Cyril Dangerville
 */
class SslServerCustomizerTests {

	private static final Provider PKCS11_PROVIDER = new MockPkcs11SecurityProvider();

	@BeforeAll
	static void beforeAllTests() {
		/*
		 * Add the mock Java security provider for PKCS#11-related unit tests.
		 *
		 */
		Security.addProvider(PKCS11_PROVIDER);
	}

	@AfterAll
	static void afterAllTests() {
		// Remove the provider previously added in setup()
		Security.removeProvider(PKCS11_PROVIDER.getName());
	}

	@Test
	@SuppressWarnings("rawtypes")
	void whenHttp2IsNotEnabledServerConnectorHasSslAndHttpConnectionFactories() {
		Server server = createCustomizedServer();
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
		assertThat(factories).extracting((factory) -> (Class) factory.getClass())
				.containsExactly(SslConnectionFactory.class, HttpConnectionFactory.class);
	}

	@Test
	@SuppressWarnings("rawtypes")
	@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
			disabledReason = "conscrypt doesn't support Linux/macOS aarch64, see https://github.com/google/conscrypt/issues/1051")
	void whenHttp2IsEnabledServerConnectorsHasSslAlpnH2AndHttpConnectionFactories() {
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		Server server = createCustomizedServer(http2);
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
		assertThat(factories).extracting((factory) -> (Class) factory.getClass()).containsExactly(
				SslConnectionFactory.class, ALPNServerConnectionFactory.class, HTTP2ServerConnectionFactory.class,
				HttpConnectionFactory.class);
	}

	@Test
	@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
			disabledReason = "conscrypt doesn't support Linux/macOS aarch64, see https://github.com/google/conscrypt/issues/1051")
	void alpnConnectionFactoryHasNullDefaultProtocolToAllowNegotiationToHttp11() {
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		Server server = createCustomizedServer(http2);
		assertThat(server.getConnectors()).hasSize(1);
		List<ConnectionFactory> factories = new ArrayList<>(server.getConnectors()[0].getConnectionFactories());
		assertThat(((ALPNServerConnectionFactory) factories.get(1)).getDefaultProtocol()).isNull();
	}

	/**
	 * Null/undefined keystore is invalid unless keystore type is PKCS11.
	 */
	@Test
	void configureSslWhenSslIsEnabledWithNoKeyStoreAndNotPkcs11ThrowsWebServerException() {
		Ssl ssl = new Ssl();
		SslServerCustomizer customizer = new SslServerCustomizer(null, ssl, null, null);
		assertThatExceptionOfType(Exception.class)
				.isThrownBy(() -> customizer.configureSsl(new SslContextFactory.Server(), ssl, null))
				.satisfies((ex) -> {
					assertThat(ex).isInstanceOf(WebServerException.class);
					assertThat(ex).hasMessageContaining("Could not load key store 'null'");
				});
	}

	/**
	 * No keystore path should be defined if keystore type is PKCS#11.
	 */
	@Test
	void configureSslWhenSslIsEnabledWithPkcs11AndKeyStoreThrowsIllegalArgumentException() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		SslServerCustomizer customizer = new SslServerCustomizer(null, ssl, null, null);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> customizer.configureSsl(new SslContextFactory.Server(), ssl, null))
				.withMessageContaining("Input keystore location is not valid for keystore type 'PKCS11'");
	}

	@Test
	void customizeWhenSslIsEnabledWithPkcs11AndKeyStoreProvider() {
		Ssl ssl = new Ssl();
		ssl.setKeyStoreType("PKCS11");
		ssl.setKeyStoreProvider(PKCS11_PROVIDER.getName());
		ssl.setKeyStorePassword("1234");
		SslServerCustomizer customizer = new SslServerCustomizer(null, ssl, null, null);
		// Loading the KeyManagerFactory should be successful
		assertThatNoException().isThrownBy(() -> customizer.configureSsl(new SslContextFactory.Server(), ssl, null));
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
		new SslServerCustomizer(new InetSocketAddress(0), ssl, null, http2).customize(server);
		return server;
	}

}
