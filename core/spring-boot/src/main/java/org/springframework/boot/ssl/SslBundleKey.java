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
	 * @param alias the alias of the server and client key
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
			public @Nullable String getServerAlias() {
				return alias;
			}

			@Override
			public @Nullable String getClientAlias() {
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
				return serverAlias; // returns server alias for backwards compatibility
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

	/**
	 * Return the password that should be used to access the key or {@code null} if no
	 * password is required.
	 * @return the key password
	 */
	@Nullable String getPassword();

	/**
	 * Return the alias of the key or {@code null} if the key has no alias.
	 * @return the key alias
	 * @deprecated in favor of {@link #getServerAlias()} and {@link #getClientAlias()}
	 */
	@Deprecated(since = "4.0.0")
	@Nullable String getAlias();

	/**
	 * Return the alias of the key to use for server connections or {@code null} if the key has no alias.
	 * @return the server key alias
	 */
	@Nullable String getServerAlias();

	/**
	 * Return the alias of the key to use for client connections or {@code null} if the key has no alias.
	 * @return the client key alias
	 */
	@Nullable String getClientAlias();

	/**
	 * Assert that the alias is contained in the given keystore.
	 * @param keyStore the keystore to check
	 * @deprecated in favor of {@link #assertContainsServerAlias(KeyStore)} and {@link #assertContainsClientAlias(KeyStore)}
	 */
	@Deprecated(since = "4.0.0")
	default void assertContainsAlias(@Nullable KeyStore keyStore) {
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
	 * Assert that the server alias is contained in the given keystore.
	 * @param keyStore the keystore to check
	 */
	default void assertContainsServerAlias(@Nullable KeyStore keyStore) {
		String alias = getServerAlias();
		if (StringUtils.hasLength(alias) && keyStore != null) {
			try {
				Assert.state(keyStore.containsAlias(alias),
						() -> String.format("Keystore does not contain server alias '%s'", alias));
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException(
						String.format("Could not determine if keystore contains server alias '%s'", alias), ex);
			}
		}
	}

	/**
	 * Assert that the client alias is contained in the given keystore.
	 * @param keyStore the keystore to check
	 */
	default void assertContainsClientAlias(@Nullable KeyStore keyStore) {
		String alias = getClientAlias();
		if (StringUtils.hasLength(alias) && keyStore != null) {
			try {
				Assert.state(keyStore.containsAlias(alias),
						() -> String.format("Keystore does not contain client alias '%s'", alias));
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException(
						String.format("Could not determine if keystore contains client alias '%s'", alias), ex);
			}
		}
	}

}
