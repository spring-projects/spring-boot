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

package org.springframework.boot.autoconfigure.ssl;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.jks.JksSslStoreBundle;

/**
 * {@link SslBundleProperties} for Java keystores.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 * @see JksSslStoreBundle
 */
public class JksSslBundleProperties extends SslBundleProperties {

	/**
	 * Keystore properties.
	 */
	private final Store keystore = new Store();

	/**
	 * Truststore properties.
	 */
	private final Store truststore = new Store();

	/**
	 * Key properties.
	 */
	private final JksKey key = new JksKey();

	public Store getKeystore() {
		return this.keystore;
	}

	public Store getTruststore() {
		return this.truststore;
	}

	@Override
	public JksKey getKey() {
		return this.key;
	}

	/**
	 * Store properties.
	 */
	public static class Store {

		/**
		 * Type of the store to create, e.g. JKS.
		 */
		private @Nullable String type;

		/**
		 * Provider for the store.
		 */
		private @Nullable String provider;

		/**
		 * Location of the resource containing the store content.
		 */
		private @Nullable String location;

		/**
		 * Password used to access the store.
		 */
		private @Nullable String password;

		public @Nullable String getType() {
			return this.type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		public @Nullable String getProvider() {
			return this.provider;
		}

		public void setProvider(@Nullable String provider) {
			this.provider = provider;
		}

		public @Nullable String getLocation() {
			return this.location;
		}

		public void setLocation(@Nullable String location) {
			this.location = location;
		}

		public @Nullable String getPassword() {
			return this.password;
		}

		public void setPassword(@Nullable String password) {
			this.password = password;
		}

	}

	public static class JksKey extends Key {
		/**
		 * The alias that identifies the server key in the key store.
		 */
		private @Nullable String serverAlias;

		/**
		 * The alias that identifies the client key in the key store.
		 */
		private @Nullable String clientAlias;

		public @Nullable String getServerAlias() {
			return this.serverAlias;
		}

		public void setServerAlias(@Nullable String serverAlias) {
			this.serverAlias = serverAlias;
		}

		public @Nullable String getClientAlias() {
			return this.clientAlias;
		}

		public void setClientAlias(@Nullable String clientAlias) {
			this.clientAlias = clientAlias;
		}

		/**
		 * Alias that identifies the key in the key store. Deprecated in favor of {@link #getServerAlias()}
		 * @return the server key alias
		 * @deprecated in favor of {@link #getServerAlias()}
		 */
		@Override
		@Deprecated(since = "4.0.0", forRemoval = true)
		public @Nullable String getAlias() {
			return super.getAlias();
		}

		/**
		 * Alias that identifies the key in the key store. Deprecated in favor of {@link #setServerAlias(String)}
		 * @param alias the server key alias to set
		 * @deprecated in favor of {@link #setServerAlias(String)}
		 */
		@Override
		@Deprecated(since = "4.0.0", forRemoval = true)
		public void setAlias(@Nullable String alias) {
			super.setAlias(alias);
		}
	}
}
