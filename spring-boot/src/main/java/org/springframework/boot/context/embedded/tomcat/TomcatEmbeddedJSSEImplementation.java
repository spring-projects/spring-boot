/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded.tomcat;

import java.io.IOException;
import java.security.KeyStore;

import org.apache.tomcat.util.net.AbstractEndpoint;
import org.apache.tomcat.util.net.SSLUtil;
import org.apache.tomcat.util.net.ServerSocketFactory;
import org.apache.tomcat.util.net.jsse.JSSEImplementation;
import org.apache.tomcat.util.net.jsse.JSSESocketFactory;

import org.springframework.boot.context.embedded.SslStoreProvider;

/**
 * {@link JSSEImplementation} for embedded Tomcat that supports {@link SslStoreProvider}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @since 1.4.0
 */
public class TomcatEmbeddedJSSEImplementation extends JSSEImplementation {

	@Override
	public ServerSocketFactory getServerSocketFactory(AbstractEndpoint<?> endpoint) {
		return new SocketFactory(endpoint);
	}

	@Override
	public SSLUtil getSSLUtil(AbstractEndpoint<?> endpoint) {
		return new SocketFactory(endpoint);
	}

	/**
	 * {@link JSSESocketFactory} that supports {@link SslStoreProvider}.
	 */
	static class SocketFactory extends JSSESocketFactory {

		private final SslStoreProvider sslStoreProvider;

		SocketFactory(AbstractEndpoint<?> endpoint) {
			super(endpoint);
			this.sslStoreProvider = (SslStoreProvider) endpoint
					.getAttribute("sslStoreProvider");
		}

		@Override
		protected KeyStore getKeystore(String type, String provider, String pass)
				throws IOException {
			if (this.sslStoreProvider != null) {
				try {
					KeyStore store = this.sslStoreProvider.getKeyStore();
					if (store != null) {
						return store;
					}
				}
				catch (Exception ex) {
					throw new IOException(ex);
				}
			}
			return super.getKeystore(type, provider, pass);
		}

		@Override
		protected KeyStore getTrustStore(String keystoreType, String keystoreProvider)
				throws IOException {
			if (this.sslStoreProvider != null) {
				try {
					KeyStore store = this.sslStoreProvider.getTrustStore();
					if (store != null) {
						return store;
					}
				}
				catch (Exception ex) {
					throw new IOException(ex);
				}
			}
			return super.getTrustStore(keystoreType, keystoreProvider);
		}

	}

}
