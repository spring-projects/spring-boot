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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;

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
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslConfigurationValidator;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link JettyServerCustomizer} that configures SSL on the given Jetty server instance.
 *
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chris Bono
 */
class SslServerCustomizer implements JettyServerCustomizer {

	private final InetSocketAddress address;

	private final Ssl ssl;

	private final SslStoreProvider sslStoreProvider;

	private final Http2 http2;

	SslServerCustomizer(InetSocketAddress address, Ssl ssl, SslStoreProvider sslStoreProvider, Http2 http2) {
		this.address = address;
		this.ssl = ssl;
		this.sslStoreProvider = sslStoreProvider;
		this.http2 = http2;
	}

	@Override
	public void customize(Server server) {
		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
		sslContextFactory.setEndpointIdentificationAlgorithm(null);
		configureSsl(sslContextFactory, this.ssl, this.sslStoreProvider);
		ServerConnector connector = createConnector(server, sslContextFactory, this.address);
		server.setConnectors(new Connector[] { connector });
	}

	private ServerConnector createConnector(Server server, SslContextFactory.Server sslContextFactory,
			InetSocketAddress address) {
		HttpConfiguration config = new HttpConfiguration();
		config.setSendServerVersion(false);
		config.setSecureScheme("https");
		config.setSecurePort(address.getPort());
		config.addCustomizer(new SecureRequestCustomizer());
		ServerConnector connector = createServerConnector(server, sslContextFactory, config);
		connector.setPort(address.getPort());
		connector.setHost(address.getHostString());
		return connector;
	}

	private ServerConnector createServerConnector(Server server, SslContextFactory.Server sslContextFactory,
			HttpConfiguration config) {
		if (this.http2 == null || !this.http2.isEnabled()) {
			return createHttp11ServerConnector(server, config, sslContextFactory);
		}
		Assert.state(isAlpnPresent(),
				() -> "The 'org.eclipse.jetty:jetty-alpn-server' dependency is required for HTTP/2 support.");
		Assert.state(isConscryptPresent(), () -> "The 'org.eclipse.jetty.http2:http2-server' and Conscrypt "
				+ "dependencies are required for HTTP/2 support.");
		return createHttp2ServerConnector(server, config, sslContextFactory);
	}

	private ServerConnector createHttp11ServerConnector(Server server, HttpConfiguration config,
			SslContextFactory.Server sslContextFactory) {
		HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(sslContextFactory,
				HttpVersion.HTTP_1_1.asString());
		return new SslValidatingServerConnector(server, sslContextFactory, this.ssl.getKeyAlias(), sslConnectionFactory,
				connectionFactory);
	}

	private boolean isAlpnPresent() {
		return ClassUtils.isPresent("org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory", null);
	}

	private boolean isConscryptPresent() {
		return ClassUtils.isPresent("org.conscrypt.Conscrypt", null);
	}

	private ServerConnector createHttp2ServerConnector(Server server, HttpConfiguration config,
			SslContextFactory.Server sslContextFactory) {
		HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(config);
		ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
		sslContextFactory.setCipherComparator(HTTP2Cipher.COMPARATOR);
		sslContextFactory.setProvider("Conscrypt");
		SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());
		return new SslValidatingServerConnector(server, sslContextFactory, this.ssl.getKeyAlias(), ssl, alpn, h2,
				new HttpConnectionFactory(config));
	}

	/**
	 * Configure the SSL connection.
	 * @param factory the Jetty {@link Server SslContextFactory.Server}.
	 * @param ssl the ssl details.
	 * @param sslStoreProvider the ssl store provider
	 */
	protected void configureSsl(SslContextFactory.Server factory, Ssl ssl, SslStoreProvider sslStoreProvider) {
		factory.setProtocol(ssl.getProtocol());
		configureSslClientAuth(factory, ssl);
		configureSslPasswords(factory, ssl);
		factory.setCertAlias(ssl.getKeyAlias());
		if (!ObjectUtils.isEmpty(ssl.getCiphers())) {
			factory.setIncludeCipherSuites(ssl.getCiphers());
			factory.setExcludeCipherSuites();
		}
		if (ssl.getEnabledProtocols() != null) {
			factory.setIncludeProtocols(ssl.getEnabledProtocols());
		}
		if (sslStoreProvider != null) {
			try {
				factory.setKeyStore(sslStoreProvider.getKeyStore());
				factory.setTrustStore(sslStoreProvider.getTrustStore());
			}
			catch (Exception ex) {
				throw new IllegalStateException("Unable to set SSL store", ex);
			}
		}
		else {
			configureSslKeyStore(factory, ssl);
			configureSslTrustStore(factory, ssl);
		}
	}

	private void configureSslClientAuth(SslContextFactory.Server factory, Ssl ssl) {
		if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			factory.setNeedClientAuth(true);
			factory.setWantClientAuth(true);
		}
		else if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			factory.setWantClientAuth(true);
		}
	}

	private void configureSslPasswords(SslContextFactory.Server factory, Ssl ssl) {
		if (ssl.getKeyStorePassword() != null) {
			factory.setKeyStorePassword(ssl.getKeyStorePassword());
		}
		if (ssl.getKeyPassword() != null) {
			factory.setKeyManagerPassword(ssl.getKeyPassword());
		}
	}

	private void configureSslKeyStore(SslContextFactory.Server factory, Ssl ssl) {
		try {
			URL url = ResourceUtils.getURL(ssl.getKeyStore());
			factory.setKeyStoreResource(Resource.newResource(url));
		}
		catch (Exception ex) {
			throw new WebServerException("Could not load key store '" + ssl.getKeyStore() + "'", ex);
		}
		if (ssl.getKeyStoreType() != null) {
			factory.setKeyStoreType(ssl.getKeyStoreType());
		}
		if (ssl.getKeyStoreProvider() != null) {
			factory.setKeyStoreProvider(ssl.getKeyStoreProvider());
		}
	}

	private void configureSslTrustStore(SslContextFactory.Server factory, Ssl ssl) {
		if (ssl.getTrustStorePassword() != null) {
			factory.setTrustStorePassword(ssl.getTrustStorePassword());
		}
		if (ssl.getTrustStore() != null) {
			try {
				URL url = ResourceUtils.getURL(ssl.getTrustStore());
				factory.setTrustStoreResource(Resource.newResource(url));
			}
			catch (IOException ex) {
				throw new WebServerException("Could not find trust store '" + ssl.getTrustStore() + "'", ex);
			}
		}
		if (ssl.getTrustStoreType() != null) {
			factory.setTrustStoreType(ssl.getTrustStoreType());
		}
		if (ssl.getTrustStoreProvider() != null) {
			factory.setTrustStoreProvider(ssl.getTrustStoreProvider());
		}
	}

	/**
	 * A {@link ServerConnector} that validates the ssl key alias on server startup.
	 */
	static class SslValidatingServerConnector extends ServerConnector {

		private SslContextFactory sslContextFactory;

		private String keyAlias;

		SslValidatingServerConnector(Server server, SslContextFactory sslContextFactory, String keyAlias,
				SslConnectionFactory sslConnectionFactory, HttpConnectionFactory connectionFactory) {
			super(server, sslConnectionFactory, connectionFactory);
			this.sslContextFactory = sslContextFactory;
			this.keyAlias = keyAlias;
		}

		SslValidatingServerConnector(Server server, SslContextFactory sslContextFactory, String keyAlias,
				ConnectionFactory... factories) {
			super(server, factories);
			this.sslContextFactory = sslContextFactory;
			this.keyAlias = keyAlias;
		}

		@Override
		protected void doStart() throws Exception {
			super.doStart();
			SslConfigurationValidator.validateKeyAlias(this.sslContextFactory.getKeyStore(), this.keyAlias);
		}

	}

}
