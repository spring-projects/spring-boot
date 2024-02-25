/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.server;

import java.security.KeyStore;

import org.springframework.boot.ssl.NoSuchSslBundleException;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link SslBundle} backed by {@link Ssl}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public final class WebServerSslBundle implements SslBundle {

	private final SslStoreBundle stores;

	private final SslBundleKey key;

	private final SslOptions options;

	private final String protocol;

	private final SslManagerBundle managers;

	/**
	 * Constructs a new WebServerSslBundle with the specified SSL store bundle, key
	 * password, and SSL configuration.
	 * @param stores the SSL store bundle containing the SSL certificates and keys
	 * @param keyPassword the password for the SSL key
	 * @param ssl the SSL configuration specifying the key alias, protocol, ciphers, and
	 * enabled protocols
	 */
	private WebServerSslBundle(SslStoreBundle stores, String keyPassword, Ssl ssl) {
		this.stores = stores;
		this.key = SslBundleKey.of(keyPassword, ssl.getKeyAlias());
		this.protocol = ssl.getProtocol();
		this.options = SslOptions.of(ssl.getCiphers(), ssl.getEnabledProtocols());
		this.managers = SslManagerBundle.from(this.stores, this.key);
	}

	/**
	 * Creates a PEM key store bundle for SSL configuration.
	 * @param ssl the SSL configuration
	 * @return the PEM key store bundle
	 */
	private static SslStoreBundle createPemKeyStoreBundle(Ssl ssl) {
		PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails(ssl.getKeyStoreType(), ssl.getCertificate(),
				ssl.getCertificatePrivateKey())
			.withAlias(ssl.getKeyAlias());
		return new PemSslStoreBundle(keyStoreDetails, null);
	}

	/**
	 * Creates a PEM trust store bundle for the given SSL configuration.
	 * @param ssl the SSL configuration
	 * @return the PEM trust store bundle
	 */
	private static SslStoreBundle createPemTrustStoreBundle(Ssl ssl) {
		PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustCertificate(), ssl.getTrustCertificatePrivateKey())
			.withAlias(ssl.getKeyAlias());
		return new PemSslStoreBundle(null, trustStoreDetails);
	}

	/**
	 * Creates a JKS key store bundle for SSL configuration.
	 * @param ssl the SSL configuration details
	 * @return the JKS key store bundle
	 */
	private static SslStoreBundle createJksKeyStoreBundle(Ssl ssl) {
		JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(),
				ssl.getKeyStore(), ssl.getKeyStorePassword());
		return new JksSslStoreBundle(keyStoreDetails, null);
	}

	/**
	 * Creates a JKS trust store bundle for SSL configuration.
	 * @param ssl the SSL configuration
	 * @return the JKS trust store bundle
	 */
	private static SslStoreBundle createJksTrustStoreBundle(Ssl ssl) {
		JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustStoreProvider(), ssl.getTrustStore(), ssl.getTrustStorePassword());
		return new JksSslStoreBundle(null, trustStoreDetails);
	}

	/**
	 * Returns the SSL store bundle associated with this WebServerSslBundle.
	 * @return the SSL store bundle
	 */
	@Override
	public SslStoreBundle getStores() {
		return this.stores;
	}

	/**
	 * Returns the SSL bundle key associated with this WebServerSslBundle.
	 * @return the SSL bundle key
	 */
	@Override
	public SslBundleKey getKey() {
		return this.key;
	}

	/**
	 * Returns the SSL options for the web server.
	 * @return the SSL options for the web server
	 */
	@Override
	public SslOptions getOptions() {
		return this.options;
	}

	/**
	 * Returns the protocol used by the WebServerSslBundle.
	 * @return the protocol used by the WebServerSslBundle
	 */
	@Override
	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * Returns the SslManagerBundle object associated with this WebServerSslBundle.
	 * @return the SslManagerBundle object associated with this WebServerSslBundle
	 */
	@Override
	public SslManagerBundle getManagers() {
		return this.managers;
	}

	/**
	 * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
	 * @param ssl the source ssl instance
	 * @return a {@link SslBundle} instance
	 * @throws NoSuchSslBundleException if a bundle lookup fails
	 */
	public static SslBundle get(Ssl ssl) throws NoSuchSslBundleException {
		return get(ssl, null);
	}

	/**
	 * Get the {@link SslBundle} that should be used for the given {@link Ssl} instance.
	 * @param ssl the source ssl instance
	 * @param sslBundles the bundles that should be used when {@link Ssl#getBundle()} is
	 * set
	 * @return a {@link SslBundle} instance
	 * @throws NoSuchSslBundleException if a bundle lookup fails
	 */
	public static SslBundle get(Ssl ssl, SslBundles sslBundles) throws NoSuchSslBundleException {
		Assert.state(Ssl.isEnabled(ssl), "SSL is not enabled");
		String keyPassword = ssl.getKeyPassword();
		String bundleName = ssl.getBundle();
		if (StringUtils.hasText(bundleName)) {
			Assert.state(sslBundles != null,
					() -> "SSL bundle '%s' was requested but no SslBundles instance was provided"
						.formatted(bundleName));
			return sslBundles.getBundle(bundleName);
		}
		SslStoreBundle stores = createStoreBundle(ssl);
		return new WebServerSslBundle(stores, keyPassword, ssl);
	}

	/**
	 * Creates a new SSL store bundle using the provided SSL configuration.
	 * @param ssl the SSL configuration to use for creating the store bundle
	 * @return the created SSL store bundle
	 */
	private static SslStoreBundle createStoreBundle(Ssl ssl) {
		KeyStore keyStore = createKeyStore(ssl);
		KeyStore trustStore = createTrustStore(ssl);
		return new WebServerSslStoreBundle(keyStore, trustStore, ssl.getKeyStorePassword());
	}

	/**
	 * Creates a KeyStore based on the provided SSL configuration.
	 * @param ssl the SSL configuration
	 * @return the created KeyStore
	 */
	private static KeyStore createKeyStore(Ssl ssl) {
		if (hasPemKeyStoreProperties(ssl)) {
			return createPemKeyStoreBundle(ssl).getKeyStore();
		}
		else if (hasJksKeyStoreProperties(ssl)) {
			return createJksKeyStoreBundle(ssl).getKeyStore();
		}
		return null;
	}

	/**
	 * Creates a trust store based on the provided SSL configuration.
	 * @param ssl the SSL configuration
	 * @return the trust store
	 */
	private static KeyStore createTrustStore(Ssl ssl) {
		if (hasPemTrustStoreProperties(ssl)) {
			return createPemTrustStoreBundle(ssl).getTrustStore();
		}
		else if (hasJksTrustStoreProperties(ssl)) {
			return createJksTrustStoreBundle(ssl).getTrustStore();
		}
		return null;
	}

	/**
	 * Checks if the given SSL configuration has the necessary properties for a PEM key
	 * store.
	 * @param ssl the SSL configuration to check
	 * @return true if the SSL configuration has a certificate and a private key, false
	 * otherwise
	 */
	private static boolean hasPemKeyStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
	}

	/**
	 * Checks if the provided SSL configuration has PEM trust store properties.
	 * @param ssl the SSL configuration to check
	 * @return true if the SSL is enabled and has a trust certificate, false otherwise
	 */
	private static boolean hasPemTrustStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getTrustCertificate() != null;
	}

	/**
	 * Checks if the given SSL configuration has properties related to a JKS keystore.
	 * @param ssl the SSL configuration to check
	 * @return true if the SSL configuration has JKS keystore properties, false otherwise
	 */
	private static boolean hasJksKeyStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && (ssl.getKeyStore() != null
				|| (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11")));
	}

	/**
	 * Checks if the provided SSL configuration has properties related to a JKS trust
	 * store.
	 * @param ssl the SSL configuration to check
	 * @return true if the SSL is enabled and has either a trust store or a trust store
	 * type of "PKCS11", false otherwise
	 */
	private static boolean hasJksTrustStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && (ssl.getTrustStore() != null
				|| (ssl.getTrustStoreType() != null && ssl.getTrustStoreType().equals("PKCS11")));
	}

	/**
	 * Returns a string representation of the WebServerSslBundle object.
	 * @return a string representation of the WebServerSslBundle object
	 */
	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("key", this.key);
		creator.append("protocol", this.protocol);
		creator.append("stores", this.stores);
		creator.append("options", this.options);
		return creator.toString();
	}

	/**
	 * WebServerSslStoreBundle class.
	 */
	private static final class WebServerSslStoreBundle implements SslStoreBundle {

		private final KeyStore keyStore;

		private final KeyStore trustStore;

		private final String keyStorePassword;

		/**
		 * Constructs a new WebServerSslStoreBundle with the specified key store, trust
		 * store, and key store password.
		 * @param keyStore the key store containing the SSL certificate and private key
		 * @param trustStore the trust store containing the trusted certificates
		 * @param keyStorePassword the password for the key store
		 * @throws IllegalArgumentException if SSL is enabled but no trust material is
		 * configured
		 */
		private WebServerSslStoreBundle(KeyStore keyStore, KeyStore trustStore, String keyStorePassword) {
			Assert.state(keyStore != null || trustStore != null, "SSL is enabled but no trust material is configured");
			this.keyStore = keyStore;
			this.trustStore = trustStore;
			this.keyStorePassword = keyStorePassword;
		}

		/**
		 * Returns the KeyStore object associated with this WebServerSslStoreBundle.
		 * @return the KeyStore object
		 */
		@Override
		public KeyStore getKeyStore() {
			return this.keyStore;
		}

		/**
		 * Returns the trust store used by the WebServerSslStoreBundle.
		 * @return the trust store used by the WebServerSslStoreBundle
		 */
		@Override
		public KeyStore getTrustStore() {
			return this.trustStore;
		}

		/**
		 * Returns the password for the keystore.
		 * @return the password for the keystore
		 */
		@Override
		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		/**
		 * Returns a string representation of the WebServerSslStoreBundle object.
		 * @return a string representation of the WebServerSslStoreBundle object
		 */
		@Override
		public String toString() {
			ToStringCreator creator = new ToStringCreator(this);
			creator.append("keyStore.type", (this.keyStore != null) ? this.keyStore.getType() : "none");
			creator.append("keyStorePassword", (this.keyStorePassword != null) ? "******" : null);
			creator.append("trustStore.type", (this.trustStore != null) ? this.trustStore.getType() : "none");
			return creator.toString();
		}

	}

}
