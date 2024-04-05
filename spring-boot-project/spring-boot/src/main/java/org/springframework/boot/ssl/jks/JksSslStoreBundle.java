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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.springframework.boot.io.ApplicationResourceLoader;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.core.io.Resource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
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

	@Override
	public KeyStore getKeyStore() {
		return this.keyStore;
	}

	@Override
	public String getKeyStorePassword() {
		return (this.keyStoreDetails != null) ? this.keyStoreDetails.password() : null;
	}

	@Override
	public KeyStore getTrustStore() {
		return this.trustStore;
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
			Resource resource = new ApplicationResourceLoader().getResource(location);
			try (InputStream stream = resource.getInputStream()) {
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
		creator.append("keyStore.type", (this.keyStore != null) ? this.keyStore.getType() : "none");
		String keyStorePassword = getKeyStorePassword();
		creator.append("keyStorePassword", (keyStorePassword != null) ? "******" : null);
		creator.append("trustStore.type", (this.trustStore != null) ? this.trustStore.getType() : "none");
		return creator.toString();
	}

}
