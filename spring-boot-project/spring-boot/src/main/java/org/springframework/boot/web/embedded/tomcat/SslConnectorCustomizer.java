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
import org.apache.commons.logging.Log;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11JsseProtocol;
import org.apache.coyote.http11.Http11NioProtocol;
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

	/**
     * Constructs a new SslConnectorCustomizer with the specified logger, connector, and client authentication.
     * 
     * @param logger the logger used for logging messages
     * @param connector the connector to be customized
     * @param clientAuth the client authentication mode to be set on the connector
     */
    SslConnectorCustomizer(Log logger, Connector connector, ClientAuth clientAuth) {
		this.logger = logger;
		this.clientAuth = clientAuth;
		this.connector = connector;
	}

	/**
     * Updates the SSL configuration with the provided SSL bundle.
     * 
     * @param updatedSslBundle the updated SSL bundle containing the new SSL configuration
     */
    void update(SslBundle updatedSslBundle) {
		this.logger.debug("SSL Bundle has been updated, reloading SSL configuration");
		customize(updatedSslBundle);
	}

	/**
     * Customizes the SSL configuration for the given SSL bundle.
     * 
     * @param sslBundle the SSL bundle containing the SSL configuration
     * @throws IllegalStateException if the connector's protocol handler is not an instance of AbstractHttp11JsseProtocol
     */
    void customize(SslBundle sslBundle) {
		ProtocolHandler handler = this.connector.getProtocolHandler();
		Assert.state(handler instanceof AbstractHttp11JsseProtocol,
				"To use SSL, the connector's protocol handler must be an AbstractHttp11JsseProtocol subclass");
		configureSsl(sslBundle, (AbstractHttp11JsseProtocol<?>) handler);
		this.connector.setScheme("https");
		this.connector.setSecure(true);
	}

	/**
	 * Configure Tomcat's {@link AbstractHttp11JsseProtocol} for SSL.
	 * @param sslBundle the SSL bundle
	 * @param protocol the protocol
	 */
	private void configureSsl(SslBundle sslBundle, AbstractHttp11JsseProtocol<?> protocol) {
		protocol.setSSLEnabled(true);
		SSLHostConfig sslHostConfig = new SSLHostConfig();
		sslHostConfig.setHostName(protocol.getDefaultSSLHostConfigName());
		configureSslClientAuth(sslHostConfig);
		applySslBundle(sslBundle, protocol, sslHostConfig);
		protocol.addSslHostConfig(sslHostConfig, true);
	}

	/**
     * Applies the SSL bundle to the given protocol and SSL host configuration.
     * 
     * @param sslBundle the SSL bundle containing the necessary SSL configuration
     * @param protocol the protocol to apply the SSL bundle to
     * @param sslHostConfig the SSL host configuration to apply the SSL bundle to
     */
    private void applySslBundle(SslBundle sslBundle, AbstractHttp11JsseProtocol<?> protocol,
			SSLHostConfig sslHostConfig) {
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
		configureSslStoreProvider(protocol, sslHostConfig, certificate, stores);
		configureEnabledProtocols(sslHostConfig, options);
	}

	/**
     * Configures the enabled protocols for the SSLHostConfig object based on the provided SslOptions object.
     * 
     * @param sslHostConfig The SSLHostConfig object to configure.
     * @param options The SslOptions object containing the enabled protocols.
     */
    private void configureEnabledProtocols(SSLHostConfig sslHostConfig, SslOptions options) {
		if (options.getEnabledProtocols() != null) {
			String enabledProtocols = StringUtils.arrayToDelimitedString(options.getEnabledProtocols(), "+");
			sslHostConfig.setProtocols(enabledProtocols);
		}
	}

	/**
     * Configures SSL client authentication for the specified SSLHostConfig.
     * 
     * @param config the SSLHostConfig to configure
     */
    private void configureSslClientAuth(SSLHostConfig config) {
		config.setCertificateVerification(ClientAuth.map(this.clientAuth, "none", "optional", "required"));
	}

	/**
     * Configures the SSL store provider for the given protocol, SSL host configuration, certificate, and store bundle.
     * 
     * @param protocol The protocol to configure the SSL store provider for. Must be an instance of Http11NioProtocol.
     * @param sslHostConfig The SSL host configuration to use.
     * @param certificate The SSL host configuration certificate to use.
     * @param stores The SSL store bundle to use.
     * @throws IllegalStateException If the store could not be loaded.
     */
    private void configureSslStoreProvider(AbstractHttp11JsseProtocol<?> protocol, SSLHostConfig sslHostConfig,
			SSLHostConfigCertificate certificate, SslStoreBundle stores) {
		Assert.isInstanceOf(Http11NioProtocol.class, protocol,
				"SslStoreProvider can only be used with Http11NioProtocol");
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
