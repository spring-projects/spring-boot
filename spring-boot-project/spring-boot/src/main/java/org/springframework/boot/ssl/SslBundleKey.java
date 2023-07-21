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
import java.security.KeyStoreException;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A reference to a single key obtained via {@link SslBundle}.
 *
 * @author Phillip Webb
 * @since 3.1.0
 */
public interface SslBundleKey {

	/**
	 * {@link SslBundleKey} that returns no values.
	 */
	SslBundleKey NONE = of(null, null);

	/**
	 * Return the password that should be used to access the key or {@code null} if no
	 * password is required.
	 * @return the key password
	 */
	String getPassword();

	/**
	 * Return the alias of the key or {@code null} if the key has no alias.
	 * @return the key alias
	 */
	String getAlias();

	/**
	 * Assert that the alias is contained in the given keystore.
	 * @param keyStore the keystore to check
	 */
	default void assertContainsAlias(KeyStore keyStore) {
		String alias = getAlias();
		if (StringUtils.hasLength(alias) && keyStore != null) {
			try {
				Assert.state(keyStore.containsAlias(alias),
						() -> String.format("Keystore does not contain alias '%s'", alias));
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException(
						String.format("Could not determine if keystore contains alias '%s'", alias), ex);
			}
		}
	}

	/**
	 * Factory method to create a new {@link SslBundleKey} instance.
	 * @param password the password used to access the key
	 * @return a new {@link SslBundleKey} instance
	 */
	static SslBundleKey of(String password) {
		return of(password, null);
	}

	/**
	 * Factory method to create a new {@link SslBundleKey} instance.
	 * @param password the password used to access the key
	 * @param alias the alias of the key
	 * @return a new {@link SslBundleKey} instance
	 */
	static SslBundleKey of(String password, String alias) {
		return new SslBundleKey() {

			@Override
			public String getPassword() {
				return password;
			}

			@Override
			public String getAlias() {
				return alias;
			}

		};
	}

}
