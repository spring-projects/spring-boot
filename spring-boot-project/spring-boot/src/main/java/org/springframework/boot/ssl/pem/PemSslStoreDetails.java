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

package org.springframework.boot.ssl.pem;

import java.security.KeyStore;

import org.springframework.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link PemSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param alias the alias used when setting entries in the {@link KeyStore}
 * @param password the password used
 * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
 * setting key entries} in the {@link KeyStore}
 * @param certificates the certificates content (either the PEM content itself or a
 * reference to the resource to load). When a {@link #privateKey() private key} is present
 * this value is treated as a certificate chain, otherwise it is treated a list of
 * certificates that should all be registered.
 * @param privateKey the private key content (either the PEM content itself or a reference
 * to the resource to load)
 * @param privateKeyPassword a password used to decrypt an encrypted private key
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 * @see PemSslStore#load(PemSslStoreDetails)
 */
public record PemSslStoreDetails(String type, String alias, String password, String certificates, String privateKey,
		String privateKeyPassword) {

	/**
	 * Create a new {@link PemSslStoreDetails} instance.
	 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
	 * {@code null} value will use {@link KeyStore#getDefaultType()}).
	 * @param alias the alias used when setting entries in the {@link KeyStore}
	 * @param password the password used
	 * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
	 * setting key entries} in the {@link KeyStore}
	 * @param certificates the certificate content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @param privateKey the private key content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @param privateKeyPassword a password used to decrypt an encrypted private key
	 * @since 3.2.0
	 */
	public PemSslStoreDetails {
	}

	/**
	 * Create a new {@link PemSslStoreDetails} instance.
	 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
	 * {@code null} value will use {@link KeyStore#getDefaultType()}).
	 * @param certificate the certificate content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @param privateKey the private key content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @param privateKeyPassword a password used to decrypt an encrypted private key
	 */
	public PemSslStoreDetails(String type, String certificate, String privateKey, String privateKeyPassword) {
		this(type, null, null, certificate, privateKey, privateKeyPassword);
	}

	/**
	 * Create a new {@link PemSslStoreDetails} instance.
	 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
	 * {@code null} value will use {@link KeyStore#getDefaultType()}).
	 * @param certificate the certificate content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @param privateKey the private key content (either the PEM content itself or a
	 * reference to the resource to load)
	 */
	public PemSslStoreDetails(String type, String certificate, String privateKey) {
		this(type, certificate, privateKey, null);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new alias.
	 * @param alias the new alias
	 * @return a new {@link PemSslStoreDetails} instance
	 * @since 3.2.0
	 */
	public PemSslStoreDetails withAlias(String alias) {
		return new PemSslStoreDetails(this.type, alias, this.password, this.certificates, this.privateKey,
				this.privateKeyPassword);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new password.
	 * @param password the new password
	 * @return a new {@link PemSslStoreDetails} instance
	 * @since 3.2.0
	 */
	public PemSslStoreDetails withPassword(String password) {
		return new PemSslStoreDetails(this.type, this.alias, password, this.certificates, this.privateKey,
				this.privateKeyPassword);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new private key.
	 * @param privateKey the new private key
	 * @return a new {@link PemSslStoreDetails} instance
	 */
	public PemSslStoreDetails withPrivateKey(String privateKey) {
		return new PemSslStoreDetails(this.type, this.alias, this.password, this.certificates, privateKey,
				this.privateKeyPassword);
	}

	/**
	 * Return a new {@link PemSslStoreDetails} instance with a new private key password.
	 * @param privateKeyPassword the new private key password
	 * @return a new {@link PemSslStoreDetails} instance
	 */
	public PemSslStoreDetails withPrivateKeyPassword(String privateKeyPassword) {
		return new PemSslStoreDetails(this.type, this.alias, this.password, this.certificates, this.privateKey,
				privateKeyPassword);
	}

	boolean isEmpty() {
		return isEmpty(this.type) && isEmpty(this.certificates) && isEmpty(this.privateKey);
	}

	private boolean isEmpty(String value) {
		return !StringUtils.hasText(value);
	}

	/**
	 * Factory method to create a new {@link PemSslStoreDetails} instance for the given
	 * certificate. <b>Note:</b> This method doesn't actually check if the provided value
	 * only contains a single certificate. It is functionally equivalent to
	 * {@link #forCertificates(String)}.
	 * @param certificate the certificate content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @return a new {@link PemSslStoreDetails} instance.
	 */
	public static PemSslStoreDetails forCertificate(String certificate) {
		return forCertificates(certificate);
	}

	/**
	 * Factory method to create a new {@link PemSslStoreDetails} instance for the given
	 * certificates.
	 * @param certificates the certificates content (either the PEM content itself or a
	 * reference to the resource to load)
	 * @return a new {@link PemSslStoreDetails} instance.
	 * @since 3.2.0
	 */
	public static PemSslStoreDetails forCertificates(String certificates) {
		return new PemSslStoreDetails(null, certificates, null);
	}

}
