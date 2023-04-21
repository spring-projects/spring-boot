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

package org.springframework.boot.ssl;

import java.security.KeyStore;

/**
 * A bundle of key and trust stores that can be used to establish an SSL connection.
 *
 * @author Scott Frederick
 * @since 3.1.0
 * @see SslBundle#getStores()
 */
public interface SslStoreBundle {

	/**
	 * {@link SslStoreBundle} that returns {@code null} for each method.
	 */
	SslStoreBundle NONE = of(null, null, null);

	/**
	 * Return a key store generated from the trust material or {@code null}.
	 * @return the key store
	 */
	KeyStore getKeyStore();

	/**
	 * Return the password for the key in the key store or {@code null}.
	 * @return the key password
	 */
	String getKeyStorePassword();

	/**
	 * Return a trust store generated from the trust material or {@code null}.
	 * @return the trust store
	 */
	KeyStore getTrustStore();

	/**
	 * Factory method to create a new {@link SslStoreBundle} instance.
	 * @param keyStore the key store or {@code null}
	 * @param keyStorePassword the key store password or {@code null}
	 * @param trustStore the trust store or {@code null}
	 * @return a new {@link SslStoreBundle} instance
	 */
	static SslStoreBundle of(KeyStore keyStore, String keyStorePassword, KeyStore trustStore) {
		return new SslStoreBundle() {

			@Override
			public KeyStore getKeyStore() {
				return keyStore;
			}

			@Override
			public KeyStore getTrustStore() {
				return trustStore;
			}

			@Override
			public String getKeyStorePassword() {
				return keyStorePassword;
			}

		};
	}

}
