/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertiesLdapConnectionDetails}.
 *
 * @author Moritz Halbritter
 */
class PropertiesLdapConnectionDetailsTests {

	private final LdapProperties properties = new LdapProperties();

	@Test
	void shouldUseDefaultLdapPortWhenSslIsNotEnabled() {
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldap://localhost:389");
	}

	@Test
	void shouldUseDefaultLdapsPortWhenSslIsEnabled() {
		this.properties.getSsl().setEnabled(true);
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldaps://localhost:636");
	}

	@Test
	void shouldUseDefaultLdapsPortWhenSslBundleIsConfigured() {
		this.properties.getSsl().setBundle("example");
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldaps://localhost:636");
	}

	@Test
	void shouldUseConfiguredUrlsRegardlessOfSsl() {
		this.properties.setUrls(new String[] { "ldap://localhost:1234" });
		this.properties.getSsl().setEnabled(true);
		assertThat(createConnectionDetails().getUrls()).containsExactly("ldap://localhost:1234");
	}

	private PropertiesLdapConnectionDetails createConnectionDetails() {
		return new PropertiesLdapConnectionDetails(this.properties, null);
	}

}
