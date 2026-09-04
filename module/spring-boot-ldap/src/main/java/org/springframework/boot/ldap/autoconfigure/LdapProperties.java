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

package org.springframework.boot.ldap.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.ldap.ReferralException;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for LDAP.
 *
 * @author Eddú Meléndez
 * @since 4.0.0
 */
@ConfigurationProperties("spring.ldap")
public class LdapProperties {

	/**
	 * LDAP URLs of the server.
	 */
	private String @Nullable [] urls;

	/**
	 * Base suffix from which all operations should originate.
	 */
	private @Nullable String base;

	/**
	 * Login username of the server.
	 */
	private @Nullable String username;

	/**
	 * Login password of the server.
	 */
	private @Nullable String password;

	/**
	 * Whether read-only operations should use an anonymous environment. Disabled by
	 * default unless a username is set.
	 */
	private @Nullable Boolean anonymousReadOnly;

	/**
	 * Specify how referrals encountered by the service provider are to be processed. If
	 * not specified, the default is determined by the provider.
	 */
	private @Nullable Referral referral;

	/**
	 * LDAP specification settings.
	 */
	private final Map<String, String> baseEnvironment = new LinkedHashMap<>();

	private final Template template = new Template();

	private final Ssl ssl = new Ssl();

	public String @Nullable [] getUrls() {
		return this.urls;
	}

	public void setUrls(String @Nullable [] urls) {
		this.urls = urls;
	}

	public @Nullable String getBase() {
		return this.base;
	}

	public void setBase(@Nullable String base) {
		this.base = base;
	}

	public @Nullable String getUsername() {
		return this.username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public @Nullable Boolean getAnonymousReadOnly() {
		return this.anonymousReadOnly;
	}

	public void setAnonymousReadOnly(@Nullable Boolean anonymousReadOnly) {
		this.anonymousReadOnly = anonymousReadOnly;
	}

	public @Nullable Referral getReferral() {
		return this.referral;
	}

	public void setReferral(@Nullable Referral referral) {
		this.referral = referral;
	}

	public Map<String, String> getBaseEnvironment() {
		return this.baseEnvironment;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	/**
	 * {@link LdapTemplate settings}.
	 */
	public static class Template {

		/**
		 * Whether PartialResultException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignorePartialResultException;

		/**
		 * Whether NameNotFoundException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignoreNameNotFoundException;

		/**
		 * Whether SizeLimitExceededException should be ignored in searches through the
		 * LdapTemplate.
		 */
		private boolean ignoreSizeLimitExceededException = true;

		public boolean isIgnorePartialResultException() {
			return this.ignorePartialResultException;
		}

		public void setIgnorePartialResultException(boolean ignorePartialResultException) {
			this.ignorePartialResultException = ignorePartialResultException;
		}

		public boolean isIgnoreNameNotFoundException() {
			return this.ignoreNameNotFoundException;
		}

		public void setIgnoreNameNotFoundException(boolean ignoreNameNotFoundException) {
			this.ignoreNameNotFoundException = ignoreNameNotFoundException;
		}

		public boolean isIgnoreSizeLimitExceededException() {
			return this.ignoreSizeLimitExceededException;
		}

		public void setIgnoreSizeLimitExceededException(Boolean ignoreSizeLimitExceededException) {
			this.ignoreSizeLimitExceededException = ignoreSizeLimitExceededException;
		}

	}

	/**
	 * SSL configuration.
	 */
	public static class Ssl {

		/**
		 * Whether to enable SSL support. Enabled automatically if "bundle" is provided
		 * unless specified otherwise.
		 */
		private @Nullable Boolean enabled;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		public boolean isEnabled() {
			return (this.enabled != null) ? this.enabled : StringUtils.hasText(this.bundle);
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

	}

	/**
	 * Define the methods to handle referrals.
	 */
	public enum Referral {

		/**
		 * Follow referrals automatically.
		 */
		FOLLOW,

		/**
		 * Ignore referrals.
		 */
		IGNORE,

		/**
		 * Throw {@link ReferralException} when a referral is encountered.
		 */
		THROW

	}

}
