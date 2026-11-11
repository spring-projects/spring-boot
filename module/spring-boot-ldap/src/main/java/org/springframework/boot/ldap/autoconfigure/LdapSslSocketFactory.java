/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import javax.net.SocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.Assert;
import org.springframework.util.function.SingletonSupplier;

/**
 * Bridges the JNDI {@code java.naming.ldap.factory.socket} environment property to an
 * {@link SslBundle}. That property only accepts a class name, which JNDI loads and on
 * which it then calls {@link #getDefault()}, with no way of passing an actual factory
 * instance. The bundle to use is therefore held in a static field.
 * <p>
 * As the bundle is held statically, a single {@link SslBundle} applies JVM-wide. This is
 * sufficient for the auto-configured
 * {@link org.springframework.ldap.core.support.LdapContextSource}, of which there is at
 * most one per application context.
 * <p>
 * This class is referenced by name from a JNDI environment and is not intended to be used
 * directly.
 *
 * @author Moritz Halbritter
 * @since 4.2.0
 */
public final class LdapSslSocketFactory {

	private static final Log logger = LogFactory.getLog(LdapSslSocketFactory.class);

	private static volatile @Nullable BundleSocketFactory current;

	private LdapSslSocketFactory() {
	}

	public static SocketFactory getDefault() {
		BundleSocketFactory current = LdapSslSocketFactory.current;
		Assert.state(current != null, "No SSL bundle has been set");
		return current.socketFactory().obtain();
	}

	static void setSslBundle(@Nullable SslBundle sslBundle) {
		BundleSocketFactory previous = LdapSslSocketFactory.current;
		if (previous != null && previous.sslBundle() == sslBundle) {
			return;
		}
		if (previous != null && sslBundle != null) {
			logger.warn("A different SSL bundle has already been set for LDAP. As the bundle applies JVM-wide, "
					+ "LDAPS connections opened from now on use the key and trust material of the new bundle, "
					+ "including connections from context sources that were configured with the previous one");
		}
		LdapSslSocketFactory.current = (sslBundle != null) ? new BundleSocketFactory(sslBundle) : null;
	}

	/**
	 * Pairs a bundle with the socket factory lazily created from it, so both are replaced
	 * atomically.
	 */
	private record BundleSocketFactory(SslBundle sslBundle, SingletonSupplier<SocketFactory> socketFactory) {

		BundleSocketFactory(SslBundle sslBundle) {
			this(sslBundle, SingletonSupplier.of(() -> sslBundle.createSslContext().getSocketFactory()));
		}

	}

}
