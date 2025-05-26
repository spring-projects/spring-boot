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

package org.springframework.boot.ssl.pem;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

/**
 * {@link SslStoreBundle} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public class PemSslStoreBundle implements SslStoreBundle {

	private static final String DEFAULT_ALIAS = "ssl";

	private final Supplier<KeyStore> keyStore;

	private final Supplier<KeyStore> trustStore;

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 */
	public PemSslStoreBundle(PemSslStoreDetails keyStoreDetails, PemSslStoreDetails trustStoreDetails) {
		this(PemSslStore.load(keyStoreDetails), PemSslStore.load(trustStoreDetails));
	}

	/**
	 * Create a new {@link PemSslStoreBundle} instance.
	 * @param pemKeyStore the PEM key store
	 * @param pemTrustStore the PEM trust store
	 * @since 3.2.0
	 */
	public PemSslStoreBundle(PemSslStore pemKeyStore, PemSslStore pemTrustStore) {
		this.keyStore = SingletonSupplier.of(() -> createKeyStore("key", pemKeyStore));
		this.trustStore = SingletonSupplier.of(() -> createKeyStore("trust", pemTrustStore));
	}

	@Override
	public KeyStore getKeyStore() {
		return this.keyStore.get();
	}

	@Override
	public String getKeyStorePassword() {
		return null;
	}

	@Override
	public KeyStore getTrustStore() {
		return this.trustStore.get();
	}

	private static KeyStore createKeyStore(String name, PemSslStore pemSslStore) {
		if (pemSslStore == null) {
			return null;
		}
		try {
			List<X509Certificate> certificates = pemSslStore.certificates();
			Assert.state(!ObjectUtils.isEmpty(certificates), "Certificates must not be empty");
			String alias = (pemSslStore.alias() != null) ? pemSslStore.alias() : DEFAULT_ALIAS;
			KeyStore store = createKeyStore(pemSslStore.type());
			PrivateKey privateKey = pemSslStore.privateKey();
			if (privateKey != null) {
				addPrivateKey(store, privateKey, alias, pemSslStore.password(), certificates);
			}
			else {
				addCertificates(store, certificates, alias);
			}
			return store;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
		}
	}

	private static KeyStore createKeyStore(String type)
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore store = KeyStore.getInstance(StringUtils.hasText(type) ? type : KeyStore.getDefaultType());
		store.load(null);
		return store;
	}

	private static void addPrivateKey(KeyStore keyStore, PrivateKey privateKey, String alias, String keyPassword,
			List<X509Certificate> certificateChain) throws KeyStoreException {
		keyStore.setKeyEntry(alias, privateKey, (keyPassword != null) ? keyPassword.toCharArray() : null,
				certificateChain.toArray(X509Certificate[]::new));
	}

	private static void addCertificates(KeyStore keyStore, List<X509Certificate> certificates, String alias)
			throws KeyStoreException {
		for (int index = 0; index < certificates.size(); index++) {
			String entryAlias = alias + ((certificates.size() == 1) ? "" : "-" + index);
			X509Certificate certificate = certificates.get(index);
			keyStore.setCertificateEntry(entryAlias, certificate);
		}
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		KeyStore keyStore = this.keyStore.get();
		KeyStore trustStore = this.trustStore.get();
		creator.append("keyStore.type", (keyStore != null) ? keyStore.getType() : "none");
		creator.append("keyStorePassword", null);
		creator.append("trustStore.type", (trustStore != null) ? trustStore.getType() : "none");
		return creator.toString();
	}

}
