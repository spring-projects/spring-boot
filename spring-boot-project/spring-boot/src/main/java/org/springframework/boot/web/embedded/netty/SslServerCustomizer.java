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

import java.net.URL;
import java.security.KeyStore;
import java.util.Arrays;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContextBuilder;
import reactor.ipc.netty.http.server.HttpServerOptions;

import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.util.ResourceUtils;

/**
 * {@link NettyServerCustomizer} that configures SSL for the given Reactor Netty server
 * instance.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class SslServerCustomizer implements NettyServerCustomizer {

	private final Ssl ssl;

	private final SslStoreProvider sslStoreProvider;

	public SslServerCustomizer(Ssl ssl, SslStoreProvider sslStoreProvider) {
		this.ssl = ssl;
		this.sslStoreProvider = sslStoreProvider;
	}

	@Override
	public void customize(HttpServerOptions.Builder builder) {
		SslContextBuilder sslBuilder = SslContextBuilder
				.forServer(getKeyManagerFactory(this.ssl, this.sslStoreProvider))
				.trustManager(getTrustManagerFactory(this.ssl, this.sslStoreProvider));
		if (this.ssl.getEnabledProtocols() != null) {
			sslBuilder.protocols(this.ssl.getEnabledProtocols());
		}
		if (this.ssl.getCiphers() != null) {
			sslBuilder = sslBuilder.ciphers(Arrays.asList(this.ssl.getCiphers()));
		}
		if (this.ssl.getClientAuth() == Ssl.ClientAuth.NEED) {
			sslBuilder = sslBuilder.clientAuth(ClientAuth.REQUIRE);
		}
		else if (this.ssl.getClientAuth() == Ssl.ClientAuth.WANT) {
			sslBuilder = sslBuilder.clientAuth(ClientAuth.OPTIONAL);
		}
		try {
			builder.sslContext(sslBuilder.build());
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}

	protected KeyManagerFactory getKeyManagerFactory(Ssl ssl, SslStoreProvider sslStoreProvider) {
		try {
			KeyStore keyStore = getKeyStore(ssl, sslStoreProvider);
			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
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
		return loadKeyStore(ssl.getTrustStoreType(), ssl.getTrustStoreProvider(), ssl.getTrustStore(),
				ssl.getTrustStorePassword());
	}

	private KeyStore loadKeyStore(String type, String provider, String resource, String password) throws Exception {
		type = (type != null) ? type : "JKS";
		if (resource == null) {
			return null;
		}
		KeyStore store = (provider != null) ? KeyStore.getInstance(type, provider) : KeyStore.getInstance(type);
		URL url = ResourceUtils.getURL(resource);
		store.load(url.openStream(), (password != null) ? password.toCharArray() : null);
		return store;
	}

}
