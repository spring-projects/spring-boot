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

package org.springframework.boot.web.embedded.jetty;

import java.net.InetSocketAddress;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http2.HTTP2Cipher;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link JettyServerCustomizer} that configures SSL on the given Jetty server instance.
 *
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chris Bono
 * @author Cyril Dangerville
 * @author Scott Frederick
 */
class SslServerCustomizer implements JettyServerCustomizer {

	private final Http2 http2;

	private final InetSocketAddress address;

	private final ClientAuth clientAuth;

	private final SslBundle sslBundle;

	/**
	 * Constructs a new instance of SslServerCustomizer with the specified parameters.
	 * @param http2 the Http2 object to be used for HTTP/2 configuration
	 * @param address the InetSocketAddress representing the server address
	 * @param clientAuth the ClientAuth object representing the client authentication
	 * configuration
	 * @param sslBundle the SslBundle object containing the SSL/TLS configuration
	 */
	SslServerCustomizer(Http2 http2, InetSocketAddress address, ClientAuth clientAuth, SslBundle sslBundle) {
		this.address = address;
		this.clientAuth = clientAuth;
		this.sslBundle = sslBundle;
		this.http2 = http2;
	}

	/**
	 * Customizes the server by configuring SSL settings.
	 * @param server the server to be customized
	 */
	@Override
	public void customize(Server server) {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setEndpointIdentificationAlgorithm(null);
		configureSsl(sslContextFactory, this.clientAuth);
		ServerConnector connector = createConnector(server, sslContextFactory);
		server.setConnectors(new Connector[] { connector });
	}

	/**
	 * Creates a ServerConnector with the given server and SSL context factory.
	 * @param server the server to create the connector for
	 * @param sslContextFactory the SSL context factory to use for secure connections
	 * @return the created ServerConnector
	 */
	private ServerConnector createConnector(Server server, SslContextFactory.Server sslContextFactory) {
		HttpConfiguration config = new HttpConfiguration();
		config.setSendServerVersion(false);
		config.setSecureScheme("https");
		config.setSecurePort(this.address.getPort());
		config.addCustomizer(new SecureRequestCustomizer());
		ServerConnector connector = createServerConnector(server, sslContextFactory, config);
		connector.setPort(this.address.getPort());
		connector.setHost(this.address.getHostString());
		return connector;
	}

	/**
	 * Creates a server connector for the given server, SSL context factory, and HTTP
	 * configuration. If HTTP/2 support is enabled, a HTTP/2 server connector is created.
	 * If HTTP/2 support is not enabled, a HTTP/1.1 server connector is created.
	 * @param server the server instance
	 * @param sslContextFactory the SSL context factory
	 * @param config the HTTP configuration
	 * @return the created server connector
	 * @throws IllegalStateException if the required dependencies for HTTP/2 support are
	 * not present
	 */
	private ServerConnector createServerConnector(Server server, SslContextFactory.Server sslContextFactory,
			HttpConfiguration config) {
		if (this.http2 == null || !this.http2.isEnabled()) {
			return createHttp11ServerConnector(config, sslContextFactory, server);
		}
		Assert.state(isJettyAlpnPresent(),
				() -> "An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.");
		Assert.state(isJettyHttp2Present(),
				() -> "The 'org.eclipse.jetty.http2:jetty-http2-server' dependency is required for HTTP/2 support.");
		return createHttp2ServerConnector(config, sslContextFactory, server);
	}

	/**
	 * Creates a HTTP/1.1 server connector with the given configuration, SSL context
	 * factory, and server.
	 * @param config the HTTP configuration
	 * @param sslContextFactory the SSL context factory
	 * @param server the server
	 * @return the created server connector
	 */
	private ServerConnector createHttp11ServerConnector(HttpConfiguration config,
			SslContextFactory.Server sslContextFactory, Server server) {
		SslConnectionFactory sslConnectionFactory = createSslConnectionFactory(sslContextFactory,
				HttpVersion.HTTP_1_1.asString());
		HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
		return new SslValidatingServerConnector(this.sslBundle.getKey(), sslContextFactory, server,
				sslConnectionFactory, connectionFactory);
	}

	/**
	 * Creates a new instance of {@link SslConnectionFactory} with the provided
	 * {@link SslContextFactory.Server} and protocol.
	 * @param sslContextFactory the {@link SslContextFactory.Server} to be used for SSL
	 * configuration
	 * @param protocol the SSL protocol to be used
	 * @return a new instance of {@link SslConnectionFactory}
	 */
	private SslConnectionFactory createSslConnectionFactory(SslContextFactory.Server sslContextFactory,
			String protocol) {
		return new SslConnectionFactory(sslContextFactory, protocol);
	}

	/**
	 * Checks if the Jetty ALPN (Application Layer Protocol Negotiation) library is
	 * present.
	 * @return {@code true} if the Jetty ALPN library is present, {@code false} otherwise.
	 */
	private boolean isJettyAlpnPresent() {
		return ClassUtils.isPresent("org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory", null);
	}

	/**
	 * Checks if Jetty HTTP/2 is present.
	 * @return true if Jetty HTTP/2 is present, false otherwise
	 */
	private boolean isJettyHttp2Present() {
		return ClassUtils.isPresent("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory", null);
	}

	/**
	 * Creates a HTTP/2 server connector with the given configuration, SSL context
	 * factory, and server.
	 * @param config the HTTP configuration
	 * @param sslContextFactory the SSL context factory
	 * @param server the server
	 * @return the created server connector
	 */
	private ServerConnector createHttp2ServerConnector(HttpConfiguration config,
			SslContextFactory.Server sslContextFactory, Server server) {
		HttpConnectionFactory http = new HttpConnectionFactory(config);
		HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(config);
		ALPNServerConnectionFactory alpn = createAlpnServerConnectionFactory();
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		if (isConscryptPresent()) {
			sslContextFactory.setProvider("Conscrypt");
		}
		SslConnectionFactory sslConnectionFactory = createSslConnectionFactory(sslContextFactory, alpn.getProtocol());
		return new SslValidatingServerConnector(this.sslBundle.getKey(), sslContextFactory, server,
				sslConnectionFactory, alpn, h2, http);
	}

	/**
	 * Creates an ALPNServerConnectionFactory.
	 * @return the ALPNServerConnectionFactory
	 * @throws IllegalStateException if an 'org.eclipse.jetty:jetty-alpn-*-server'
	 * dependency is required for HTTP/2 support
	 */
	private ALPNServerConnectionFactory createAlpnServerConnectionFactory() {
		try {
			return new ALPNServerConnectionFactory();
		}
		catch (IllegalStateException ex) {
			throw new IllegalStateException(
					"An 'org.eclipse.jetty:jetty-alpn-*-server' dependency is required for HTTP/2 support.", ex);
		}
	}

	/**
	 * Checks if Conscrypt library is present.
	 * @return true if Conscrypt library is present, false otherwise.
	 */
	private boolean isConscryptPresent() {
		return ClassUtils.isPresent("org.conscrypt.Conscrypt", null)
				&& ClassUtils.isPresent("org.eclipse.jetty.alpn.conscrypt.server.ConscryptServerALPNProcessor", null);
	}

	/**
	 * Configure the SSL connection.
	 * @param factory the Jetty {@link Server SslContextFactory.Server}.
	 * @param clientAuth the client authentication mode
	 */
	protected void configureSsl(SslContextFactory.Server factory, ClientAuth clientAuth) {
		SslBundleKey key = this.sslBundle.getKey();
		SslOptions options = this.sslBundle.getOptions();
		SslStoreBundle stores = this.sslBundle.getStores();
		factory.setProtocol(this.sslBundle.getProtocol());
		configureSslClientAuth(factory, clientAuth);
		if (stores.getKeyStorePassword() != null) {
			factory.setKeyStorePassword(stores.getKeyStorePassword());
		}
		factory.setCertAlias(key.getAlias());
		if (options.getCiphers() != null) {
			factory.setIncludeCipherSuites(options.getCiphers());
			factory.setExcludeCipherSuites();
		}
		if (options.getEnabledProtocols() != null) {
			factory.setIncludeProtocols(options.getEnabledProtocols());
			factory.setExcludeProtocols();
		}
		try {
			if (key.getPassword() != null) {
				factory.setKeyManagerPassword(key.getPassword());
			}
			factory.setKeyStore(stores.getKeyStore());
			factory.setTrustStore(stores.getTrustStore());
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to set SSL store: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Configures SSL client authentication for the given SslContextFactory.Server
	 * instance.
	 * @param factory the SslContextFactory.Server instance to configure
	 * @param clientAuth the desired client authentication mode
	 */
	private void configureSslClientAuth(SslContextFactory.Server factory, ClientAuth clientAuth) {
		factory.setWantClientAuth(clientAuth == ClientAuth.WANT || clientAuth == ClientAuth.NEED);
		factory.setNeedClientAuth(clientAuth == ClientAuth.NEED);
	}

	/**
	 * A {@link ServerConnector} that validates the ssl key alias on server startup.
	 */
	static class SslValidatingServerConnector extends ServerConnector {

		private final SslBundleKey key;

		private final SslContextFactory sslContextFactory;

		/**
		 * Constructs a new SslValidatingServerConnector with the specified SSL bundle
		 * key, SSL context factory, server, SSL connection factory, and HTTP connection
		 * factory.
		 * @param key the SSL bundle key used for validating the SSL certificate
		 * @param sslContextFactory the SSL context factory used for creating SSL contexts
		 * @param server the server to which this connector is being added
		 * @param sslConnectionFactory the SSL connection factory used for creating SSL
		 * connections
		 * @param connectionFactory the HTTP connection factory used for creating HTTP
		 * connections
		 */
		SslValidatingServerConnector(SslBundleKey key, SslContextFactory sslContextFactory, Server server,
				SslConnectionFactory sslConnectionFactory, HttpConnectionFactory connectionFactory) {
			super(server, sslConnectionFactory, connectionFactory);
			this.key = key;
			this.sslContextFactory = sslContextFactory;
		}

		/**
		 * Constructs a new SslValidatingServerConnector with the specified SSL bundle key
		 * alias, SSL context factory, server, and connection factories.
		 * @param keyAlias the SSL bundle key alias to be used for SSL validation
		 * @param sslContextFactory the SSL context factory to be used for SSL
		 * configuration
		 * @param server the server to be associated with the connector
		 * @param factories the connection factories to be used for creating connections
		 */
		SslValidatingServerConnector(SslBundleKey keyAlias, SslContextFactory sslContextFactory, Server server,
				ConnectionFactory... factories) {
			super(server, factories);
			this.key = keyAlias;
			this.sslContextFactory = sslContextFactory;
		}

		/**
		 * Starts the SSL validating server connector.
		 * @throws Exception if an error occurs while starting the connector
		 */
		@Override
		protected void doStart() throws Exception {
			super.doStart();
			this.key.assertContainsAlias(this.sslContextFactory.getKeyStore());
		}

	}

}
