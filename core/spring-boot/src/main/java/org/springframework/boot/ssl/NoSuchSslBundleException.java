/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import org.jspecify.annotations.Nullable;

/**
 * Exception indicating that an {@link SslBundle} was referenced with a name that does not
 * match any registered bundle.
 *
 * @author Scott Frederick
 * @since 3.1.0
 */
public class NoSuchSslBundleException extends RuntimeException {

	private final String bundleName;

	/**
	 * Create a new {@code SslBundleNotFoundException} instance.
	 * @param bundleName the name of the bundle that could not be found
	 * @param message the exception message
	 */
	public NoSuchSslBundleException(String bundleName, String message) {
		this(bundleName, message, null);
	}

	/**
	 * Create a new {@code SslBundleNotFoundException} instance.
	 * @param bundleName the name of the bundle that could not be found
	 * @param message the exception message
	 * @param cause the exception cause
	 */
	public NoSuchSslBundleException(String bundleName, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.bundleName = bundleName;
	}

	/**
	 * Return the name of the bundle that was not found.
	 * @return the bundle name
	 */
	public String getBundleName() {
		return this.bundleName;
	}

}
