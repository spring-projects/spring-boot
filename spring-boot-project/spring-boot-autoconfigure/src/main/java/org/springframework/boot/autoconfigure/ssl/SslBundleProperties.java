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
	 * Options for the SSL connection.
	 */
	private final Options options = new Options();

	/**
	 * SSL Protocol to use.
	 */
	private String protocol = SslBundle.DEFAULT_PROTOCOL;

	/**
	 * Whether to reload the SSL bundle.
	 */
	private boolean reloadOnUpdate;

	/**
     * Returns the key associated with this SslBundleProperties object.
     *
     * @return the key associated with this SslBundleProperties object
     */
    public Key getKey() {
		return this.key;
	}

	/**
     * Returns the options associated with the SslBundleProperties.
     *
     * @return the options associated with the SslBundleProperties
     */
    public Options getOptions() {
		return this.options;
	}

	/**
     * Returns the protocol used by the SSL bundle.
     *
     * @return the protocol used by the SSL bundle
     */
    public String getProtocol() {
		return this.protocol;
	}

	/**
     * Sets the protocol for the SSL bundle properties.
     * 
     * @param protocol the protocol to be set
     */
    public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	/**
     * Returns a boolean value indicating whether the bundle should be reloaded on update.
     *
     * @return true if the bundle should be reloaded on update, false otherwise
     */
    public boolean isReloadOnUpdate() {
		return this.reloadOnUpdate;
	}

	/**
     * Sets the flag indicating whether the bundle should be reloaded on update.
     * 
     * @param reloadOnUpdate true if the bundle should be reloaded on update, false otherwise
     */
    public void setReloadOnUpdate(boolean reloadOnUpdate) {
		this.reloadOnUpdate = reloadOnUpdate;
	}

	/**
     * Options class.
     */
    public static class Options {

		/**
		 * Supported SSL ciphers.
		 */
		private Set<String> ciphers;

		/**
		 * Enabled SSL protocols.
		 */
		private Set<String> enabledProtocols;

		/**
         * Returns the set of available ciphers.
         *
         * @return the set of available ciphers
         */
        public Set<String> getCiphers() {
			return this.ciphers;
		}

		/**
         * Sets the ciphers for the Options class.
         * 
         * @param ciphers the set of ciphers to be set
         */
        public void setCiphers(Set<String> ciphers) {
			this.ciphers = ciphers;
		}

		/**
         * Returns the set of enabled protocols.
         *
         * @return the set of enabled protocols
         */
        public Set<String> getEnabledProtocols() {
			return this.enabledProtocols;
		}

		/**
         * Sets the enabled protocols for the Options class.
         * 
         * @param enabledProtocols the set of enabled protocols to be set
         */
        public void setEnabledProtocols(Set<String> enabledProtocols) {
			this.enabledProtocols = enabledProtocols;
		}

	}

	/**
     * Key class.
     */
    public static class Key {

		/**
		 * The password used to access the key in the key store.
		 */
		private String password;

		/**
		 * The alias that identifies the key in the key store.
		 */
		private String alias;

		/**
         * Returns the password associated with the Key object.
         *
         * @return the password as a String
         */
        public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the Key.
         * 
         * @param password the password to be set
         */
        public void setPassword(String password) {
			this.password = password;
		}

		/**
         * Returns the alias of the Key.
         *
         * @return the alias of the Key
         */
        public String getAlias() {
			return this.alias;
		}

		/**
         * Sets the alias for the Key.
         * 
         * @param alias the alias to be set for the Key
         */
        public void setAlias(String alias) {
			this.alias = alias;
		}

	}

}
