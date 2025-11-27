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

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.Delimiter;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for Embedded LDAP.
 *
 * @author Eddú Meléndez
 * @author Mathieu Ouellet
 * @since 4.0.0
 */
@ConfigurationProperties("spring.ldap.embedded")
public class EmbeddedLdapProperties {

	/**
	 * Embedded LDAP port.
	 */
	private int port;

	/**
	 * Embedded LDAP credentials.
	 */
	private Credential credential = new Credential();

	/**
	 * List of base DNs.
	 */
	@Delimiter(Delimiter.NONE)
	private List<String> baseDn = new ArrayList<>();

	/**
	 * Schema (LDIF) script resource reference.
	 */
	private String ldif = "classpath:schema.ldif";

	/**
	 * Schema validation.
	 */
	private final Validation validation = new Validation();

	/**
	 * SSL configuration.
	 */
	private final Ssl ssl = new Ssl();

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Credential getCredential() {
		return this.credential;
	}

	public void setCredential(Credential credential) {
		this.credential = credential;
	}

	public List<String> getBaseDn() {
		return this.baseDn;
	}

	public void setBaseDn(List<String> baseDn) {
		this.baseDn = baseDn;
	}

	public String getLdif() {
		return this.ldif;
	}

	public void setLdif(String ldif) {
		this.ldif = ldif;
	}

	public Validation getValidation() {
		return this.validation;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public static class Credential {

		/**
		 * Embedded LDAP username.
		 */
		private @Nullable String username;

		/**
		 * Embedded LDAP password.
		 */
		private @Nullable String password;

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

		boolean isAvailable() {
			return StringUtils.hasText(this.username) && StringUtils.hasText(this.password);
		}

	}

	public static class Ssl {

		private static final String SUN_X509 = "SunX509";

		private static final String DEFAULT_PROTOCOL;

		static {
			String protocol = "TLSv1.1";
			try {
				String[] protocols = SSLContext.getDefault().getSupportedSSLParameters().getProtocols();
				for (String prot : protocols) {
					if ("TLSv1.2".equals(prot)) {
						protocol = "TLSv1.2";
						break;
					}
				}
			}
			catch (NoSuchAlgorithmException ex) {
				// nothing
			}
			DEFAULT_PROTOCOL = protocol;
		}

		/**
		 * Whether to enable SSL support.
		 */
		private Boolean enabled = false;

		/**
		 * SSL bundle name.
		 */
		private @Nullable String bundle;

		/**
		 * Path to the key store that holds the SSL certificate.
		 */
		private @Nullable String keyStore;

		/**
		 * Key store type.
		 */
		private String keyStoreType = "PKCS12";

		/**
		 * Password used to access the key store.
		 */
		private @Nullable String keyStorePassword;

		/**
		 * Key store algorithm.
		 */
		private String keyStoreAlgorithm = SUN_X509;

		/**
		 * Trust store that holds SSL certificates.
		 */
		private @Nullable String trustStore;

		/**
		 * Trust store type.
		 */
		private String trustStoreType = "JKS";

		/**
		 * Password used to access the trust store.
		 */
		private @Nullable String trustStorePassword;

		/**
		 * Trust store algorithm.
		 */
		private String trustStoreAlgorithm = SUN_X509;

		/**
		 * SSL algorithm to use.
		 */
		private String algorithm = DEFAULT_PROTOCOL;

		public Boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable String getBundle() {
			return this.bundle;
		}

		public void setBundle(@Nullable String bundle) {
			this.bundle = bundle;
		}

		public @Nullable String getKeyStore() {
			return this.keyStore;
		}

		public void setKeyStore(@Nullable String keyStore) {
			this.keyStore = keyStore;
		}

		public String getKeyStoreType() {
			return this.keyStoreType;
		}

		public void setKeyStoreType(String keyStoreType) {
			this.keyStoreType = keyStoreType;
		}

		public @Nullable String getKeyStorePassword() {
			return this.keyStorePassword;
		}

		public void setKeyStorePassword(@Nullable String keyStorePassword) {
			this.keyStorePassword = keyStorePassword;
		}

		public String getKeyStoreAlgorithm() {
			return this.keyStoreAlgorithm;
		}

		public void setKeyStoreAlgorithm(String keyStoreAlgorithm) {
			this.keyStoreAlgorithm = keyStoreAlgorithm;
		}

		public @Nullable String getTrustStore() {
			return this.trustStore;
		}

		public void setTrustStore(@Nullable String trustStore) {
			this.trustStore = trustStore;
		}

		public String getTrustStoreType() {
			return this.trustStoreType;
		}

		public void setTrustStoreType(String trustStoreType) {
			this.trustStoreType = trustStoreType;
		}

		public @Nullable String getTrustStorePassword() {
			return this.trustStorePassword;
		}

		public void setTrustStorePassword(@Nullable String trustStorePassword) {
			this.trustStorePassword = trustStorePassword;
		}

		public String getTrustStoreAlgorithm() {
			return this.trustStoreAlgorithm;
		}

		public void setTrustStoreAlgorithm(String trustStoreAlgorithm) {
			this.trustStoreAlgorithm = trustStoreAlgorithm;
		}

		public String getAlgorithm() {
			return this.algorithm;
		}

		public void setAlgorithm(String sslAlgorithm) {
			this.algorithm = sslAlgorithm;
		}

	}

	public static class Validation {

		/**
		 * Whether to enable LDAP schema validation.
		 */
		private boolean enabled = true;

		/**
		 * Path to the custom schema.
		 */
		private @Nullable Resource schema;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public @Nullable Resource getSchema() {
			return this.schema;
		}

		public void setSchema(@Nullable Resource schema) {
			this.schema = schema;
		}

	}

}
