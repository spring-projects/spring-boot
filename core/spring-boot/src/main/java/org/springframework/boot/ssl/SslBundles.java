/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A managed set of {@link SslBundle} instances that can be retrieved by name.
 *
 * @author Scott Frederick
 * @author Moritz Halbritter
 * @author Jonatan Ivanov
 * @since 3.1.0
 */
public interface SslBundles {

	/**
	 * Return an {@link SslBundle} with the provided name.
	 * @param name the bundle name
	 * @return the bundle
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 */
	SslBundle getBundle(String name) throws NoSuchSslBundleException;

	/**
	 * Add a handler that will be called each time the named bundle is updated.
	 * @param name the bundle name
	 * @param updateHandler the handler that should be called
	 * @throws NoSuchSslBundleException if a bundle with the provided name does not exist
	 * @since 3.2.0
	 */
	void addBundleUpdateHandler(String name, Consumer<SslBundle> updateHandler) throws NoSuchSslBundleException;

	/**
	 * Add a handler that will be called each time a bundle is registered. The handler
	 * will be called with the bundle name and the bundle.
	 * @param registerHandler the handler that should be called
	 * @since 3.5.0
	 */
	void addBundleRegisterHandler(BiConsumer<String, SslBundle> registerHandler);

	/**
	 * Return the names of all bundles managed by this instance.
	 * @return the bundle names
	 * @since 3.4.0
	 */
	List<String> getBundleNames();

}
