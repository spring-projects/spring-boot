/*
 * Copyright 2012-2017 the original author or authors.
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

import java.io.IOException;
import java.net.URL;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;

/**
 * {@link JettyServerCustomizer} that configures SSL on the given Jetty server instance.
 *
 * @author Brian Clozel
 */
class SslServerCustomizer implements JettyServerCustomizer {

	private final int port;

	private final Ssl ssl;

	private final SslStoreProvider sslStoreProvider;

	SslServerCustomizer(int port, Ssl ssl, SslStoreProvider sslStoreProvider) {
		this.port = port;
		this.ssl = ssl;
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public void customize(Server server) {
		if (this.ssl != null && this.ssl.isEnabled()) {
			SslContextFactory sslContextFactory = new SslContextFactory();
			configureSsl(sslContextFactory, this.ssl, this.sslStoreProvider);
			AbstractConnector connector = createSslConnector(server, sslContextFactory,
					this.port);
			server.setConnectors(new Connector[] { connector });
		}
	}

	private AbstractConnector createSslConnector(Server server,
			SslContextFactory sslContextFactory, int port) {
		HttpConfiguration config = new HttpConfiguration();
		config.setSendServerVersion(false);
		config.addCustomizer(new SecureRequestCustomizer());
		HttpConnectionFactory connectionFactory = new HttpConnectionFactory(config);
		SslConnectionFactory sslConnectionFactory = new SslConnectionFactory(
				sslContextFactory, HttpVersion.HTTP_1_1.asString());
		ServerConnector serverConnector = new ServerConnector(server,
				sslConnectionFactory, connectionFactory);
		serverConnector.setPort(port);
		return serverConnector;
	}

	/**
	 * Configure the SSL connection.
	 * @param factory the Jetty {@link SslContextFactory}.
	 * @param ssl the ssl details.
	 * @param sslStoreProvider the ssl store provider
	 */
	protected void configureSsl(SslContextFactory factory, Ssl ssl,
			SslStoreProvider sslStoreProvider) {
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

	private void configureSslClientAuth(SslContextFactory factory, Ssl ssl) {
		if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			factory.setNeedClientAuth(true);
			factory.setWantClientAuth(true);
		}
		else if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			factory.setWantClientAuth(true);
		}
	}

	private void configureSslPasswords(SslContextFactory factory, Ssl ssl) {
		if (ssl.getKeyStorePassword() != null) {
			factory.setKeyStorePassword(ssl.getKeyStorePassword());
		}
		if (ssl.getKeyPassword() != null) {
			factory.setKeyManagerPassword(ssl.getKeyPassword());
		}
	}

	private void configureSslKeyStore(SslContextFactory factory, Ssl ssl) {
		try {
			URL url = ResourceUtils.getURL(ssl.getKeyStore());
			factory.setKeyStoreResource(Resource.newResource(url));
		}
		catch (IOException ex) {
			throw new WebServerException(
					"Could not find key store '" + ssl.getKeyStore() + "'", ex);
		}
		if (ssl.getKeyStoreType() != null) {
			factory.setKeyStoreType(ssl.getKeyStoreType());
		}
		if (ssl.getKeyStoreProvider() != null) {
			factory.setKeyStoreProvider(ssl.getKeyStoreProvider());
		}
	}

	private void configureSslTrustStore(SslContextFactory factory, Ssl ssl) {
		if (ssl.getTrustStorePassword() != null) {
			factory.setTrustStorePassword(ssl.getTrustStorePassword());
		}
		if (ssl.getTrustStore() != null) {
			try {
				URL url = ResourceUtils.getURL(ssl.getTrustStore());
				factory.setTrustStoreResource(Resource.newResource(url));
			}
			catch (IOException ex) {
				throw new WebServerException(
						"Could not find trust store '" + ssl.getTrustStore() + "'", ex);
			}
		}
		if (ssl.getTrustStoreType() != null) {
			factory.setTrustStoreType(ssl.getTrustStoreType());
		}
		if (ssl.getTrustStoreProvider() != null) {
			factory.setTrustStoreProvider(ssl.getTrustStoreProvider());
		}
	}

}
