/*
 * Copyright 2012-2025 the original author or authors.
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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;

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

	private final ResourceLoader resourceLoader;

	private final Supplier<KeyStore> keyStore;

	private final Supplier<KeyStore> trustStore;

	/**
	 * Create a new {@link JksSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 */
	public JksSslStoreBundle(JksSslStoreDetails keyStoreDetails, JksSslStoreDetails trustStoreDetails) {
		this(keyStoreDetails, trustStoreDetails, ApplicationResourceLoader.get());
	}

	/**
	 * Create a new {@link JksSslStoreBundle} instance.
	 * @param keyStoreDetails the key store details
	 * @param trustStoreDetails the trust store details
	 * @param resourceLoader the resource loader used to load content
	 * @since 3.3.5
	 */
	public JksSslStoreBundle(JksSslStoreDetails keyStoreDetails, JksSslStoreDetails trustStoreDetails,
			ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "'resourceLoader' must not be null");
		this.keyStoreDetails = keyStoreDetails;
		this.resourceLoader = resourceLoader;
		this.keyStore = SingletonSupplier.of(() -> createKeyStore("key", keyStoreDetails));
		this.trustStore = SingletonSupplier.of(() -> createKeyStore("trust", trustStoreDetails));
	}

	@Override
	public KeyStore getKeyStore() {
		return this.keyStore.get();
	}

	@Override
	public String getKeyStorePassword() {
		return (this.keyStoreDetails != null) ? this.keyStoreDetails.password() : null;
	}

	@Override
	public KeyStore getTrustStore() {
		return this.trustStore.get();
	}

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

	private KeyStore getKeyStoreInstance(String type, String provider)
			throws KeyStoreException, NoSuchProviderException {
		return (!StringUtils.hasText(provider)) ? KeyStore.getInstance(type) : KeyStore.getInstance(type, provider);
	}

	private boolean isHardwareKeystoreType(String type) {
		return type.equalsIgnoreCase("PKCS11");
	}

	private void loadHardwareKeyStore(KeyStore store, String location, char[] password)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		Assert.state(!StringUtils.hasText(location),
				() -> "Location is '%s', but must be empty or null for PKCS11 hardware key stores".formatted(location));
		store.load(null, password);
	}

	private void loadKeyStore(KeyStore store, String location, char[] password) {
		Assert.state(StringUtils.hasText(location), () -> "Location must not be empty or null");
		try {
			try (InputStream stream = this.resourceLoader.getResource(location).getInputStream()) {
				store.load(stream, password);
			}
		}
		catch (Exception ex) {
			throw new IllegalStateException("Could not load store from '" + location + "'", ex);
		}
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		KeyStore keyStore = this.keyStore.get();
		creator.append("keyStore.type", (keyStore != null) ? keyStore.getType() : "none");
		String keyStorePassword = getKeyStorePassword();
		creator.append("keyStorePassword", (keyStorePassword != null) ? "******" : null);
		KeyStore trustStore = this.trustStore.get();
		creator.append("trustStore.type", (trustStore != null) ? trustStore.getType() : "none");
		return creator.toString();
	}

}
