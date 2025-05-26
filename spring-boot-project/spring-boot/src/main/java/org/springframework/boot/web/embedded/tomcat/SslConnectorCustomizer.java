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

package org.springframework.boot.web.embedded.tomcat;

import java.util.Map;

import org.apache.catalina.connector.Connector;
import org.apache.commons.logging.Log;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.SSLHostConfigCertificate.Type;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility that configures SSL support on the given connector.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Cyril Dangerville
 * @author Moritz Halbritter
 */
class SslConnectorCustomizer {

	private final Log logger;

	private final ClientAuth clientAuth;

	private final Connector connector;

	SslConnectorCustomizer(Log logger, Connector connector, ClientAuth clientAuth) {
		this.logger = logger;
		this.clientAuth = clientAuth;
		this.connector = connector;
	}

	void update(String serverName, SslBundle updatedSslBundle) {
		AbstractHttp11Protocol<?> protocol = (AbstractHttp11Protocol<?>) this.connector.getProtocolHandler();
		String host = (serverName != null) ? serverName : protocol.getDefaultSSLHostConfigName();
		this.logger.debug("SSL Bundle for host " + host + " has been updated, reloading SSL configuration");
		addSslHostConfig(protocol, host, updatedSslBundle);
	}

	void customize(SslBundle sslBundle, Map<String, SslBundle> serverNameSslBundles) {
		ProtocolHandler handler = this.connector.getProtocolHandler();
		Assert.state(handler instanceof AbstractHttp11Protocol,
				"To use SSL, the connector's protocol handler must be an AbstractHttp11Protocol subclass");
		configureSsl((AbstractHttp11Protocol<?>) handler, sslBundle, serverNameSslBundles);
		this.connector.setScheme("https");
		this.connector.setSecure(true);
	}

	/**
	 * Configure Tomcat's {@link AbstractHttp11Protocol} for SSL.
	 * @param protocol the protocol
	 * @param sslBundle the SSL bundle
	 * @param serverNameSslBundles the SSL bundles for specific SNI host names
	 */
	private void configureSsl(AbstractHttp11Protocol<?> protocol, SslBundle sslBundle,
			Map<String, SslBundle> serverNameSslBundles) {
		protocol.setSSLEnabled(true);
		if (sslBundle != null) {
			addSslHostConfig(protocol, protocol.getDefaultSSLHostConfigName(), sslBundle);
		}
		serverNameSslBundles.forEach((serverName, bundle) -> addSslHostConfig(protocol, serverName, bundle));
	}

	private void addSslHostConfig(AbstractHttp11Protocol<?> protocol, String serverName, SslBundle sslBundle) {
		SSLHostConfig sslHostConfig = new SSLHostConfig();
		sslHostConfig.setHostName(serverName);
		configureSslClientAuth(sslHostConfig);
		applySslBundle(protocol, sslHostConfig, sslBundle);
		protocol.addSslHostConfig(sslHostConfig, true);
	}

	private void applySslBundle(AbstractHttp11Protocol<?> protocol, SSLHostConfig sslHostConfig, SslBundle sslBundle) {
		SslBundleKey key = sslBundle.getKey();
		SslStoreBundle stores = sslBundle.getStores();
		SslOptions options = sslBundle.getOptions();
		sslHostConfig.setSslProtocol(sslBundle.getProtocol());
		SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(sslHostConfig, Type.UNDEFINED);
		String keystorePassword = (stores.getKeyStorePassword() != null) ? stores.getKeyStorePassword() : "";
		certificate.setCertificateKeystorePassword(keystorePassword);
		if (key.getPassword() != null) {
			certificate.setCertificateKeyPassword(key.getPassword());
		}
		if (key.getAlias() != null) {
			certificate.setCertificateKeyAlias(key.getAlias());
		}
		sslHostConfig.addCertificate(certificate);
		if (options.getCiphers() != null) {
			String ciphers = StringUtils.arrayToCommaDelimitedString(options.getCiphers());
			sslHostConfig.setCiphers(ciphers);
		}
		configureSslStores(sslHostConfig, certificate, stores);
		configureEnabledProtocols(sslHostConfig, options);
	}

	private void configureEnabledProtocols(SSLHostConfig sslHostConfig, SslOptions options) {
		if (options.getEnabledProtocols() != null) {
			String enabledProtocols = StringUtils.arrayToDelimitedString(options.getEnabledProtocols(), "+");
			sslHostConfig.setProtocols(enabledProtocols);
		}
	}

	private void configureSslClientAuth(SSLHostConfig config) {
		config.setCertificateVerification(ClientAuth.map(this.clientAuth, "none", "optional", "required"));
	}

	private void configureSslStores(SSLHostConfig sslHostConfig, SSLHostConfigCertificate certificate,
			SslStoreBundle stores) {
		try {
			if (stores.getKeyStore() != null) {
				certificate.setCertificateKeystore(stores.getKeyStore());
			}
			if (stores.getTrustStore() != null) {
				sslHostConfig.setTrustStore(stores.getTrustStore());
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store: " + ex.getMessage(), ex);
		}
	}

}
