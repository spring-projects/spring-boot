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

import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link PemSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param certificate the certificate content (either the PEM content itself or something
 * that can be loaded by {@link ResourceUtils#getURL})
 * @param privateKey the private key content (either the PEM content itself or something
 * that can be loaded by {@link ResourceUtils#getURL})
 * @param privateKeyPassword a password used to decrypt an encrypted private key
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public record PemSslStoreDetails(String type, String certificate, String privateKey, String privateKeyPassword) {

	public PemSslStoreDetails(String type, String certificate, String privateKey) {
		this(type, certificate, privateKey, null);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new private key.
	 * @param privateKey the new private key
	 * @return a new {@link PemSslStoreDetails} instance
	 */
	public PemSslStoreDetails withPrivateKey(String privateKey) {
		return new PemSslStoreDetails(this.type, this.certificate, privateKey, this.privateKeyPassword);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new private key password.
	 * @param password the new private key password
	 * @return a new {@link PemSslStoreDetails} instance
	 */
	public PemSslStoreDetails withPrivateKeyPassword(String password) {
		return new PemSslStoreDetails(this.type, this.certificate, this.privateKey, password);
	}

	boolean isEmpty() {
		return isEmpty(this.type) && isEmpty(this.certificate) && isEmpty(this.privateKey);
	}

	private boolean isEmpty(String value) {
		return !StringUtils.hasText(value);
	}

	/**
	 * Factory method to create a new {@link PemSslStoreDetails} instance for the given
	 * certificate.
	 * @param certificate the certificate
	 * @return a new {@link PemSslStoreDetails} instance.
	 */
	public static PemSslStoreDetails forCertificate(String certificate) {
		return new PemSslStoreDetails(null, certificate, null);
	}

}
