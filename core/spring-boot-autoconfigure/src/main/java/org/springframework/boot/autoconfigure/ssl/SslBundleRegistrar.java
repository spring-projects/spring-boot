/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.ssl;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleRegistry;

/**
 * Interface to be implemented by types that register {@link SslBundle} instances with an
 * {@link SslBundleRegistry}.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
@FunctionalInterface
public interface SslBundleRegistrar {

	/**
	 * Callback method for registering {@link SslBundle}s with an
	 * {@link SslBundleRegistry}.
	 * @param registry the registry that accepts {@code SslBundle}s
	 */
	void registerBundles(SslBundleRegistry registry);

}
