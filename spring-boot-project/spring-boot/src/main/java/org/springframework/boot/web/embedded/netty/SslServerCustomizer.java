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

package org.springframework.boot.web.embedded.netty;

import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManagerFactorySpi;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;

import io.netty.handler.ssl.ClientAuth;
import reactor.netty.http.Http11SslContextSpec;
import reactor.netty.http.Http2SslContextSpec;
import reactor.netty.http.server.HttpServer;
import reactor.netty.tcp.AbstractProtocolSslContextSpec;

import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslConfigurationValidator;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * {@link NettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Chris Bono
 * @author Cyril Dangerville
 * @since 2.0.0
 * @deprecated this class is meant for Spring Boot internal use only.
 */
@Deprecated
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
		AbstractProtocolSslContextSpec<?> sslContextSpec = createSslContextSpec();
		return server.secure((spec) -> spec.sslContext(sslContextSpec));
	}

	protected AbstractProtocolSslContextSpec<?> createSslContextSpec() {
		AbstractProtocolSslContextSpec<?> sslContextSpec;
		if (this.http2 != null && this.http2.isEnabled()) {
			sslContextSpec = Http2SslContextSpec.forServer(getKeyManagerFactory(this.ssl, this.sslStoreProvider));
		}
		else {
			sslContextSpec = Http11SslContextSpec.forServer(getKeyManagerFactory(this.ssl, this.sslStoreProvider));
		}
		sslContextSpec.configure((builder) -> {
			builder.trustManager(getTrustManagerFactory(this.ssl, this.sslStoreProvider));
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
		});
		return sslContextSpec;
	}

	KeyManagerFactory getKeyManagerFactory(Ssl ssl, SslStoreProvider sslStoreProvider) {
		try {
			KeyStore keyStore = getKeyStore(ssl, sslStoreProvider);
			SslConfigurationValidator.validateKeyAlias(keyStore, ssl.getKeyAlias());
			KeyManagerFactory keyManagerFactory = (ssl.getKeyAlias() == null)
					? KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
					: new ConfigurableAliasKeyManagerFactory(ssl.getKeyAlias(),
							KeyManagerFactory.getDefaultAlgorithm());
			String keyPassword = (sslStoreProvider != null) ? sslStoreProvider.getKeyPassword() : null;
			if (keyPassword == null) {
				keyPassword = (ssl.getKeyPassword() != null) ? ssl.getKeyPassword() : ssl.getKeyStorePassword();
			}
			keyManagerFactory.init(keyStore, (keyPassword != null) ? keyPassword.toCharArray() : null);
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

	TrustManagerFactory getTrustManagerFactory(Ssl ssl, SslStoreProvider sslStoreProvider) {
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

	private KeyStore loadStore(String keystoreType, String provider, String keystoreLocation, String password)
			throws Exception {
		keystoreType = (keystoreType != null) ? keystoreType : "JKS";
		char[] passwordChars = (password != null) ? password.toCharArray() : null;
		KeyStore store = (provider != null) ? KeyStore.getInstance(keystoreType, provider)
				: KeyStore.getInstance(keystoreType);
		if (keystoreType.equalsIgnoreCase("PKCS11")) {
			Assert.state(!StringUtils.hasText(keystoreLocation),
					() -> "Keystore location '" + keystoreLocation + "' must be empty or null for PKCS11 key stores");
			store.load(null, passwordChars);
		}
		else {
			try {
				URL url = ResourceUtils.getURL(keystoreLocation);
				try (InputStream stream = url.openStream()) {
					store.load(stream, passwordChars);
				}
			}
			catch (Exception ex) {
				throw new WebServerException("Could not load key store '" + keystoreLocation + "'", ex);
			}
		}
		return store;
	}

	/**
	 * A {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to
	 * the fact that the actual calls to retrieve the key by alias are done at request
	 * time the approach is to wrap the actual key managers with a
	 * {@link ConfigurableAliasKeyManager}. The actual SPI has to be wrapped as well due
	 * to the fact that {@link KeyManagerFactory#getKeyManagers()} is final.
	 */
	private static final class ConfigurableAliasKeyManagerFactory extends KeyManagerFactory {

		private ConfigurableAliasKeyManagerFactory(String alias, String algorithm) throws NoSuchAlgorithmException {
			this(KeyManagerFactory.getInstance(algorithm), alias, algorithm);
		}

		private ConfigurableAliasKeyManagerFactory(KeyManagerFactory delegate, String alias, String algorithm) {
			super(new ConfigurableAliasKeyManagerFactorySpi(delegate, alias), delegate.getProvider(), algorithm);
		}

	}

	private static final class ConfigurableAliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

		private final KeyManagerFactory delegate;

		private final String alias;

		private ConfigurableAliasKeyManagerFactorySpi(KeyManagerFactory delegate, String alias) {
			this.delegate = delegate;
			this.alias = alias;
		}

		@Override
		protected void engineInit(KeyStore keyStore, char[] chars)
				throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
			this.delegate.init(keyStore, chars);
		}

		@Override
		protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
				throws InvalidAlgorithmParameterException {
			throw new InvalidAlgorithmParameterException("Unsupported ManagerFactoryParameters");
		}

		@Override
		protected KeyManager[] engineGetKeyManagers() {
			return Arrays.stream(this.delegate.getKeyManagers())
				.filter(X509ExtendedKeyManager.class::isInstance)
				.map(X509ExtendedKeyManager.class::cast)
				.map(this::wrap)
				.toArray(KeyManager[]::new);
		}

		private ConfigurableAliasKeyManager wrap(X509ExtendedKeyManager keyManager) {
			return new ConfigurableAliasKeyManager(keyManager, this.alias);
		}

	}

	private static final class ConfigurableAliasKeyManager extends X509ExtendedKeyManager {

		private final X509ExtendedKeyManager delegate;

		private final String alias;

		private ConfigurableAliasKeyManager(X509ExtendedKeyManager keyManager, String alias) {
			this.delegate = keyManager;
			this.alias = alias;
		}

		@Override
		public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
			return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
		}

		@Override
		public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
			return (this.alias != null) ? this.alias : this.delegate.chooseEngineServerAlias(s, principals, sslEngine);
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
