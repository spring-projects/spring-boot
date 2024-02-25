/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Configuration properties for LDAP.
 *
 * @author Eddú Meléndez
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.ldap")
public class LdapProperties {

	private static final int DEFAULT_PORT = 389;

	/**
	 * LDAP URLs of the server.
	 */
	private String[] urls;

	/**
	 * Base suffix from which all operations should originate.
	 */
	private String base;

	/**
	 * Login username of the server.
	 */
	private String username;

	/**
	 * Login password of the server.
	 */
	private String password;

	/**
	 * Whether read-only operations should use an anonymous environment. Disabled by
	 * default unless a username is set.
	 */
	private Boolean anonymousReadOnly;

	/**
	 * LDAP specification settings.
	 */
	private final Map<String, String> baseEnvironment = new HashMap<>();

	private final Template template = new Template();

	/**
	 * Returns an array of URLs.
	 * @return an array of URLs
	 */
	public String[] getUrls() {
		return this.urls;
	}

	/**
	 * Sets the URLs for the LDAP server.
	 * @param urls an array of strings representing the URLs of the LDAP server
	 */
	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	/**
	 * Returns the base of the LDAP properties.
	 * @return the base of the LDAP properties
	 */
	public String getBase() {
		return this.base;
	}

	/**
	 * Sets the base for LDAP search operations.
	 * @param base the base DN (Distinguished Name) to set
	 */
	public void setBase(String base) {
		this.base = base;
	}

	/**
	 * Returns the username associated with the LDAP properties.
	 * @return the username associated with the LDAP properties
	 */
	public String getUsername() {
		return this.username;
	}

	/**
	 * Sets the username for LDAP authentication.
	 * @param username the username to be set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Returns the password for the LDAP connection.
	 * @return the password for the LDAP connection
	 */
	public String getPassword() {
		return this.password;
	}

	/**
	 * Sets the password for the LDAP connection.
	 * @param password the password to be set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Returns the value of the anonymousReadOnly property.
	 * @return the value of the anonymousReadOnly property
	 */
	public Boolean getAnonymousReadOnly() {
		return this.anonymousReadOnly;
	}

	/**
	 * Sets the flag indicating whether anonymous users are allowed to read LDAP
	 * properties.
	 * @param anonymousReadOnly the flag indicating whether anonymous users are allowed to
	 * read LDAP properties
	 */
	public void setAnonymousReadOnly(Boolean anonymousReadOnly) {
		this.anonymousReadOnly = anonymousReadOnly;
	}

	/**
	 * Returns the base environment of the LDAP properties.
	 * @return the base environment as a map of key-value pairs
	 */
	public Map<String, String> getBaseEnvironment() {
		return this.baseEnvironment;
	}

	/**
	 * Returns the template used by the LdapProperties class.
	 * @return the template used by the LdapProperties class
	 */
	public Template getTemplate() {
		return this.template;
	}

	/**
	 * Determines the URLs for the LDAP server based on the given environment. If the URLs
	 * are not set, it will default to "ldap://localhost" with the determined port.
	 * @param environment the environment to determine the URLs for
	 * @return an array of URLs for the LDAP server
	 */
	public String[] determineUrls(Environment environment) {
		if (ObjectUtils.isEmpty(this.urls)) {
			return new String[] { "ldap://localhost:" + determinePort(environment) };
		}
		return this.urls;
	}

	/**
	 * Determines the port to be used for LDAP connection based on the given environment.
	 * @param environment the environment object containing the LDAP port property
	 * @return the determined LDAP port
	 * @throws IllegalArgumentException if the environment is null
	 */
	private int determinePort(Environment environment) {
		Assert.notNull(environment, "Environment must not be null");
		String localPort = environment.getProperty("local.ldap.port");
		if (localPort != null) {
			return Integer.parseInt(localPort);
		}
		return DEFAULT_PORT;
	}

	/**
	 * {@link LdapTemplate settings}.
	 */
	public static class Template {

		/**
		 * Whether PartialResultException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignorePartialResultException = false;

		/**
		 * Whether NameNotFoundException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignoreNameNotFoundException = false;

		/**
		 * Whether SizeLimitExceededException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignoreSizeLimitExceededException = true;

		/**
		 * Returns whether the partial result exception should be ignored.
		 * @return {@code true} if the partial result exception should be ignored,
		 * {@code false} otherwise
		 */
		public boolean isIgnorePartialResultException() {
			return this.ignorePartialResultException;
		}

		/**
		 * Sets whether to ignore the PartialResultException.
		 * @param ignorePartialResultException true to ignore the PartialResultException,
		 * false otherwise
		 */
		public void setIgnorePartialResultException(boolean ignorePartialResultException) {
			this.ignorePartialResultException = ignorePartialResultException;
		}

		/**
		 * Returns a boolean value indicating whether the NameNotFoundException should be
		 * ignored.
		 * @return true if the NameNotFoundException should be ignored, false otherwise
		 */
		public boolean isIgnoreNameNotFoundException() {
			return this.ignoreNameNotFoundException;
		}

		/**
		 * Sets the flag to ignore NameNotFoundException.
		 * @param ignoreNameNotFoundException the flag indicating whether to ignore
		 * NameNotFoundException
		 */
		public void setIgnoreNameNotFoundException(boolean ignoreNameNotFoundException) {
			this.ignoreNameNotFoundException = ignoreNameNotFoundException;
		}

		/**
		 * Returns a boolean value indicating whether the ignoreSizeLimitExceededException
		 * flag is set.
		 * @return true if the ignoreSizeLimitExceededException flag is set, false
		 * otherwise
		 */
		public boolean isIgnoreSizeLimitExceededException() {
			return this.ignoreSizeLimitExceededException;
		}

		/**
		 * Sets the flag to ignore the SizeLimitExceededException.
		 * @param ignoreSizeLimitExceededException the flag indicating whether to ignore
		 * the SizeLimitExceededException
		 */
		public void setIgnoreSizeLimitExceededException(Boolean ignoreSizeLimitExceededException) {
			this.ignoreSizeLimitExceededException = ignoreSizeLimitExceededException;
		}

	}

}
