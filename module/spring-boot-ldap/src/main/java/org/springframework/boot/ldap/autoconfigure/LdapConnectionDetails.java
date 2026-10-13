/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.ldap.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.boot.ssl.SslBundle;

/**
 * Details required to establish a connection to an LDAP service.
 *
 * @author Philipp Kessler
 * @since 4.0.0
 */
public interface LdapConnectionDetails extends ConnectionDetails {

	/**
	 * LDAP URLs of the server.
	 * @return the LDAP URLs to use
	 */
	String[] getUrls();

	/**
	 * Base suffix from which all operations should originate.
	 * @return base suffix
	 */
	default @Nullable String getBase() {
		return null;
	}

	/**
	 * Login username of the server.
	 * @return login username
	 */
	default @Nullable String getUsername() {
		return null;
	}

	/**
	 * Login password of the server.
	 * @return login password
	 */
	default @Nullable String getPassword() {
		return null;
	}

	/**
	 * SSL bundle to use to establish the LDAP connection.
	 * @return the SSL bundle to use, or {@code null} if none
	 * @since 4.2.0
	 */
	default @Nullable SslBundle getSslBundle() {
		return null;
	}

}
