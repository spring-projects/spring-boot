/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.tomcat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.security.KeyStore;

import org.springframework.boot.context.embedded.SslStoreProvider;

/**
 * A {@link URLStreamHandlerFactory} that provides a {@link URLStreamHandler} for
 * accessing an {@link SslStoreProvider}'s key store and trust store from a URL.
 *
 * @author Andy Wilkinson
 */
class SslStoreProviderUrlStreamHandlerFactory implements URLStreamHandlerFactory {

	private static final String PROTOCOL = "springbootssl";

	private static final String KEY_STORE_PATH = "keyStore";

	static final String KEY_STORE_URL = PROTOCOL + ":" + KEY_STORE_PATH;

	private static final String TRUST_STORE_PATH = "trustStore";

	static final String TRUST_STORE_URL = PROTOCOL + ":" + TRUST_STORE_PATH;

	private final SslStoreProvider sslStoreProvider;

	SslStoreProviderUrlStreamHandlerFactory(SslStoreProvider sslStoreProvider) {
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(String protocol) {
		if (PROTOCOL.equals(protocol)) {
			return new URLStreamHandler() {

				@Override
				protected URLConnection openConnection(URL url) throws IOException {
					try {
						if (KEY_STORE_PATH.equals(url.getPath())) {
							return new KeyStoreUrlConnection(url,
									SslStoreProviderUrlStreamHandlerFactory.this.sslStoreProvider
											.getKeyStore());
						}
						if (TRUST_STORE_PATH.equals(url.getPath())) {
							return new KeyStoreUrlConnection(url,
									SslStoreProviderUrlStreamHandlerFactory.this.sslStoreProvider
											.getTrustStore());
						}
					}
					catch (Exception ex) {
						throw new IOException(ex);
					}
					throw new IOException("Invalid path: " + url.getPath());
				}
			};
		}
		return null;
	}

	private static final class KeyStoreUrlConnection extends URLConnection {

		private final KeyStore keyStore;

		private KeyStoreUrlConnection(URL url, KeyStore keyStore) {
			super(url);
			this.keyStore = keyStore;
		}

		@Override
		public void connect() throws IOException {

		}

		@Override
		public InputStream getInputStream() throws IOException {

			try {
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				this.keyStore.store(stream, new char[0]);
				return new ByteArrayInputStream(stream.toByteArray());
			}
			catch (Exception ex) {
				throw new IOException(ex);
			}
		}

	}

}
