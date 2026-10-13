/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ssl;

import java.security.KeyStore;
import java.security.Provider;

import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.TrustManagerFactorySpi;

/**
 * {@link TrustManagerFactory} which uses a fixed set of {@link TrustManager
 * TrustManagers}.
 *
 * @author Moritz Halbritter
 */
final class FixedTrustManagerFactory extends TrustManagerFactory {

	private static final Provider PROVIDER = new FixedTrustManagerFactoryProvider();

	private FixedTrustManagerFactory(FixedTrustManagersSpi spi, String algorithm) {
		super(spi, PROVIDER, algorithm);
	}

	static FixedTrustManagerFactory of(TrustManagerFactory trustManagerFactory, TrustManager... trustManagers) {
		return new FixedTrustManagerFactory(new FixedTrustManagersSpi(trustManagers),
				trustManagerFactory.getAlgorithm());
	}

	private static final class FixedTrustManagersSpi extends TrustManagerFactorySpi {

		private final TrustManager[] trustManagers;

		private FixedTrustManagersSpi(TrustManager[] trustManagers) {
			this.trustManagers = trustManagers;
		}

		@Override
		protected void engineInit(KeyStore ks) {
		}

		@Override
		protected void engineInit(ManagerFactoryParameters spec) {
		}

		@Override
		protected TrustManager[] engineGetTrustManagers() {
			return this.trustManagers;
		}

	}

	private static class FixedTrustManagerFactoryProvider extends Provider {

		FixedTrustManagerFactoryProvider() {
			super("FixedTrustManagerFactory", SpringBootProviderVersion.get(), "");
		}

	}

}
