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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.pem.KeyVerifier.Result;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public class PemSslStoreBundle implements SslStoreBundle {

	private static final String DEFAULT_KEY_ALIAS = "ssl";

	private final KeyStore keyStore;

	private final KeyStore trustStore;

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 */
	public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails) {
		this(keyStoreDetails, trustStoreDetails, null);
	}

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 * @param keyAlias the key alias to use or {@code null} to use a default alias
	 */
	public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails,
			String keyAlias) {
		this(keyStoreDetails, trustStoreDetails, keyAlias, null);
	}

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 * @param keyAlias the key alias to use or {@code null} to use a default alias
	 * @param keyPassword the password to use for the key
	 * @since 3.2.0
	 */
	public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails, String keyAlias,
			String keyPassword) {
		this(keyStoreDetails, trustStoreDetails, keyAlias, keyPassword, false);
	}

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 * @param keyAlias the key alias to use or {@code null} to use a default alias
	 * @param keyPassword the password to use for the key
	 * @param verifyKeys whether to verify that the private key matches the public key
	 * @since 3.2.0
	 */
	public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails, String keyAlias,
			String keyPassword, boolean verifyKeys) {
		this.keyStore = createKeyStore("key", keyStoreDetails, (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS,
				keyPassword, verifyKeys);
		this.trustStore = createKeyStore("trust", trustStoreDetails, (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS,
				keyPassword, verifyKeys);
	}

	@Override
	public KeyStore getKeyStore() {
		return this.keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return null;
	}

	@Override
	public KeyStore getTrustStore() {
		return this.trustStore;
	}

	private static KeyStore createKeyStore(String name, PemSslStoreDetails details, String keyAlias, String keyPassword,
			boolean verifyKeys) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		try {
			Assert.notNull(details.certificate(), "Certificate content must not be null");
			KeyStore store = createKeyStore(details);
			X509Certificate[] certificates = loadCertificates(details);
			PrivateKey privateKey = loadPrivateKey(details);
			if (privateKey != null) {
				if (verifyKeys) {
					verifyKeys(privateKey, certificates);
				}
				addPrivateKey(store, privateKey, keyAlias, keyPassword, certificates);
			}
			else {
				addCertificates(store, certificates, keyAlias);
			}
			return store;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
		}
	}

	private static void verifyKeys(PrivateKey privateKey, X509Certificate[] certificates) {
		KeyVerifier keyVerifier = new KeyVerifier();
		// Key should match one of the certificates
		for (X509Certificate certificate : certificates) {
			Result result = keyVerifier.matches(privateKey, certificate.getPublicKey());
			if (result == Result.YES) {
				return;
			}
		}
		throw new IllegalStateException("Private key matches none of the certificates");
	}

	private static PrivateKey loadPrivateKey(PemSslStoreDetails details) {
		String privateKeyContent = PemContent.load(details.privateKey());
		return PemPrivateKeyParser.parse(privateKeyContent, details.privateKeyPassword());
	}

	private static X509Certificate[] loadCertificates(PemSslStoreDetails details) {
		String certificateContent = PemContent.load(details.certificate());
		X509Certificate[] certificates = PemCertificateParser.parse(certificateContent);
		Assert.state(certificates != null && certificates.length > 0, "Loaded certificates are empty");
		return certificates;
	}

	private static KeyStore createKeyStore(PemSslStoreDetails details)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		String type = StringUtils.hasText(details.type()) ? details.type() : KeyStore.getDefaultType();
		KeyStore store = KeyStore.getInstance(type);
		store.load(null);
		return store;
	}

	private static void addPrivateKey(KeyStore keyStore, PrivateKey privateKey, String alias, String keyPassword,
			X509Certificate[] certificates) throws KeyStoreException {
		keyStore.setKeyEntry(alias, privateKey, (keyPassword != null) ? keyPassword.toCharArray() : null, certificates);
	}

	private static void addCertificates(KeyStore keyStore, X509Certificate[] certificates, String alias)
			throws KeyStoreException {
		for (int index = 0; index < certificates.length; index++) {
			keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
		}
	}

}
