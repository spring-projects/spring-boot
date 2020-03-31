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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Builds an {@link SSLContext} for use with an HTTP connection.
 *
 * @author Scott Frederick
 * @since 2.3.0
 */
public class SslContextFactory {

	private static final String KEY_STORE_ALIAS = "spring-boot-docker";

	public SslContextFactory() {
	}

	/**
	 * Create an {@link SSLContext} from files in the specified directory.
	 *
	 * The directory must contain files with the names 'key.pem', 'cert.pem', and
	 * 'ca.pem'.
	 * @param certificatePath the path to a directory containing certificate and key files
	 * @return the {@code SSLContext}
	 */
	public SSLContext forPath(String certificatePath) {

		try {

			Path keyPath = Paths.get(certificatePath, "key.pem");
			Path certPath = Paths.get(certificatePath, "cert.pem");
			Path certAuthorityPath = Paths.get(certificatePath, "ca.pem");
			Path certAuthorityKeyPath = Paths.get(certificatePath, "ca-key.pem");

			verifyCertificateFiles(keyPath, certPath, certAuthorityPath);

			KeyStore keyStore = KeyStoreFactory.create(certPath, keyPath, KEY_STORE_ALIAS);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, new char[] {});

			KeyStore trustStore = KeyStoreFactory.create(certAuthorityPath, certAuthorityKeyPath, KEY_STORE_ALIAS);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(trustStore);

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

			return sslContext;

		}
		catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}

	}

	private static void verifyCertificateFiles(Path... certificateFilePaths) {
		for (Path path : certificateFilePaths) {
			if (!Files.exists(path)) {
				throw new RuntimeException(
						"Certificate path must contain the files 'ca.pem', 'cert.pem', and 'key.pem'");
			}
		}
	}

}
