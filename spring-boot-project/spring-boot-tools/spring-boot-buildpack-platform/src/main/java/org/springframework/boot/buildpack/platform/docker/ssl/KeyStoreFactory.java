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
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Utility methods for creating Java trust material from key and certificate files.
 *
 * @author Scott Frederick
 */
final class KeyStoreFactory {

	private static final char[] NO_PASSWORD = {};

	/**
	 * Private constructor for the KeyStoreFactory class.
	 */
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
			KeyStore keyStore = getKeyStore();
			X509Certificate[] certificates = CertificateParser.parse(certPath);
			PrivateKey privateKey = getPrivateKey(keyPath);
			try {
				addCertificates(keyStore, certificates, privateKey, alias);
			}
			catch (KeyStoreException ex) {
				throw new IllegalStateException("Error adding certificates to KeyStore: " + ex.getMessage(), ex);
			}
			return keyStore;
		}
		catch (GeneralSecurityException | IOException ex) {
			throw new IllegalStateException("Error creating KeyStore: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Retrieves the default KeyStore instance.
	 * @return the default KeyStore instance
	 * @throws KeyStoreException if there is an error accessing the KeyStore
	 * @throws IOException if there is an error reading the KeyStore
	 * @throws NoSuchAlgorithmException if the specified algorithm is not available
	 * @throws CertificateException if there is an error with the certificate
	 */
	private static KeyStore getKeyStore()
			throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null);
		return keyStore;
	}

	/**
	 * Retrieves the private key from the specified file path.
	 * @param path the path to the private key file
	 * @return the private key if the file exists and is valid, otherwise null
	 */
	private static PrivateKey getPrivateKey(Path path) {
		if (path != null && Files.exists(path)) {
			return PrivateKeyParser.parse(path);
		}
		return null;
	}

	/**
	 * Adds certificates to the specified KeyStore.
	 * @param keyStore the KeyStore to which the certificates will be added
	 * @param certificates an array of X509Certificates to be added to the KeyStore
	 * @param privateKey the PrivateKey associated with the certificates, or null if no
	 * private key is available
	 * @param alias the alias to be used for the certificates in the KeyStore
	 * @throws KeyStoreException if an error occurs while adding the certificates to the
	 * KeyStore
	 */
	private static void addCertificates(KeyStore keyStore, X509Certificate[] certificates, PrivateKey privateKey,
			String alias) throws KeyStoreException {
		if (privateKey != null) {
			keyStore.setKeyEntry(alias, privateKey, NO_PASSWORD, certificates);
		}
		else {
			for (int index = 0; index < certificates.length; index++) {
				keyStore.setCertificateEntry(alias + "-" + index, certificates[index]);
			}
		}
	}

}
