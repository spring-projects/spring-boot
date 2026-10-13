/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.grpc.server.autoconfigure;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.grpc.TlsServerCredentials.ClientAuth;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.util.Assert;

/**
 * Server credential details to use with gRPC servers.
 *
 * @author Chris Bono
 * @author Phillip Webb
 * @param keyManagerFactory the key manager factory to use or {@code null}
 * @param trustManagerFactory the trust manager factory to use or {@code null}
 * @param clientAuth the client auth to use
 */
record ServerCredentials(@Nullable KeyManagerFactory keyManagerFactory,
		@Nullable TrustManagerFactory trustManagerFactory, ClientAuth clientAuth) {

	/**
	 * Return the credentials to use based on the given properties.
	 * @param properties the SSL properties
	 * @param bundles the SSL bundles
	 * @param insecureTrustManagerFactory the trust manager factory to use for insecure
	 * connections
	 * @return the server credentials to use
	 */
	static ServerCredentials get(GrpcServerProperties.Ssl properties, SslBundles bundles,
			TrustManagerFactory insecureTrustManagerFactory) {
		Boolean enabled = properties.getEnabled();
		String bundle = properties.getBundle();
		ClientAuth clientAuth = properties.getClientAuth();
		if (Boolean.FALSE.equals(enabled) || (enabled == null && bundle == null)) {
			return new ServerCredentials(null, null, clientAuth);
		}
		Assert.state(bundle != null,
				() -> "SSL bundle-name is requested when 'spring.grpc.server.ssl.enabled' is true");
		SslManagerBundle managers = bundles.getBundle(bundle).getManagers();
		KeyManagerFactory keyManagerFactory = managers.getKeyManagerFactory();
		TrustManagerFactory trustManagerFactory = (!properties.isSecure()) ? insecureTrustManagerFactory
				: managers.getTrustManagerFactory();
		return new ServerCredentials(keyManagerFactory, trustManagerFactory, clientAuth);
	}

}
