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

package org.springframework.boot.autoconfigure.ssl;

import java.util.Set;

import org.springframework.boot.ssl.SslBundle;

/**
 * Base class for SSL Bundle properties.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 * @see SslBundle
 */
public abstract class SslBundleProperties {

	/**
	 * Key details for the bundle.
	 */
	private final Key key = new Key();

	/**
	 * Options for the SLL connection.
	 */
	private final Options options = new Options();

	/**
	 * SSL Protocol to use.
	 */
	private String protocol = SslBundle.DEFAULT_PROTOCOL;

	public Key getKey() {
		return this.key;
	}

	public Options getOptions() {
		return this.options;
	}

	public String getProtocol() {
		return this.protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public static class Options {

		/**
		 * Supported SSL ciphers.
		 */
		private Set<String> ciphers;

		/**
		 * Enabled SSL protocols.
		 */
		private Set<String> enabledProtocols;

		public Set<String> getCiphers() {
			return this.ciphers;
		}

		public void setCiphers(Set<String> ciphers) {
			this.ciphers = ciphers;
		}

		public Set<String> getEnabledProtocols() {
			return this.enabledProtocols;
		}

		public void setEnabledProtocols(Set<String> enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

	}

	public static class Key {

		/**
		 * The password used to access the key in the key store.
		 */
		private String password;

		/**
		 * The alias that identifies the key in the key store.
		 */
		private String alias;

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

		public String getAlias() {
			return this.alias;
		}

		public void setAlias(String alias) {
			this.alias = alias;
		}

	}

}
