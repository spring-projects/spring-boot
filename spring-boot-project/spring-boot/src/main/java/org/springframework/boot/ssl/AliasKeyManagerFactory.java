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

package org.springframework.boot.ssl;

import java.net.Socket;
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
import javax.net.ssl.X509ExtendedKeyManager;

/**
 * {@link KeyManagerFactory} that allows a configurable key alias to be used. Due to the
 * fact that the actual calls to retrieve the key by alias are done at request time the
 * approach is to wrap the actual key managers with a {@link AliasX509ExtendedKeyManager}.
 * The actual SPI has to be wrapped as well due to the fact that
 * {@link KeyManagerFactory#getKeyManagers()} is final.
 *
 * @author Scott Frederick
 */
final class AliasKeyManagerFactory extends KeyManagerFactory {

	/**
     * Constructs a new AliasKeyManagerFactory with the specified delegate, alias, and algorithm.
     * 
     * @param delegate the delegate KeyManagerFactory
     * @param alias the alias for the KeyManagerFactory
     * @param algorithm the algorithm for the KeyManagerFactory
     */
    AliasKeyManagerFactory(KeyManagerFactory delegate, String alias, String algorithm) {
		super(new AliasKeyManagerFactorySpi(delegate, alias), delegate.getProvider(), algorithm);
	}

	/**
	 * {@link KeyManagerFactorySpi} that allows a configurable key alias to be used.
	 */
	private static final class AliasKeyManagerFactorySpi extends KeyManagerFactorySpi {

		private final KeyManagerFactory delegate;

		private final String alias;

		/**
         * Constructs a new AliasKeyManagerFactorySpi with the specified delegate KeyManagerFactory and alias.
         * 
         * @param delegate the delegate KeyManagerFactory to be used
         * @param alias the alias to be associated with the KeyManagerFactory
         */
        private AliasKeyManagerFactorySpi(KeyManagerFactory delegate, String alias) {
			this.delegate = delegate;
			this.alias = alias;
		}

		/**
         * Initializes the engine with the specified KeyStore and password.
         * 
         * @param keyStore the KeyStore containing the keys and certificates
         * @param chars the password used to access the KeyStore
         * @throws KeyStoreException if there is an error accessing the KeyStore
         * @throws NoSuchAlgorithmException if the specified algorithm is not available
         * @throws UnrecoverableKeyException if the key cannot be recovered
         */
        @Override
		protected void engineInit(KeyStore keyStore, char[] chars)
				throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
			this.delegate.init(keyStore, chars);
		}

		/**
         * Initializes the engine with the specified ManagerFactoryParameters.
         * 
         * @param managerFactoryParameters the ManagerFactoryParameters to initialize the engine with
         * @throws InvalidAlgorithmParameterException if the ManagerFactoryParameters are unsupported
         */
        @Override
		protected void engineInit(ManagerFactoryParameters managerFactoryParameters)
				throws InvalidAlgorithmParameterException {
			throw new InvalidAlgorithmParameterException("Unsupported ManagerFactoryParameters");
		}

		/**
         * Returns an array of KeyManagers for this AliasKeyManagerFactorySpi.
         * 
         * @return an array of KeyManagers
         */
        @Override
		protected KeyManager[] engineGetKeyManagers() {
			return Arrays.stream(this.delegate.getKeyManagers())
				.filter(X509ExtendedKeyManager.class::isInstance)
				.map(X509ExtendedKeyManager.class::cast)
				.map(this::wrap)
				.toArray(KeyManager[]::new);
		}

		/**
         * Wraps the provided X509ExtendedKeyManager with an instance of AliasX509ExtendedKeyManager.
         * 
         * @param keyManager the X509ExtendedKeyManager to be wrapped
         * @return an instance of AliasX509ExtendedKeyManager
         */
        private AliasKeyManagerFactory.AliasX509ExtendedKeyManager wrap(X509ExtendedKeyManager keyManager) {
			return new AliasX509ExtendedKeyManager(keyManager, this.alias);
		}

	}

	/**
	 * {@link X509ExtendedKeyManager} that allows a configurable key alias to be used.
	 */
	static final class AliasX509ExtendedKeyManager extends X509ExtendedKeyManager {

		private final X509ExtendedKeyManager delegate;

		private final String alias;

		/**
         * Constructs a new AliasX509ExtendedKeyManager with the specified X509ExtendedKeyManager and alias.
         * 
         * @param keyManager the X509ExtendedKeyManager to delegate to
         * @param alias the alias to use for this key manager
         */
        private AliasX509ExtendedKeyManager(X509ExtendedKeyManager keyManager, String alias) {
			this.delegate = keyManager;
			this.alias = alias;
		}

		/**
         * Returns the client alias to be used when selecting the certificate chain for the given SSL engine.
         * 
         * @param strings    the key types to be used for the client authentication
         * @param principals the principal names associated with the client authentication
         * @param sslEngine  the SSL engine for which the client alias is being selected
         * @return the client alias to be used for the given SSL engine
         */
        @Override
		public String chooseEngineClientAlias(String[] strings, Principal[] principals, SSLEngine sslEngine) {
			return this.delegate.chooseEngineClientAlias(strings, principals, sslEngine);
		}

		/**
         * Returns the alias of the engine server to be used for SSL/TLS connections.
         * 
         * @param s the identification algorithm
         * @param principals the array of principals representing the client
         * @param sslEngine the SSL engine
         * @return the alias of the engine server
         */
        @Override
		public String chooseEngineServerAlias(String s, Principal[] principals, SSLEngine sslEngine) {
			return this.alias;
		}

		/**
         * Returns the alias of the client certificate to be used for authentication during SSL/TLS handshake.
         * 
         * @param keyType the acceptable key types for the client certificate, ordered by the client's preference
         * @param issuers the acceptable certificate issuers for the client certificate, ordered by the client's preference
         * @param socket the socket to be used for the SSL/TLS handshake
         * @return the alias of the client certificate to be used for authentication, or null if no suitable client certificate is available
         */
        @Override
		public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseClientAlias(keyType, issuers, socket);
		}

		/**
         * Returns the server alias for the specified key type, issuers, and socket.
         * 
         * @param keyType the type of the key
         * @param issuers the issuers of the key
         * @param socket the socket
         * @return the server alias
         */
        @Override
		public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
			return this.delegate.chooseServerAlias(keyType, issuers, socket);
		}

		/**
         * Returns the certificate chain associated with the specified alias.
         * 
         * @param alias the alias for the certificate chain
         * @return the certificate chain associated with the specified alias
         */
        @Override
		public X509Certificate[] getCertificateChain(String alias) {
			return this.delegate.getCertificateChain(alias);
		}

		/**
         * Returns the client aliases associated with the specified key type and issuers.
         * 
         * @param keyType the type of the key
         * @param issuers the issuers of the key
         * @return an array of client aliases
         */
        @Override
		public String[] getClientAliases(String keyType, Principal[] issuers) {
			return this.delegate.getClientAliases(keyType, issuers);
		}

		/**
         * Returns the private key associated with the specified alias.
         * 
         * @param alias the alias of the private key
         * @return the private key associated with the specified alias
         */
        @Override
		public PrivateKey getPrivateKey(String alias) {
			return this.delegate.getPrivateKey(alias);
		}

		/**
         * Returns the server aliases associated with the specified key type and issuers.
         * 
         * @param keyType the type of the key
         * @param issuers the issuers of the key
         * @return an array of server aliases
         */
        @Override
		public String[] getServerAliases(String keyType, Principal[] issuers) {
			return this.delegate.getServerAliases(keyType, issuers);
		}

	}

}
