/*
 * Copyright 2012-2024 the original author or authors.
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link SslStoreBundle} backed by a Java keystore.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 3.1.0
 */
public class JksSslStoreBundle implements SslStoreBundle {

	private final JksSslStoreDetails keyStoreDetails;

	private final KeyStore keyStore;

	private final KeyStore trustStore;

	/**
	 * Create a new {@link JksSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 */
	public JksSslStoreBundle(JksSslStoreDetails keyStoreDetails, JksSslStoreDetails trustStoreDetails) {
		this.keyStoreDetails = keyStoreDetails;
		this.keyStore = createKeyStore("key", this.keyStoreDetails);
		this.trustStore = createKeyStore("trust", trustStoreDetails);
	}

	/**
	 * Returns the KeyStore object associated with this JksSslStoreBundle.
	 * @return the KeyStore object
	 */
	@Override
	public KeyStore getKeyStore() {
		return this.keyStore;
	}

	/**
	 * Returns the password for the keystore.
	 * @return the password for the keystore, or null if the keystore details are not set
	 */
	@Override
	public String getKeyStorePassword() {
		return (this.keyStoreDetails != null) ? this.keyStoreDetails.password() : null;
	}

	/**
	 * Returns the trust store used for SSL/TLS connections.
	 * @return the trust store
	 */
	@Override
	public KeyStore getTrustStore() {
		return this.trustStore;
	}

	/**
	 * Creates a KeyStore with the given name and JksSslStoreDetails.
	 * @param name the name of the KeyStore
	 * @param details the JksSslStoreDetails containing the details for the KeyStore
	 * @return the created KeyStore
	 * @throws IllegalStateException if unable to create the KeyStore
	 */
	private KeyStore createKeyStore(String name, JksSslStoreDetails details) {
		if (details == null || details.isEmpty()) {
			return null;
		}
		try {
			String type = (!StringUtils.hasText(details.type())) ? KeyStore.getDefaultType() : details.type();
			char[] password = (details.password() != null) ? details.password().toCharArray() : null;
			String location = details.location();
			KeyStore store = getKeyStoreInstance(type, details.provider());
			if (isHardwareKeystoreType(type)) {
				loadHardwareKeyStore(store, location, password);
			}
			else {
				loadKeyStore(store, location, password);
			}
			return store;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Unable to create %s store: %s".formatted(name, ex.getMessage()), ex);
		}
	}

	/**
	 * Returns an instance of KeyStore based on the specified type and provider.
	 * @param type the type of the KeyStore
	 * @param provider the provider of the KeyStore (optional)
	 * @return an instance of KeyStore
	 * @throws KeyStoreException if an error occurs while creating the KeyStore
	 * @throws NoSuchProviderException if the specified provider is not available
	 */
	private KeyStore getKeyStoreInstance(String type, String provider)
			throws KeyStoreException, NoSuchProviderException {
		return (!StringUtils.hasText(provider)) ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider);
	}

	/**
	 * Checks if the given keystore type is a hardware keystore type.
	 * @param type the keystore type to check
	 * @return true if the keystore type is a hardware keystore type, false otherwise
	 */
	private boolean isHardwareKeystoreType(String type) {
		return type.equalsIgnoreCase("PKCS11");
	}

	/**
	 * Loads a hardware key store.
	 * @param store the key store to load
	 * @param location the location of the key store (must be empty or null for PKCS11
	 * hardware key stores)
	 * @param password the password to access the key store
	 * @throws IOException if an I/O error occurs while loading the key store
	 * @throws NoSuchAlgorithmException if the specified algorithm is not available
	 * @throws CertificateException if any of the certificates in the key store could not
	 * be loaded
	 */
	private void loadHardwareKeyStore(KeyStore store, String location, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		Assert.state(!StringUtils.hasText(location),
				() -> "Location is '%s', but must be empty or null for PKCS11 hardware key stores".formatted(location));
		store.load(null, password);
	}

	/**
	 * Loads a KeyStore from the specified location using the provided password.
	 * @param store the KeyStore object to load the data into
	 * @param location the location of the KeyStore file
	 * @param password the password to access the KeyStore
	 * @throws IllegalStateException if the KeyStore cannot be loaded from the specified
	 * location
	 * @throws IllegalArgumentException if the location is empty or null
	 */
	private void loadKeyStore(KeyStore store, String location, char[] password) {
		Assert.state(StringUtils.hasText(location), () -> "Location must not be empty or null");
		try {
			URL url = ResourceUtils.getURL(location);
			try (InputStream stream = url.openStream()) {
				store.load(stream, password);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store from '" + location + "'", ex);
		}
	}

	/**
	 * Returns a string representation of the JksSslStoreBundle object.
	 *
	 * The string representation includes the type of the key store, the masked password
	 * of the key store, and the type of the trust store.
	 * @return a string representation of the JksSslStoreBundle object
	 */
	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("keyStore.type", (this.keyStore != null) ? this.keyStore.getType() : "none");
		String keyStorePassword = getKeyStorePassword();
		creator.append("keyStorePassword", (keyStorePassword != null) ? "******" : null);
		creator.append("trustStore.type", (this.trustStore != null) ? this.trustStore.getType() : "none");
		return creator.toString();
	}

}
