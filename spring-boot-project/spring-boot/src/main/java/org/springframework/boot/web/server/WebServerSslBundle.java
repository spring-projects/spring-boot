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

	private WebServerSslBundle(SslStoreBundle stores, String keyPassword, Ssl ssl) {
		this.stores = stores;
		this.key = SslBundleKey.of(keyPassword, ssl.getKeyAlias());
		this.protocol = ssl.getProtocol();
		this.options = SslOptions.of(ssl.getCiphers(), ssl.getEnabledProtocols());
		this.managers = SslManagerBundle.from(this.stores, this.key);
	}

	private static SslStoreBundle createPemKeyStoreBundle(Ssl ssl) {
		PemSslStoreDetails keyStoreDetails = new PemSslStoreDetails(ssl.getKeyStoreType(), ssl.getCertificate(),
				ssl.getCertificatePrivateKey())
			.withAlias(ssl.getKeyAlias());
		return new PemSslStoreBundle(keyStoreDetails, null);
	}

	private static SslStoreBundle createPemTrustStoreBundle(Ssl ssl) {
		PemSslStoreDetails trustStoreDetails = new PemSslStoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustCertificate(), ssl.getTrustCertificatePrivateKey())
			.withAlias(ssl.getKeyAlias());
		return new PemSslStoreBundle(null, trustStoreDetails);
	}

	private static SslStoreBundle createJksKeyStoreBundle(Ssl ssl) {
		JksSslStoreDetails keyStoreDetails = new JksSslStoreDetails(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(),
				ssl.getKeyStore(), ssl.getKeyStorePassword());
		return new JksSslStoreBundle(keyStoreDetails, null);
	}

	private static SslStoreBundle createJksTrustStoreBundle(Ssl ssl) {
		JksSslStoreDetails trustStoreDetails = new JksSslStoreDetails(ssl.getTrustStoreType(),
				ssl.getTrustStoreProvider(), ssl.getTrustStore(), ssl.getTrustStorePassword());
		return new JksSslStoreBundle(null, trustStoreDetails);
	}

	@Override
	public SslStoreBundle getStores() {
		return this.stores;
	}

	@Override
	public SslBundleKey getKey() {
		return this.key;
	}

	@Override
	public SslOptions getOptions() {
		return this.options;
	}

	@Override
	public String getProtocol() {
		return this.protocol;
	}

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

	private static SslStoreBundle createStoreBundle(Ssl ssl) {
		KeyStore keyStore = createKeyStore(ssl);
		KeyStore trustStore = createTrustStore(ssl);
		return new WebServerSslStoreBundle(keyStore, trustStore, ssl.getKeyStorePassword());
	}

	private static KeyStore createKeyStore(Ssl ssl) {
		if (hasPemKeyStoreProperties(ssl)) {
			return createPemKeyStoreBundle(ssl).getKeyStore();
		}
		else if (hasJksKeyStoreProperties(ssl)) {
			return createJksKeyStoreBundle(ssl).getKeyStore();
		}
		return null;
	}

	private static KeyStore createTrustStore(Ssl ssl) {
		if (hasPemTrustStoreProperties(ssl)) {
			return createPemTrustStoreBundle(ssl).getTrustStore();
		}
		else if (hasJksTrustStoreProperties(ssl)) {
			return createJksTrustStoreBundle(ssl).getTrustStore();
		}
		return null;
	}

	private static boolean hasPemKeyStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null;
	}

	private static boolean hasPemTrustStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && ssl.getTrustCertificate() != null;
	}

	private static boolean hasJksKeyStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && (ssl.getKeyStore() != null
				|| (ssl.getKeyStoreType() != null && ssl.getKeyStoreType().equals("PKCS11")));
	}

	private static boolean hasJksTrustStoreProperties(Ssl ssl) {
		return Ssl.isEnabled(ssl) && (ssl.getTrustStore() != null
				|| (ssl.getTrustStoreType() != null && ssl.getTrustStoreType().equals("PKCS11")));
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("key", this.key);
		creator.append("protocol", this.protocol);
		creator.append("stores", this.stores);
		creator.append("options", this.options);
		return creator.toString();
	}

	private static final class WebServerSslStoreBundle implements SslStoreBundle {

		private final KeyStore keyStore;

		private final KeyStore trustStore;

		private final String keyStorePassword;

		private WebServerSslStoreBundle(KeyStore keyStore, KeyStore trustStore, String keyStorePassword) {
			Assert.state(keyStore != null || trustStore != null,
					"SSL is enabled but no trust material is configured for the default host");
			this.keyStore = keyStore;
			this.trustStore = trustStore;
			this.keyStorePassword = keyStorePassword;
		}

		@Override
		public KeyStore getKeyStore() {
			return this.keyStore;
		}

		@Override
		public KeyStore getTrustStore() {
			return this.trustStore;
		}

		@Override
		public String getKeyStorePassword() {
			return this.keyStorePassword;
		}

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
