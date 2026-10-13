/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Adapts {@link LdapProperties} to {@link LdapConnectionDetails}.
 *
 * @author Philipp Kessler
 * @author Moritz Halbritter
 */
class PropertiesLdapConnectionDetails implements LdapConnectionDetails {

	private static final int DEFAULT_PORT = 389;

	private static final int DEFAULT_SSL_PORT = 636;

	private final LdapProperties properties;

	private final @Nullable SslBundles sslBundles;

	PropertiesLdapConnectionDetails(LdapProperties properties, @Nullable SslBundles sslBundles) {
		this.properties = properties;
		this.sslBundles = sslBundles;
		registerSslBundleUpdateHandler();
	}

	/**
	 * Keeps {@link LdapSslSocketFactory} up-to-date when the configured bundle is
	 * reloaded. Without this, the bundle resolved when the context source was created is
	 * held on to and reloaded key or trust material is never used.
	 */
	private void registerSslBundleUpdateHandler() {
		LdapProperties.Ssl ssl = this.properties.getSsl();
		if (this.sslBundles == null || !ssl.isEnabled() || !StringUtils.hasLength(ssl.getBundle())) {
			return;
		}
		this.sslBundles.addBundleUpdateHandler(ssl.getBundle(), LdapSslSocketFactory::updateSslBundle);
	}

	@Override
	public String[] getUrls() {
		String[] urls = this.properties.getUrls();
		if (!ObjectUtils.isEmpty(urls)) {
			return urls;
		}
		boolean useSsl = this.properties.getSsl().isEnabled();
		String protocol = useSsl ? "ldaps" : "ldap";
		int port = useSsl ? DEFAULT_SSL_PORT : DEFAULT_PORT;
		return new String[] { protocol + "://localhost:" + port };
	}

	@Override
	public @Nullable String getBase() {
		return this.properties.getBase();
	}

	@Override
	public @Nullable String getUsername() {
		return this.properties.getUsername();
	}

	@Override
	public @Nullable String getPassword() {
		return this.properties.getPassword();
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		LdapProperties.Ssl ssl = this.properties.getSsl();
		if (!ssl.isEnabled()) {
			return null;
		}
		if (StringUtils.hasLength(ssl.getBundle())) {
			Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
			return this.sslBundles.getBundle(ssl.getBundle());
		}
		return SslBundle.systemDefault();
	}

}
