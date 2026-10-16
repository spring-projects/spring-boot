/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import javax.net.ssl.SSLContext;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundleKey;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.boot.ssl.SslManagerBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.SslStoreBundle;

/**
 * An {@link SslBundle} that resolves the currently registered bundle on each access.
 *
 * @param bundles the registry to resolve from
 * @param name the name the bundle is registered under
 * @author Henning Kvinnesland
 */
record LiveSslBundle(SslBundles bundles, String name) implements SslBundle {

	private SslBundle current() {
		return this.bundles.getBundle(this.name);
	}

	@Override
	public SslStoreBundle getStores() {
		return current().getStores();
	}

	@Override
	public SslBundleKey getKey() {
		return current().getKey();
	}

	@Override
	public SslOptions getOptions() {
		return current().getOptions();
	}

	@Override
	public String getProtocol() {
		return current().getProtocol();
	}

	@Override
	public SslManagerBundle getManagers() {
		return current().getManagers();
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Resolves the registered bundle once so that the context is built from a single
	 * coherent bundle. The inherited default calls {@link #getManagers()} and
	 * {@link #getProtocol()} separately, which a concurrent reload could fall between.
	 */
	@Override
	public SSLContext createSslContext() {
		return current().createSslContext();
	}

	@Override
	public String toString() {
		return current().toString();
	}

}
