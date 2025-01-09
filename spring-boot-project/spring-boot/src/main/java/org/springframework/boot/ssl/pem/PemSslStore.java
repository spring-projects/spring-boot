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

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;

/**
 * An individual trust or key store that has been loaded from PEM content.
 *
 * @author Phillip Webb
 * @since 3.2.0
 * @see PemSslStoreDetails
 * @see PemContent
 */
public interface PemSslStore {

	/**
	 * The key store type, for example {@code JKS} or {@code PKCS11}. A {@code null} value
	 * will use {@link KeyStore#getDefaultType()}).
	 * @return the key store type
	 */
	String type();

	/**
	 * The alias used when setting entries in the {@link KeyStore}.
	 * @return the alias
	 */
	String alias();

	/**
	 * The password used when
	 * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
	 * setting key entries} in the {@link KeyStore}.
	 * @return the password
	 */
	String password();

	/**
	 * The certificates for this store. When a {@link #privateKey() private key} is
	 * present the returned value is treated as a certificate chain, otherwise it is
	 * treated a list of certificates that should all be registered.
	 * @return the X509 certificates
	 */
	List<X509Certificate> certificates();

	/**
	 * The private key for this store or {@code null}.
	 * @return the private key
	 */
	PrivateKey privateKey();

	/**
	 * Return a new {@link PemSslStore} instance with a new alias.
	 * @param alias the new alias
	 * @return a new {@link PemSslStore} instance
	 */
	default PemSslStore withAlias(String alias) {
		return of(type(), alias, password(), certificates(), privateKey());
	}

	/**
	 * Return a new {@link PemSslStore} instance with a new password.
	 * @param password the new password
	 * @return a new {@link PemSslStore} instance
	 */
	default PemSslStore withPassword(String password) {
		return of(type(), alias(), password, certificates(), privateKey());
	}

	/**
	 * Return a {@link PemSslStore} instance loaded using the given
	 * {@link PemSslStoreDetails}.
	 * @param details the PEM store details
	 * @return a loaded {@link PemSslStore} or {@code null}.
	 */
	static PemSslStore load(PemSslStoreDetails details) {
		return load(details, ApplicationResourceLoader.get());
	}

	/**
	 * Return a {@link PemSslStore} instance loaded using the given
	 * {@link PemSslStoreDetails}.
	 * @param details the PEM store details
	 * @param resourceLoader the resource loader used to load content
	 * @return a loaded {@link PemSslStore} or {@code null}.
	 * @since 3.3.5
	 */
	static PemSslStore load(PemSslStoreDetails details, ResourceLoader resourceLoader) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		return new LoadedPemSslStore(details, resourceLoader);
	}

	/**
	 * Factory method that can be used to create a new {@link PemSslStore} with the given
	 * values.
	 * @param type the key store type
	 * @param certificates the certificates for this store
	 * @param privateKey the private key
	 * @return a new {@link PemSslStore} instance
	 */
	static PemSslStore of(String type, List<X509Certificate> certificates, PrivateKey privateKey) {
		return of(type, null, null, certificates, privateKey);
	}

	/**
	 * Factory method that can be used to create a new {@link PemSslStore} with the given
	 * values.
	 * @param certificates the certificates for this store
	 * @param privateKey the private key
	 * @return a new {@link PemSslStore} instance
	 */
	static PemSslStore of(List<X509Certificate> certificates, PrivateKey privateKey) {
		return of(null, null, null, certificates, privateKey);
	}

	/**
	 * Factory method that can be used to create a new {@link PemSslStore} with the given
	 * values.
	 * @param type the key store type
	 * @param alias the alias used when setting entries in the {@link KeyStore}
	 * @param password the password used
	 * {@link KeyStore#setKeyEntry(String, java.security.Key, char[], java.security.cert.Certificate[])
	 * setting key entries} in the {@link KeyStore}
	 * @param certificates the certificates for this store
	 * @param privateKey the private key
	 * @return a new {@link PemSslStore} instance
	 */
	static PemSslStore of(String type, String alias, String password, List<X509Certificate> certificates,
			PrivateKey privateKey) {
		Assert.notEmpty(certificates, "'certificates' must not be empty");
		return new PemSslStore() {

			@Override
			public String type() {
				return type;
			}

			@Override
			public String alias() {
				return alias;
			}

			@Override
			public String password() {
				return password;
			}

			@Override
			public List<X509Certificate> certificates() {
				return certificates;
			}

			@Override
			public PrivateKey privateKey() {
				return privateKey;
			}

		};
	}

}
