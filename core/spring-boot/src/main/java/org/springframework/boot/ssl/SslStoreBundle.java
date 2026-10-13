/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.security.KeyStore;

import org.jspecify.annotations.Nullable;

import org.springframework.core.style.ToStringCreator;

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
	@Nullable KeyStore getKeyStore();

	/**
	 * Return the password for the key in the key store or {@code null}.
	 * @return the key password
	 */
	@Nullable String getKeyStorePassword();

	/**
	 * Return a trust store generated from the trust material or {@code null}.
	 * @return the trust store
	 */
	@Nullable KeyStore getTrustStore();

	/**
	 * Factory method to create a new {@link SslStoreBundle} instance.
	 * @param keyStore the key store or {@code null}
	 * @param keyStorePassword the key store password or {@code null}
	 * @param trustStore the trust store or {@code null}
	 * @return a new {@link SslStoreBundle} instance
	 */
	static SslStoreBundle of(@Nullable KeyStore keyStore, @Nullable String keyStorePassword,
			@Nullable KeyStore trustStore) {
		return new SslStoreBundle() {

			@Override
			public @Nullable KeyStore getKeyStore() {
				return keyStore;
			}

			@Override
			public @Nullable KeyStore getTrustStore() {
				return trustStore;
			}

			@Override
			public @Nullable String getKeyStorePassword() {
				return keyStorePassword;
			}

			@Override
			public String toString() {
				ToStringCreator creator = new ToStringCreator(this);
				creator.append("keyStore.type", (keyStore != null) ? keyStore.getType() : "none");
				creator.append("keyStorePassword", (keyStorePassword != null) ? "******" : null);
				creator.append("trustStore.type", (trustStore != null) ? trustStore.getType() : "none");
				return creator.toString();
			}

		};
	}

}
