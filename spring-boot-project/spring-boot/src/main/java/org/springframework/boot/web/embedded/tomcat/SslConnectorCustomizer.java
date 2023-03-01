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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.SslStoreProviderFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Cyril Dangerville
 */
class SslConnectorCustomizer implements TomcatConnectorCustomizer {

	private final Ssl ssl;

	private final SslStoreProvider sslStoreProvider;

	SslConnectorCustomizer(Ssl ssl) {
		this(ssl, SslStoreProviderFactory.from(ssl));
	}

	SslConnectorCustomizer(Ssl ssl, SslStoreProvider sslStoreProvider) {
		Assert.notNull(ssl, "Ssl configuration should not be null");
		this.ssl = ssl;
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public void customize(Connector connector) {
		ProtocolHandler handler = connector.getProtocolHandler();
		Assert.state(handler instanceof AbstractHttp11JsseProtocol,
				"To use SSL, the connector's protocol handler must be an AbstractHttp11JsseProtocol subclass");
		configureSsl((AbstractHttp11JsseProtocol<?>) handler, this.ssl, this.sslStoreProvider);
		connector.setScheme("https");
		connector.setSecure(true);
	}

	/**
	 * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
	 * @param protocol the protocol
	 * @param ssl the ssl details
	 * @param sslStoreProvider the ssl store provider
	 */
	protected void configureSsl(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl, SslStoreProvider sslStoreProvider) {
		protocol.setSSLEnabled(true);
		SSLHostConfig sslHostConfig = new SSLHostConfig();
		sslHostConfig.setHostName(protocol.getDefaultSSLHostConfigName());
		sslHostConfig.setSslProtocol(ssl.getProtocol());
		protocol.addSslHostConfig(sslHostConfig);
		configureSslClientAuth(sslHostConfig, ssl);
		SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
		if (ssl.getKeyStorePassword() != null) {
			certificate.setCertificateKeystorePassword(ssl.getKeyStorePassword());
		}
		if (ssl.getKeyPassword() != null) {
			certificate.setCertificateKeyPassword(ssl.getKeyPassword());
		}
		if (ssl.getKeyAlias() != null) {
			certificate.setCertificateKeyAlias(ssl.getKeyAlias());
		}
		sslHostConfig.addCertificate(certificate);
		String ciphers = StringUtils.arrayToCommaDelimitedString(ssl.getCiphers());
		if (StringUtils.hasText(ciphers)) {
			sslHostConfig.setCiphers(ciphers);
		}
		configureEnabledProtocols(protocol, ssl);
		if (sslStoreProvider != null) {
			configureSslStoreProvider(protocol, sslHostConfig, certificate, sslStoreProvider);
			String keyPassword = sslStoreProvider.getKeyPassword();
			if (keyPassword != null) {
				certificate.setCertificateKeyPassword(keyPassword);
			}
		}
	}

	private void configureEnabledProtocols(AbstractHttp11JsseProtocol<?> protocol, Ssl ssl) {
		if (ssl.getEnabledProtocols() != null) {
			for (SSLHostConfig sslHostConfig : protocol.findSslHostConfigs()) {
				sslHostConfig.setProtocols(StringUtils.arrayToCommaDelimitedString(ssl.getEnabledProtocols()));
			}
		}
	}

	private void configureSslClientAuth(SSLHostConfig config, Ssl ssl) {
		if (ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			config.setCertificateVerification("required");
		}
		else if (ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			config.setCertificateVerification("optional");
		}
	}

	protected void configureSslStoreProvider(AbstractHttp11JsseProtocol<?> protocol, SSLHostConfig sslHostConfig,
			SSLHostConfigCertificate certificate, SslStoreProvider sslStoreProvider) {
		Assert.isInstanceOf(Http11NioProtocol.class, protocol,
				"SslStoreProvider can only be used with Http11NioProtocol");
		try {
			if (sslStoreProvider.getKeyStore() != null) {
				certificate.setCertificateKeystore(sslStoreProvider.getKeyStore());
			}
			if (sslStoreProvider.getTrustStore() != null) {
				sslHostConfig.setTrustStore(sslStoreProvider.getTrustStore());
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store: " + ex.getMessage(), ex);
		}
	}

}
