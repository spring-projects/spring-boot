/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link SslManagerBundle}.
 *
 * @author Scott Frederick
 * @see SslManagerBundle#from(SslStoreBundle, SslBundleKey)
 */
class DefaultSslManagerBundle implements SslManagerBundle {

	private final SslStoreBundle storeBundle;

	private final SslBundleKey key;

	DefaultSslManagerBundle(@Nullable SslStoreBundle storeBundle, @Nullable SslBundleKey key) {
		this.storeBundle = (storeBundle != null) ? storeBundle : SslStoreBundle.NONE;
		this.key = (key != null) ? key : SslBundleKey.NONE;
	}

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

	protected KeyManagerFactory getKeyManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
		return KeyManagerFactory.getInstance(algorithm);
	}

	protected TrustManagerFactory getTrustManagerFactoryInstance(String algorithm) throws NoSuchAlgorithmException {
		return TrustManagerFactory.getInstance(algorithm);
	}

}
