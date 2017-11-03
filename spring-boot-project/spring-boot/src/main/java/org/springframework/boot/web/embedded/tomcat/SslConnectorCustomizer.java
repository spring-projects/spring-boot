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

package org.springframework.boot.web.embedded.tomcat;

import java.io.FileNotFoundException;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 */
class SslConnectorCustomizer implements TomcatConnectorCustomizer {

	private final Ssl ssl;

	private final SslStoreProvider sslStoreProvider;

	SslConnectorCustomizer(Ssl ssl, SslStoreProvider sslStoreProvider) {
		Assert.notNull(ssl, "Ssl configuration should not be null");
		this.ssl = ssl;
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public void customize(Connector connector) {
		ProtocolHandler handler = connector.getProtocolHandler();
		Assert.state(handler instanceof AbstractHttp11JsseProtocol,
				"To use SSL, the connector's protocol handler must be an "
						+ "AbstractHttp11JsseProtocol subclass");
		configureSsl((AbstractHttp11JsseProtocol<?>) handler, this.ssl,
				this.sslStoreProvider);
		connector.setScheme("https");
		connector.setSecure(true);
	}

	/**
	 * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
	 * @param protocol the protocol
	 * @param ssl the ssl details
	 * @param sslStoreProvider the ssl store provider
	 */
	protected void configureSsl(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl,
			SslStoreProvider sslStoreProvider) {
		protocol.setSSLEnabled(true);
		protocol.setSslProtocol(ssl.getProtocol());
		configureSslClientAuth(protocol, ssl);
		protocol.setKeystorePass(ssl.getKeyStorePassword());
		protocol.setKeyPass(ssl.getKeyPassword());
		protocol.setKeyAlias(ssl.getKeyAlias());
		String ciphers = StringUtils.arrayToCommaDelimitedString(ssl.getCiphers());
		if (StringUtils.hasText(ciphers)) {
			protocol.setCiphers(ciphers);
		}
		if (ssl.getEnabledProtocols() != null) {
			for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
				sslHostConfig.setProtocols(StringUtils
						.arrayToCommaDelimitedString(ssl.getEnabledProtocols()));
			}
		}
		if (sslStoreProvider != null) {
			configureSslStoreProvider(protocol, sslStoreProvider);
		}
		else {
			configureSslKeyStore(protocol, ssl);
			configureSslTrustStore(protocol, ssl);
		}
	}

	private void configureSslClientAuth(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl) {
		if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			protocol.setClientAuth(Boolean.TRUE.toString());
		}
		else if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			protocol.setClientAuth("want");
		}
	}

	protected void configureSslStoreProvider(AbstractHttp11JsseProtocol<?> protocol,
			SslStoreProvider sslStoreProvider) {
		Assert.isInstanceOf(Http11NioProtocol.class, protocol,
				"SslStoreProvider can only be used with Http11NioProtocol");
		TomcatURLStreamHandlerFactory instance = TomcatURLStreamHandlerFactory
				.getInstance();
		instance.addUserFactory(
				new SslStoreProviderUrlStreamHandlerFactory(sslStoreProvider));
		protocol.setKeystoreFile(SslStoreProviderUrlStreamHandlerFactory.KEY_STORE_URL);
		protocol.setTruststoreFile(
				SslStoreProviderUrlStreamHandlerFactory.TRUST_STORE_URL);
	}

	private void configureSslKeyStore(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl) {
		try {
			protocol.setKeystoreFile(ResourceUtils.getURL(ssl.getKeyStore()).toString());
		}
		catch (FileNotFoundException ex) {
			throw new WebServerException("Could not load key store: " + ex.getMessage(),
					ex);
		}
		if (ssl.getKeyStoreType() != null) {
			protocol.setKeystoreType(ssl.getKeyStoreType());
		}
		if (ssl.getKeyStoreProvider() != null) {
			protocol.setKeystoreProvider(ssl.getKeyStoreProvider());
		}
	}

	private void configureSslTrustStore(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl) {
		if (ssl.getTrustStore() != null) {
			try {
				protocol.setTruststoreFile(
						ResourceUtils.getURL(ssl.getTrustStore()).toString());
			}
			catch (FileNotFoundException ex) {
				throw new WebServerException(
						"Could not load trust store: " + ex.getMessage(), ex);
			}
		}
		protocol.setTruststorePass(ssl.getTrustStorePassword());
		if (ssl.getTrustStoreType() != null) {
			protocol.setTruststoreType(ssl.getTrustStoreType());
		}
		if (ssl.getTrustStoreProvider() != null) {
			protocol.setTruststoreProvider(ssl.getTrustStoreProvider());
		}
	}

}
