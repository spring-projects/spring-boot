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
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Default implementation of {@link SslManagerBundle}.
 *
 * @author Scott Frederick
 * @see SslManagerBundle#from(SslStoreBundle, SslBundleKey)
 */
class DefaultSslManagerBundle implements SslManagerBundle {

	private final SslStoreBundle storeBundle;

	private final SslBundleKey key;

	/**
	 * Constructs a new DefaultSslManagerBundle with the specified SslStoreBundle and
	 * SslBundleKey.
	 * @param storeBundle the SslStoreBundle to be used, or SslStoreBundle.NONE if null
	 * @param key the SslBundleKey to be used, or SslBundleKey.NONE if null
	 */
	DefaultSslManagerBundle(SslStoreBundle storeBundle, SslBundleKey key) {
		this.storeBundle = (storeBundle != null) ? storeBundle : SslStoreBundle.NONE;
		this.key = (key != null) ? key : SslBundleKey.NONE;
	}

	/**
	 * Returns the KeyManagerFactory for this SSL manager bundle.
	 * @return the KeyManagerFactory
	 * @throws IllegalStateException if the KeyManagerFactory could not be loaded
	 */
	@Override
	public KeyManagerFactory getKeyManagerFactory() {
		try {
			KeyStore store = this.storeBundle.getKeyStore();
			this.key.assertContainsAlias(store);
			String alias = this.key.getAlias();
			String algorithm = KeyManagerFactory.getDefaultAlgorithm();
			KeyManagerFactory factory = getKeyManagerFactoryInstance(algorithm);
			factory = (alias != null) ? new AliasKeyManagerFactory(factory, alias, algorithm) : factory;
			String password = this.key.getPassword();
			password = (password != null) ? password : this.storeBundle.getKeyStorePassword();
			factory.init(store, (password != null) ? password.toCharArray() : null);
			return factory;
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load key manager factory: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns the TrustManagerFactory instance for the trust store in the SSL manager
	 * bundle.
	 * @return the TrustManagerFactory instance
	 * @throws IllegalStateException if the trust manager factory could not be loaded
	 */
	@Override
	public TrustManagerFactory getTrustManagerFactory() {
		try {
			KeyStore store = this.storeBundle.getTrustStore();
			String algorithm = TrustManagerFactory.getDefaultAlgorithm();
			TrustManagerFactory factory = getTrustManagerFactoryInstance(algorithm);
			factory.init(store);
			return factory;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load trust manager factory: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Returns an instance of KeyManagerFactory for the specified algorithm.
	 * @param algorithm the algorithm name
	 * @return the KeyManagerFactory instance
	 * @throws NoSuchAlgorithmException if the specified algorithm is not available
	 */
	protected KeyManagerFactory getKeyManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
		return KeyManagerFactory.getInstance(algorithm);
	}

	/**
	 * Returns a TrustManagerFactory instance for the specified algorithm.
	 * @param algorithm the algorithm name
	 * @return the TrustManagerFactory instance
	 * @throws NoSuchAlgorithmException if the specified algorithm is not available
	 */
	protected TrustManagerFactory getTrustManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
		return TrustManagerFactory.getInstance(algorithm);
	}

}
