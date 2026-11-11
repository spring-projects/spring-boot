/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl.jks;

import java.security.KeyStore;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link JksSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param provider the name of the key store provider
 * @param location the location of the key store file or {@code null} if using a
 * {@code PKCS11} hardware store
 * @param password the password used to unlock the store or {@code null}
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public record JksSslStoreDetails(@Nullable String type, @Nullable String provider, @Nullable String location,
		@Nullable String password) {

	/**
	 * Return a new {@link JksSslStoreDetails} instance with a new password.
	 * @param password the new password
	 * @return a new {@link JksSslStoreDetails} instance
	 */
	public JksSslStoreDetails withPassword(String password) {
		return new JksSslStoreDetails(this.type, this.provider, this.location, password);
	}

	boolean isEmpty() {
		return isEmpty(this.type) && isEmpty(this.provider) && isEmpty(this.location);
	}

	private boolean isEmpty(@Nullable String value) {
		return !StringUtils.hasText(value);
	}

	/**
	 * Factory method to create a new {@link JksSslStoreDetails} instance for the given
	 * location.
	 * @param location the location
	 * @return a new {@link JksSslStoreDetails} instance.
	 */
	public static JksSslStoreDetails forLocation(@Nullable String location) {
		return new JksSslStoreDetails(null, null, location, null);
	}

}
