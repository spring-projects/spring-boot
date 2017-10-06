/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.ldap.embedded;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for Embedded LDAP.
 *
 * @author Eddú Meléndez
 * @author Mathieu Ouellet
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.ldap.embedded")
public class EmbeddedLdapProperties {

	/**
	 * Embedded LDAP port.
	 */
	private int port = 0;

	/**
	 * Embedded LDAP credentials.
	 */
	private Credential credential = new Credential();

	/**
	 * Base DNs.
	 */
	private String baseDn;

	/**
	 * Schema (LDIF) script resource reference.
	 */
	private String ldif = "classpath:schema.ldif";

	/**
	 * Schema validation.
	 */
	private Validation validation = new Validation();

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

	public String getBaseDn() {
		return this.baseDn;
	}

	public void setBaseDn(String baseDn) {
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

	public static class Credential {

		/**
		 * Embedded LDAP username.
		 */
		private String username;

		/**
		 * Embedded LDAP password.
		 */
		private String password;

		public String getUsername() {
			return this.username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}

	}

	public static class Validation {

		/**
		 * Enable LDAP schema validation.
		 */
		private boolean enabled = true;

		/**
		 * Path to the custom schema.
		 */
		private Resource schema;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Resource getSchema() {
			return this.schema;
		}

		public void setSchema(Resource schema) {
			this.schema = schema;
		}

	}

}
