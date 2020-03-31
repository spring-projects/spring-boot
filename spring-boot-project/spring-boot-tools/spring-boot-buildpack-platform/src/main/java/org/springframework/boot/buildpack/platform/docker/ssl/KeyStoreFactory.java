/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Utility methods for creating Java trust material from key and certificate files.
 *
 * @author Scott Frederick
 */
final class KeyStoreFactory {

	private KeyStoreFactory() {
	}

	/**
	 * Create a new {@link KeyStore} populated with the certificate stored at the
	 * specified file path and an optional private key.
	 * @param certPath the path to the certificate authority file
	 * @param keyPath the path to the private file
	 * @param alias the alias to use for KeyStore entries
	 * @return the {@code KeyStore}
	 */
	static KeyStore create(Path certPath, Path keyPath, String alias) {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null);

			X509Certificate[] certificates = CertificateParser.parse(certPath);

			if (keyPath != null && Files.exists(keyPath)) {
				PrivateKey privateKey = PrivateKeyParser.parse(keyPath);
				addCertsToStore(keyStore, certificates, privateKey, alias);
			}
			else {
				addCertsToStore(keyStore, certificates, alias);
			}

			return keyStore;
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
		}
	}

	private static void addCertsToStore(KeyStore keyStore, X509Certificate[] certificates, PrivateKey privateKey,
			String alias) {
		try {
			keyStore.setKeyEntry(alias, privateKey, new char[] {}, certificates);
		}
		catch (KeyStoreException ex) {
			throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
		}
	}

	private static void addCertsToStore(KeyStore keyStore, X509Certificate[] certs, String alias) {
		try {
			for (int index = 0; index < certs.length; index++) {
				String indexedAlias = alias + "-" + index;
				keyStore.setCertificateEntry(indexedAlias, certs[index]);
			}
		}
		catch (KeyStoreException ex) {
			throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
		}
	}

}
