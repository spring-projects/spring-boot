/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.ldap.autoconfigure.embedded;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.ldap.autoconfigure.LdapConnectionDetails;
import org.springframework.boot.ldap.autoconfigure.LdapProperties;
import org.springframework.boot.ldap.autoconfigure.embedded.EmbeddedLdapProperties.Ssl;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link LdapConnectionDetails} for the embedded LDAP server. Everything that describes
 * the connection comes from {@code spring.ldap.embedded}, as only the server can decide
 * what a connection to it looks like: the URL from the port it is listening on and its
 * SSL configuration, the credentials from {@code spring.ldap.embedded.credential}. The
 * equivalent client properties, {@code spring.ldap.urls}, {@code spring.ldap.username},
 * {@code spring.ldap.password} and {@code spring.ldap.ssl}, are ignored, so a
 * configuration meant for production does not have to be unset to run against the
 * embedded server.
 *
 * @author Moritz Halbritter
 */
class EmbeddedLdapConnectionDetails implements LdapConnectionDetails {

	private static final String SOCKET_FACTORY_ENV_KEY = "java.naming.ldap.factory.socket";

	private final Environment environment;

	private final LdapProperties properties;

	private final EmbeddedLdapProperties embeddedProperties;

	private final @Nullable SslBundles sslBundles;

	EmbeddedLdapConnectionDetails(Environment environment, LdapProperties properties,
			EmbeddedLdapProperties embeddedProperties, @Nullable SslBundles sslBundles) {
		this.environment = environment;
		this.properties = properties;
		this.embeddedProperties = embeddedProperties;
		this.sslBundles = sslBundles;
	}

	@Override
	public String[] getUrls() {
		String protocol = this.embeddedProperties.getSsl().isEnabled() ? "ldaps" : "ldap";
		return new String[] { protocol + "://localhost:" + this.environment.getRequiredProperty("local.ldap.port") };
	}

	@Override
	public @Nullable String getBase() {
		return this.properties.getBase();
	}

	@Override
	public @Nullable String getUsername() {
		return hasCredential() ? this.embeddedProperties.getCredential().getUsername() : null;
	}

	@Override
	public @Nullable String getPassword() {
		return hasCredential() ? this.embeddedProperties.getCredential().getPassword() : null;
	}

	private boolean hasCredential() {
		return StringUtils.hasText(this.embeddedProperties.getCredential().getUsername())
				&& StringUtils.hasText(this.embeddedProperties.getCredential().getPassword());
	}

	@Override
	public @Nullable SslBundle getSslBundle() {
		Ssl serverSsl = this.embeddedProperties.getSsl();
		if (!serverSsl.isEnabled()) {
			return null;
		}
		String bundle = serverSsl.getBundle();
		if (bundle == null) {
			return null;
		}
		assertNoSocketFactoryInBaseEnvironment();
		Assert.notNull(this.sslBundles, "SSL bundle name has been set but no SSL bundles found in context");
		return this.sslBundles.getBundle(bundle);
	}

	/**
	 * Rejects a socket factory in the base environment that the SSL bundle's socket
	 * factory would replace, as both come from properties and so contradict each other.
	 * Called only when a bundle is actually used, as nothing is replaced otherwise.
	 */
	private void assertNoSocketFactoryInBaseEnvironment() {
		Assert.state(!this.properties.getBaseEnvironment().containsKey(SOCKET_FACTORY_ENV_KEY),
				() -> "SSL bundle has been configured but '" + SOCKET_FACTORY_ENV_KEY
						+ "' has also been set in the base environment. Use either an SSL bundle or your own socket factory, not both");
	}

}
