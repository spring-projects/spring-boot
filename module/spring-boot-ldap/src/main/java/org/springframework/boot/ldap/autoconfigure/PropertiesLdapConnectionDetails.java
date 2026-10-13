/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Adapts {@link LdapProperties} to {@link LdapConnectionDetails}.
 *
 * @author Philipp Kessler
 */
class PropertiesLdapConnectionDetails implements LdapConnectionDetails {

	private final LdapProperties properties;

	private final Environment environment;

	PropertiesLdapConnectionDetails(LdapProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	@Override
	public String[] getUrls() {
		return this.properties.determineUrls(this.environment);
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

}
