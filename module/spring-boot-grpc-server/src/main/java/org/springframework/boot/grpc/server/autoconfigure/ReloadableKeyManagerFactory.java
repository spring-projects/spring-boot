/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure;

import java.net.Socket;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.util.Assert;

/**
 * A {@link KeyManagerFactory} that can be reloaded when the {@link SslBundle} changes.
 *
 * @author Phillip Webb
 */
final class ReloadableKeyManagerFactory extends KeyManagerFactory {

	private final ReloadableKeyManagerFactorySpi spi;

	private static final Log logger = LogFactory.getLog(ReloadableKeyManagerFactory.class);

	private ReloadableKeyManagerFactory(ReloadableKeyManagerFactorySpi spi, Provider provider) {
		super(spi, provider, "Reloadable");
		this.spi = spi;
	}

	static ReloadableKeyManagerFactory create(SslBundles bundles, String bundleName, SslBundle bundle) {
		Provider provider = bundle.getManagers().getKeyManagerFactory().getProvider();
		ReloadableKeyManagerFactory factory = new ReloadableKeyManagerFactory(new ReloadableKeyManagerFactorySpi(),
				provider);
		factory.spi.setSslBundle(bundle);
		bundles.addBundleUpdateHandler(bundleName, factory.spi::setSslBundle);
		return factory;
	}

	static class ReloadableKeyManagerFactorySpi extends KeyManagerFactorySpi {

		private volatile @Nullable ReloadableX509ExtendedKeyManager keyManager;

		@Override
		protected void engineInit(KeyStore ks, char[] password) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void engineInit(javax.net.ssl.ManagerFactoryParameters spec) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected KeyManager[] engineGetKeyManagers() {
			ReloadableX509ExtendedKeyManager localKeyManager = this.keyManager;
			Assert.state(localKeyManager != null, "KeyManager not initialized");
			return new KeyManager[] { localKeyManager };
		}

		private void setSslBundle(SslBundle bundle) {
			logger.debug("Populating Reloadable Key Manager...");
			SslManagerBundle managers = bundle.getManagers();
			KeyManagerFactory keyManagerFactory = managers.getKeyManagerFactory();
			X509ExtendedKeyManager x509KeyManager = findX509KeyManager(keyManagerFactory);
			ReloadableX509ExtendedKeyManager localKeyManager = this.keyManager;
			if (localKeyManager == null) {
				this.keyManager = new ReloadableX509ExtendedKeyManager(x509KeyManager);
			}
			else {
				localKeyManager.setDelegate(x509KeyManager);
			}
		}

		private X509ExtendedKeyManager findX509KeyManager(KeyManagerFactory keyManagerFactory) {
			for (KeyManager km : keyManagerFactory.getKeyManagers()) {
				if (km instanceof X509ExtendedKeyManager x509ExtendedKeyManager) {
					return x509ExtendedKeyManager;
				}
			}
			throw new IllegalStateException("No X509ExtendedKeyManager found in " + keyManagerFactory);
		}

	}

	static class ReloadableX509ExtendedKeyManager extends X509ExtendedKeyManager {

		private volatile X509ExtendedKeyManager delegate;

		ReloadableX509ExtendedKeyManager(X509ExtendedKeyManager delegate) {
			this.delegate = delegate;
		}

		void setDelegate(X509ExtendedKeyManager delegate) {
			this.delegate = delegate;
		}

		@Override
		public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
			return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
		}

		@Override
		public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
			return this.delegate.chooseEngineServerAlias(s, principals, sslEngine);
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return this.delegate.getCertificateChain(alias);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return this.delegate.getClientAliases(keyType, issuers);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return this.delegate.getPrivateKey(alias);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return this.delegate.getServerAliases(keyType, issuers);
		}

	}

}
