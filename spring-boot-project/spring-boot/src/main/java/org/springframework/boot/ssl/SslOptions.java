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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.net.ssl.SSLEngine;

/**
 * Configuration options that should be applied when establishing an SSL connection.
 *
 * @author Scott Frederick
 * @since 3.1.0
 * @see SslBundle#getOptions()
 */
public interface SslOptions {

	/**
	 * {@link SslOptions} that returns {@code null} results.
	 */
	SslOptions NONE = of((Set<String>) null, (Set<String>) null);

	/**
	 * Return if any SSL options have been specified.
	 * @return {@code true} if SSL options have been specified
	 */
	default boolean isSpecified() {
		return (getCiphers() != null) && (getEnabledProtocols() != null);
	}

	/**
	 * Return the ciphers that can be used or an empty set. The cipher names in this set
	 * should be compatible with those supported by
	 * {@link SSLEngine#getSupportedCipherSuites()}.
	 * @return the ciphers that can be used or {@code null}
	 */
	String[] getCiphers();

	/**
	 * Return the protocols that should be enabled or an empty set. The protocols names in
	 * this set should be compatible with those supported by
	 * {@link SSLEngine#getSupportedProtocols()}.
	 * @return the protocols to enable or {@code null}
	 */
	String[] getEnabledProtocols();

	/**
	 * Factory method to create a new {@link SslOptions} instance.
	 * @param ciphers the ciphers
	 * @param enabledProtocols the enabled protocols
	 * @return a new {@link SslOptions} instance
	 */
	static SslOptions of(String[] ciphers, String[] enabledProtocols) {
		return new SslOptions() {

			@Override
			public String[] getCiphers() {
				return ciphers;
			}

			@Override
			public String[] getEnabledProtocols() {
				return enabledProtocols;
			}

		};
	}

	/**
	 * Factory method to create a new {@link SslOptions} instance.
	 * @param ciphers the ciphers
	 * @param enabledProtocols the enabled protocols
	 * @return a new {@link SslOptions} instance
	 */
	static SslOptions of(Set<String> ciphers, Set<String> enabledProtocols) {
		return of(toArray(ciphers), toArray(enabledProtocols));
	}

	/**
	 * Helper method that provides a null-safe way to convert a {@code String[]} to a
	 * {@link Collection} for client libraries to use.
	 * @param array the array to convert
	 * @return a collection or {@code null}
	 */
	static Set<String> asSet(String[] array) {
		return (array != null) ? Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(array))) : null;
	}

	private static String[] toArray(Collection<String> collection) {
		return (collection != null) ? collection.toArray(String[]::new) : null;
	}

}
