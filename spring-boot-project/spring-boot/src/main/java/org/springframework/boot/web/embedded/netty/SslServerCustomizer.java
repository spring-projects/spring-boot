/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.netty;

import java.net.Socket;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.SslProvider;

import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.ResourceUtils;

/**
 * {@link NettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Chris Bono
 * @since 2.0.0
 */
public class SslServerCustomizer implements NettyServerCustomizer {

	private final Ssl ssl;

	private final Http2 http2;

	private final SslStoreProvider sslStoreProvider;

	public SslServerCustomizer(Ssl ssl, Http2 http2, SslStoreProvider sslStoreProvider) {
		this.ssl = ssl;
		this.http2 = http2;
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public HttpServer apply(HttpServer server) {
		try {
			return server.secure((contextSpec) -> {
				SslProvider.DefaultConfigurationSpec spec = contextSpec.sslContext(getContextBuilder());
				if (this.http2 != null && this.http2.isEnabled()) {
					spec.defaultConfiguration(SslProvider.DefaultConfigurationType.H2);
				}
			});
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected SslContextBuilder getContextBuilder() {
		SslContextBuilder builder = SslContextBuilder.forServer(getKeyManagerFactory(this.ssl, this.sslStoreProvider))
				.trustManager(getTrustManagerFactory(this.ssl, this.sslStoreProvider));
		if (this.ssl.getEnabledProtocols() != null) {
			builder.protocols(this.ssl.getEnabledProtocols());
		}
		if (this.ssl.getCiphers() != null) {
			builder.ciphers(Arrays.asList(this.ssl.getCiphers()));
		}
		if (this.ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			builder.clientAuth(ClientAuth.REQUIRE);
		}
		else if (this.ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			builder.clientAuth(ClientAuth.OPTIONAL);
		}
		return builder;
	}

	protected KeyManagerFactory getKeyManagerFactory(Ssl ssl, SslStoreProvider sslStoreProvider) {
		try {
			KeyStore keyStore = getKeyStore(ssl, sslStoreProvider);
			KeyManagerFactory keyManagerFactory = (ssl.getKeyAlias() == null)
					? KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
					: ConfigurableAliasKeyManagerFactory.instance(ssl.getKeyAlias(),
							KeyManagerFactory.getDefaultAlgorithm());
			char[] keyPassword = (ssl.getKeyPassword() != null) ? ssl.getKeyPassword().toCharArray() : null;
			if (keyPassword == null && ssl.getKeyStorePassword() != null) {
				keyPassword = ssl.getKeyStorePassword().toCharArray();
			}
			keyManagerFactory.init(keyStore, keyPassword);
			return keyManagerFactory;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private KeyStore getKeyStore(Ssl ssl, SslStoreProvider sslStoreProvider) throws Exception {
		if (sslStoreProvider != null) {
			return sslStoreProvider.getKeyStore();
		}
		return loadKeyStore(ssl.getKeyStoreType(), ssl.getKeyStoreProvider(), ssl.getKeyStore(),
				ssl.getKeyStorePassword());
	}

	protected TrustManagerFactory getTrustManagerFactory(Ssl ssl, SslStoreProvider sslStoreProvider) {
		try {
			KeyStore store = getTrustStore(ssl, sslStoreProvider);
			TrustManagerFactory trustManagerFactory = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init(store);
			return trustManagerFactory;
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	private KeyStore getTrustStore(Ssl ssl, SslStoreProvider sslStoreProvider) throws Exception {
		if (sslStoreProvider != null) {
			return sslStoreProvider.getTrustStore();
		}
		return loadTrustStore(ssl.getTrustStoreType(), ssl.getTrustStoreProvider(), ssl.getTrustStore(),
				ssl.getTrustStorePassword());
	}

	private KeyStore loadKeyStore(String type, String provider, String resource, String password) throws Exception {

		return loadStore(type, provider, resource, password);
	}

	private KeyStore loadTrustStore(String type, String provider, String resource, String password) throws Exception {
		if (resource == null) {
			return null;
		}
		return loadStore(type, provider, resource, password);
	}

	private KeyStore loadStore(String type, String provider, String resource, String password) throws Exception {
		type = (type != null) ? type : "JKS";
		KeyStore store = (provider != null) ? KeyStore.getInstance(type, provider) : KeyStore.getInstance(type);
		try {
			URL url = ResourceUtils.getURL(resource);
			store.load(url.openStream(), (password != null) ? password.toCharArray() : null);
			return store;
		}
		catch (Exception ex) {
			throw new WebServerException("Could not load key store '" + resource + "'", ex);
		}

	}

	/**
	 * A {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to
	 * the fact that the actual calls to retrieve the key by alias are done at request
	 * time the approach is to wrap the actual key managers with a
	 * {@link ConfigurableAliasKeyManager}. The actual SPI has to be wrapped as well due
	 * to the fact that {@link KeyManagerFactory#getKeyManagers()} is final.
	 */
	private static final class ConfigurableAliasKeyManagerFactory extends KeyManagerFactory {

		private static ConfigurableAliasKeyManagerFactory instance(String alias, String algorithm)
				throws NoSuchAlgorithmException {
			KeyManagerFactory originalFactory = KeyManagerFactory.getInstance(algorithm);
			ConfigurableAliasKeyManagerFactorySpi spi = new ConfigurableAliasKeyManagerFactorySpi(originalFactory,
					alias);
			return new ConfigurableAliasKeyManagerFactory(spi, originalFactory.getProvider(), algorithm);
		}

		private ConfigurableAliasKeyManagerFactory(ConfigurableAliasKeyManagerFactorySpi spi, Provider provider,
				String algorithm) {
			super(spi, provider, algorithm);
		}

	}

	private static final class ConfigurableAliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

		private KeyManagerFactory originalFactory;

		private String alias;

		private ConfigurableAliasKeyManagerFactorySpi(KeyManagerFactory originalFactory, String alias) {
			this.originalFactory = originalFactory;
			this.alias = alias;
		}

		@Override
		protected void engineInit(KeyStore keyStore, char[] chars)
				throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
			this.originalFactory.init(keyStore, chars);
		}

		@Override
		protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
				throws InvalidAlgorithmParameterException {
			throw new InvalidAlgorithmParameterException("Unsupported ManagerFactoryParameters");
		}

		@Override
		protected KeyManager[] engineGetKeyManagers() {
			return Arrays.stream(this.originalFactory.getKeyManagers()).filter(X509ExtendedKeyManager.class::isInstance)
					.map(X509ExtendedKeyManager.class::cast).map(this::wrapKeyManager).collect(Collectors.toList())
					.toArray(new KeyManager[0]);
		}

		private ConfigurableAliasKeyManager wrapKeyManager(X509ExtendedKeyManager km) {
			return new ConfigurableAliasKeyManager(km, this.alias);
		}

	}

	private static final class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

		private final X509ExtendedKeyManager keyManager;

		private final String alias;

		private ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, String alias) {
			this.keyManager = keyManager;
			this.alias = alias;
		}

		@Override
		public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
			return this.keyManager.chooseEngineClientAlias(strings, principals, sslEngine);
		}

		@Override
		public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
			if (this.alias == null) {
				return this.keyManager.chooseEngineServerAlias(s, principals, sslEngine);
			}
			return this.alias;
		}

		@Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return this.keyManager.chooseClientAlias(keyType, issuers, socket);
		}

		@Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return this.keyManager.chooseServerAlias(keyType, issuers, socket);
		}

		@Override
		public X509Certificate[] getCertificateChain(String alias) {
			return this.keyManager.getCertificateChain(alias);
		}

		@Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return this.keyManager.getClientAliases(keyType, issuers);
		}

		@Override
		public PrivateKey getPrivateKey(String alias) {
			return this.keyManager.getPrivateKey(alias);
		}

		@Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return this.keyManager.getServerAliases(keyType, issuers);
		}

	}

}
