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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by PEM-encoded certificates and private keys.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public class PemSslStoreBundle implements SslStoreBundle {

	private static final String DEFAULT_KEY_ALIAS = "ssl";

	private final PemSslStoreDetails keyStoreDetails;

	private final PemSslStoreDetails trustStoreDetails;

	private final String keyAlias;

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
		this.keyAlias = keyAlias;
		this.keyStoreDetails = keyStoreDetails;
		this.trustStoreDetails = trustStoreDetails;
	}

	@Override
	public KeyStore getKeyStore() {
		return createKeyStore("key", this.keyStoreDetails);
	}

	@Override
	public String getKeyStorePassword() {
		return null;
	}

	@Override
	public KeyStore getTrustStore() {
		return createKeyStore("trust", this.trustStoreDetails);
	}

	private KeyStore createKeyStore(String name, PemSslStoreDetails details) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		try {
			Assert.notNull(details.certificate(), "CertificateContent must not be null");
			String type = (!StringUtils.hasText(details.type())) ? KeyStore.getDefaultType() : details.type();
			KeyStore store = KeyStore.getInstance(type);
			store.load(null);
			String certificateContent = PemContent.load(details.certificate());
			String privateKeyContent = PemContent.load(details.privateKey());
			X509Certificate[] certificates = PemCertificateParser.parse(certificateContent);
			PrivateKey privateKey = PemPrivateKeyParser.parse(privateKeyContent);
			addCertificates(store, certificates, privateKey);
			return store;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
		}
	}

	private void addCertificates(KeyStore keyStore, X509Certificate[] certificates, PrivateKey privateKey)
			throws KeyStoreException {
		String alias = (this.keyAlias != null) ? this.keyAlias : DEFAULT_KEY_ALIAS;
		if (privateKey != null) {
			keyStore.setKeyEntry(alias, privateKey, null, certificates);
		}
		else {
			for (int index = 0; index < certificates.length; index++) {
				keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
			}
		}
	}

}
