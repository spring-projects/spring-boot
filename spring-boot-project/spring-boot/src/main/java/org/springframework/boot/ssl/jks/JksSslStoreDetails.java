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

package org.springframework.boot.ssl.jks;

import java.security.KeyStore;

import org.springframework.util.StringUtils;

/**
 * Details for an individual trust or key store in a {@link JksSslStoreBundle}.
 *
 * @param type the key store type, for example {@code JKS} or {@code PKCS11}. A
 * {@code null} value will use {@link KeyStore#getDefaultType()}).
 * @param provider the name of the key store provider
 * @param location the location of the key store file or {@code null} if using a
 * {@code PKCS11} hardware store
 * @param password the password used to unlock the store or {@code null}
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 3.1.0
 */
public record JksSslStoreDetails(String type, String provider, String location, String password) {

	/**
	 * Return a new {@link JksSslStoreDetails} instance with a new password.
	 * @param password the new password
	 * @return a new {@link JksSslStoreDetails} instance
	 */
	public JksSslStoreDetails withPassword(String password) {
		return new JksSslStoreDetails(this.type, this.provider, this.location, password);
	}

	boolean isEmpty() {
		return isEmpty(this.type) && isEmpty(this.provider) && isEmpty(this.location);
	}

	private boolean isEmpty(String value) {
		return !StringUtils.hasText(value);
	}

	/**
	 * Factory method to create a new {@link JksSslStoreDetails} instance for the given
	 * location.
	 * @param location the location
	 * @return a new {@link JksSslStoreDetails} instance.
	 */
	public static JksSslStoreDetails forLocation(String location) {
		return new JksSslStoreDetails(null, null, location, null);
	}

}
