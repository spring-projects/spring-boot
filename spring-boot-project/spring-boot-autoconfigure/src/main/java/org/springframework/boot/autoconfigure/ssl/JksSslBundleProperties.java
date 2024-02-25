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
     * Returns the keystore associated with this JksSslBundleProperties.
     *
     * @return the keystore associated with this JksSslBundleProperties
     */
    public Store getKeystore() {
		return this.keystore;
	}

	/**
     * Returns the truststore used by this JksSslBundleProperties.
     *
     * @return the truststore used by this JksSslBundleProperties
     */
    public Store getTruststore() {
		return this.truststore;
	}

	/**
	 * Store properties.
	 */
	public static class Store {

		/**
		 * Type of the store to create, e.g. JKS.
		 */
		private String type;

		/**
		 * Provider for the store.
		 */
		private String provider;

		/**
		 * Location of the resource containing the store content.
		 */
		private String location;

		/**
		 * Password used to access the store.
		 */
		private String password;

		/**
         * Returns the type of the store.
         * 
         * @return the type of the store
         */
        public String getType() {
			return this.type;
		}

		/**
         * Sets the type of the store.
         * 
         * @param type the type of the store
         */
        public void setType(String type) {
			this.type = type;
		}

		/**
         * Returns the provider of the store.
         *
         * @return the provider of the store
         */
        public String getProvider() {
			return this.provider;
		}

		/**
         * Sets the provider for the store.
         * 
         * @param provider the provider to be set
         */
        public void setProvider(String provider) {
			this.provider = provider;
		}

		/**
         * Returns the location of the store.
         *
         * @return the location of the store
         */
        public String getLocation() {
			return this.location;
		}

		/**
         * Sets the location of the store.
         * 
         * @param location the new location of the store
         */
        public void setLocation(String location) {
			this.location = location;
		}

		/**
         * Returns the password of the Store.
         *
         * @return the password of the Store
         */
        public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the Store.
         * 
         * @param password the password to be set
         */
        public void setPassword(String password) {
			this.password = password;
		}

	}

}
