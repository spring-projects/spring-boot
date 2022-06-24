/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * An {@link SslStoreProvider} that creates key and trust stores from certificate and
 * private key PEM files.
 *
 * @author Scott Frederick
 * @since 2.7.0
 */
public final class CertificateFileSslStoreProvider implements SslStoreProvider {

	/**
	 * The password of the private key entry in the {@link #getKeyStore provided
	 * KeyStore}.
	 */
	private static final String KEY_PASSWORD = "";

	private static final String DEFAULT_KEY_ALIAS = "spring-boot-web";

	private final Ssl ssl;

	private CertificateFileSslStoreProvider(Ssl ssl) {
		this.ssl = ssl;
	}

	@Override
	public KeyStore getKeyStore() throws Exception {
		return createKeyStore(this.ssl.getCertificate(), this.ssl.getCertificatePrivateKey(),
				this.ssl.getKeyStoreType(), this.ssl.getKeyAlias());
	}

	@Override
	public KeyStore getTrustStore() throws Exception {
		if (this.ssl.getTrustCertificate() == null) {
			return null;
		}
		return createKeyStore(this.ssl.getTrustCertificate(), this.ssl.getTrustCertificatePrivateKey(),
				this.ssl.getTrustStoreType(), this.ssl.getKeyAlias());
	}

	@Override
	public String getKeyPassword() {
		return KEY_PASSWORD;
	}

	/**
	 * Create a new {@link KeyStore} populated with the certificate stored at the
	 * specified file path and an optional private key.
	 * @param certPath the path to the certificate authority file
	 * @param keyPath the path to the private file
	 * @param storeType the {@code KeyStore} type to create
	 * @param keyAlias the alias to use when adding keys to the {@code KeyStore}
	 * @return the {@code KeyStore}
	 */
	private KeyStore createKeyStore(String certPath, String keyPath, String storeType, String keyAlias) {
		try {
			KeyStore keyStore = KeyStore.getInstance((storeType != null) ? storeType : KeyStore.getDefaultType());
			keyStore.load(null);
			X509Certificate[] certificates = CertificateParser.parse(certPath);
			PrivateKey privateKey = (keyPath != null) ? PrivateKeyParser.parse(keyPath) : null;
			try {
				addCertificates(keyStore, certificates, privateKey, keyAlias);
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
			}
			return keyStore;
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
		}
	}

	private void addCertificates(KeyStore keyStore, X509Certificate[] certificates, PrivateKey privateKey,
			String keyAlias) throws KeyStoreException {
		String alias = (keyAlias != null) ? keyAlias : DEFAULT_KEY_ALIAS;
		if (privateKey != null) {
			keyStore.setKeyEntry(alias, privateKey, KEY_PASSWORD.toCharArray(), certificates);
		}
		else {
			for (int index = 0; index < certificates.length; index++) {
				keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
			}
		}
	}

	/**
	 * Create an {@link SslStoreProvider} if the appropriate SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @return an {@code SslStoreProvider} or {@code null}
	 */
	public static SslStoreProvider from(Ssl ssl) {
		if (ssl != null && ssl.isEnabled()) {
			if (ssl.getCertificate() != null && ssl.getCertificatePrivateKey() != null) {
				return new CertificateFileSslStoreProvider(ssl);
			}
		}
		return null;
	}

}
