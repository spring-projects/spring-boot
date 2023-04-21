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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.util.Assert;

/**
 * A bundle of key and trust managers that can be used to establish an SSL connection.
 * Instances are usually created {@link #from(SslStoreBundle, SslBundleKey) from} an
 * {@link SslStoreBundle}.
 *
 * @author Scott Frederick
 * @since 3.1.0
 * @see SslStoreBundle
 * @see SslBundle#getManagers()
 */
public interface SslManagerBundle {

	/**
	 * Return the {@code KeyManager} instances used to establish identity.
	 * @return the key managers
	 */
	default KeyManager[] getKeyManagers() {
		return getKeyManagerFactory().getKeyManagers();
	}

	/**
	 * Return the {@code KeyManagerFactory} used to establish identity.
	 * @return the key manager factory
	 */
	KeyManagerFactory getKeyManagerFactory();

	/**
	 * Return the {@link TrustManager} instances used to establish trust.
	 * @return the trust managers
	 */
	default TrustManager[] getTrustManagers() {
		return getTrustManagerFactory().getTrustManagers();
	}

	/**
	 * Return the {@link TrustManagerFactory} used to establish trust.
	 * @return the trust manager factory
	 */
	TrustManagerFactory getTrustManagerFactory();

	/**
	 * Factory method to create a new {@link SSLContext} for the {@link #getKeyManagers()
	 * key managers} and {@link #getTrustManagers() trust managers} managed by this
	 * instance.
	 * @param protocol the standard name of the SSL protocol. See
	 * {@link SSLContext#getInstance(String)}
	 * @return a new {@link SSLContext} instance
	 */
	default SSLContext createSslContext(String protocol) {
		try {
			SSLContext sslContext = SSLContext.getInstance(protocol);
			sslContext.init(getKeyManagers(), getTrustManagers(), null);
			return sslContext;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load SSL context: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Factory method to create a new {@link SslManagerBundle} instance.
	 * @param keyManagerFactory the key manager factory
	 * @param trustManagerFactory the trust manager factory
	 * @return a new {@link SslManagerBundle} instance
	 */
	static SslManagerBundle of(KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
		Assert.notNull(keyManagerFactory, "KeyManagerFactory must not be null");
		Assert.notNull(trustManagerFactory, "TrustManagerFactory must not be null");
		return new SslManagerBundle() {

			@Override
			public KeyManagerFactory getKeyManagerFactory() {
				return keyManagerFactory;
			}

			@Override
			public TrustManagerFactory getTrustManagerFactory() {
				return trustManagerFactory;
			}

		};
	}

	/**
	 * Factory method to create a new {@link SslManagerBundle} backed by the given
	 * {@link SslBundle} and {@link SslBundleKey}.
	 * @param storeBundle the SSL store bundle
	 * @param key the key reference
	 * @return a new {@link SslManagerBundle} instance
	 */
	static SslManagerBundle from(SslStoreBundle storeBundle, SslBundleKey key) {
		return new DefaultSslManagerBundle(storeBundle, key);
	}

}
