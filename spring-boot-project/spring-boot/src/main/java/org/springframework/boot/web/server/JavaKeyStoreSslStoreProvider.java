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

package org.springframework.boot.web.server;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;

import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * An {@link SslStoreProvider} that creates key and trust stores from Java keystore files.
 *
 * @author Scott Frederick
 */
final class JavaKeyStoreSslStoreProvider implements SslStoreProvider {

	private final Ssl ssl;

	private JavaKeyStoreSslStoreProvider(Ssl ssl) {
		this.ssl = ssl;
	}

	@Override
	public KeyStore getKeyStore() throws Exception {
		return createKeyStore(this.ssl.getKeyStoreType(), this.ssl.getKeyStoreProvider(), this.ssl.getKeyStore(),
				this.ssl.getKeyStorePassword());
	}

	@Override
	public KeyStore getTrustStore() throws Exception {
		if (this.ssl.getTrustStore() == null) {
			return null;
		}
		return createKeyStore(this.ssl.getTrustStoreType(), this.ssl.getTrustStoreProvider(), this.ssl.getTrustStore(),
				this.ssl.getTrustStorePassword());
	}

	@Override
	public String getKeyPassword() {
		return this.ssl.getKeyPassword();
	}

	private KeyStore createKeyStore(String type, String provider, String location, String password) throws Exception {
		type = (type != null) ? type : "JKS";
		char[] passwordChars = (password != null) ? password.toCharArray() : null;
		KeyStore store = (provider != null) ? KeyStore.getInstance(type, provider) : KeyStore.getInstance(type);
		if (type.equalsIgnoreCase("PKCS11")) {
			Assert.state(!StringUtils.hasText(location),
					() -> "KeyStore location is '" + location + "', but must be empty or null for PKCS11 key stores");
			store.load(null, passwordChars);
		}
		else {
			Assert.state(StringUtils.hasText(location), () -> "KeyStore location must not be empty or null");
			try {
				URL url = ResourceUtils.getURL(location);
				try (InputStream stream = url.openStream()) {
					store.load(stream, passwordChars);
				}
			}
			catch (Exception ex) {
				throw new IllegalStateException("Could not load key store '" + location + "'", ex);
			}
		}
		return store;
	}

	/**
	 * Create an {@link SslStoreProvider} if the appropriate SSL properties are
	 * configured.
	 * @param ssl the SSL properties
	 * @return an {@code SslStoreProvider} or {@code null}
	 */
	static SslStoreProvider from(Ssl ssl) {
		if (ssl != null && ssl.isEnabled()) {
			return new JavaKeyStoreSslStoreProvider(ssl);
		}
		return null;
	}

}
