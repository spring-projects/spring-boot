/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

/**
 * Interface that can be used to register an {@link SslBundle} for a given name.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public interface SslBundleRegistry {

	/**
	 * Register a named {@link SslBundle}.
	 * @param name the bundle name
	 * @param bundle the bundle
	 */
	void registerBundle(String name, SslBundle bundle);

	/**
	 * Updates an {@link SslBundle}.
	 * @param name the bundle name
	 * @param updatedBundle the updated bundle
	 * @throws NoSuchSslBundleException if the bundle cannot be found
	 * @since 3.2.0
	 */
	void updateBundle(String name, SslBundle updatedBundle) throws NoSuchSslBundleException;

}
