/*
 * Copyright 2012-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.style.ToStringCreator;
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
	@Nullable String getPassword();

	/**
	 * Return the alias of the key or {@code null} if the key has no alias.
	 * @return the key alias
	 */
	@Nullable String getAlias();

	/**
	 * Return the alias of the server key or {@code null} if the server key has no alias.
	 * @return the server key alias
	 */
	default @Nullable String getServerAlias() {
		return this.getAlias();
	}

	/**
	 * Return the alias of the client key or {@code null} if the client key has no alias.
	 * @return the client key alias
	 */
	default @Nullable String getClientAlias() {
		return null;
	}

	/**
	 * Assert that the alias is contained in the given keystore.
	 * @param keyStore the keystore to check
	 */
	default void assertContainsAlias(@Nullable KeyStore keyStore) {
		String alias = getAlias();

		if (keyStore != null && StringUtils.hasLength(alias)) {
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

	default void assertContainsAlias(@Nullable KeyStore keyStore, @Nullable String alias) {
		if (keyStore == null) {
			return;
		}

		try {
			if (StringUtils.hasLength(alias)) {
				Assert.state(keyStore.containsAlias(alias),
						() -> String.format("Keystore does not contain alias '%s'", alias));
			}
		}
		catch (KeyStoreException ex) {
			throw new IllegalStateException(
					String.format("Could not determine if keystore contains alias '%s'", alias), ex);
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
	static SslBundleKey of(@Nullable String password, @Nullable String alias) {
		return new SslBundleKey() {

			@Override
			public @Nullable String getPassword() {
				return password;
			}

			@Override
			public @Nullable String getAlias() {
				return alias;
			}

			@Override
			public String toString() {
				ToStringCreator creator = new ToStringCreator(this);
				creator.append("alias", alias);
				creator.append("password", (password != null) ? "******" : null);
				return creator.toString();
			}

		};
	}

	/**
	 * Factory method to create a new {@link SslBundleKey} instance.
	 * @param password the password used to access the key
	 * @param serverAlias the alias of the server key
	 * @param clientAlias the alias of the client key
	 * @return a new {@link SslBundleKey} instance
	 */
	static SslBundleKey of(@Nullable String password, @Nullable String serverAlias, @Nullable String clientAlias) {
		return new SslBundleKey() {

			@Override
			public @Nullable String getPassword() {
				return password;
			}

			@Override
			public @Nullable String getAlias() {
				return serverAlias;
			}

			@Override
			public @Nullable String getServerAlias() {
				return serverAlias;
			}

			@Override
			public @Nullable String getClientAlias() {
				return clientAlias;
			}

			@Override
			public String toString() {
				ToStringCreator creator = new ToStringCreator(this);
				creator.append("serverAlias", serverAlias);
				creator.append("clientAlias", clientAlias);
				creator.append("password", (password != null) ? "******" : null);
				return creator.toString();
			}

		};
	}
}
