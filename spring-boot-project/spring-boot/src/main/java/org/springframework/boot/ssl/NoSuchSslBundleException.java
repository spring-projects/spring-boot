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
	public NoSuchSslBundleException(String bundleName, String message, Throwable cause) {
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
