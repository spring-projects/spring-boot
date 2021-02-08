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
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.springframework.util.Assert;

/**
 * Builds an {@link SSLContext} for use with an HTTP connection.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 2.3.0
 */
public class SslContextFactory {

	private static final char[] NO_PASSWORD = {};

	private static final String KEY_STORE_ALIAS = "spring-boot-docker";

	public SslContextFactory() {
	}

	/**
	 * Create an {@link SSLContext} from files in the specified directory. The directory
	 * must contain files with the names 'key.pem', 'cert.pem', and 'ca.pem'.
	 * @param directory the path to a directory containing certificate and key files
	 * @return the {@code SSLContext}
	 */
	public SSLContext forDirectory(String directory) {
		try {
			Path keyPath = Paths.get(directory, "key.pem");
			Path certPath = Paths.get(directory, "cert.pem");
			Path caPath = Paths.get(directory, "ca.pem");
			Path caKeyPath = Paths.get(directory, "ca-key.pem");
			verifyCertificateFiles(keyPath, certPath, caPath);
			KeyManagerFactory keyManagerFactory = getKeyManagerFactory(keyPath, certPath);
			TrustManagerFactory trustManagerFactory = getTrustManagerFactory(caPath, caKeyPath);
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
			return sslContext;
		}
		catch (RuntimeException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	private KeyManagerFactory getKeyManagerFactory(Path keyPath, Path certPath) throws Exception {
		KeyStore store = KeyStoreFactory.create(certPath, keyPath, KEY_STORE_ALIAS);
		KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		factory.init(store, NO_PASSWORD);
		return factory;
	}

	private TrustManagerFactory getTrustManagerFactory(Path caPath, Path caKeyPath)
			throws NoSuchAlgorithmException, KeyStoreException {
		KeyStore store = KeyStoreFactory.create(caPath, caKeyPath, KEY_STORE_ALIAS);
		TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		factory.init(store);
		return factory;
	}

	private static void verifyCertificateFiles(Path... paths) {
		for (Path path : paths) {
			Assert.state(Files.exists(path) && Files.isRegularFile(path),
					"Certificate path must contain the files 'ca.pem', 'cert.pem', and 'key.pem' files");
		}
	}

}
