/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.ldap.embedded;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.Delimiter;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

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
     * Returns the port number.
     *
     * @return the port number
     */
    public int getPort() {
		return this.port;
	}

	/**
     * Sets the port number for the embedded LDAP server.
     * 
     * @param port the port number to set
     */
    public void setPort(int port) {
		this.port = port;
	}

	/**
     * Returns the credential object associated with this EmbeddedLdapProperties instance.
     *
     * @return the credential object
     */
    public Credential getCredential() {
		return this.credential;
	}

	/**
     * Sets the credential for the EmbeddedLdapProperties.
     * 
     * @param credential the credential to set
     */
    public void setCredential(Credential credential) {
		this.credential = credential;
	}

	/**
     * Returns the list of base DNs.
     *
     * @return the list of base DNs
     */
    public List<String> getBaseDn() {
		return this.baseDn;
	}

	/**
     * Sets the base DNs for the LDAP server.
     * 
     * @param baseDn the list of base DNs to be set
     */
    public void setBaseDn(List<String> baseDn) {
		this.baseDn = baseDn;
	}

	/**
     * Returns the LDIF (LDAP Data Interchange Format) string.
     * 
     * @return the LDIF string
     */
    public String getLdif() {
		return this.ldif;
	}

	/**
     * Sets the LDIF (LDAP Data Interchange Format) for the Embedded LDAP server.
     * 
     * @param ldif the LDIF to be set
     */
    public void setLdif(String ldif) {
		this.ldif = ldif;
	}

	/**
     * Returns the validation object associated with this EmbeddedLdapProperties instance.
     *
     * @return the validation object
     */
    public Validation getValidation() {
		return this.validation;
	}

	/**
     * Credential class.
     */
    public static class Credential {

		/**
		 * Embedded LDAP username.
		 */
		private String username;

		/**
		 * Embedded LDAP password.
		 */
		private String password;

		/**
         * Returns the username associated with this Credential.
         *
         * @return the username
         */
        public String getUsername() {
			return this.username;
		}

		/**
         * Sets the username for the Credential.
         * 
         * @param username the username to be set
         */
        public void setUsername(String username) {
			this.username = username;
		}

		/**
         * Returns the password of the Credential object.
         *
         * @return the password of the Credential object
         */
        public String getPassword() {
			return this.password;
		}

		/**
         * Sets the password for the Credential.
         * 
         * @param password the password to be set
         */
        public void setPassword(String password) {
			this.password = password;
		}

		/**
         * Checks if the username and password are available.
         * 
         * @return true if both the username and password are available, false otherwise
         */
        boolean isAvailable() {
			return StringUtils.hasText(this.username) && StringUtils.hasText(this.password);
		}

	}

	/**
     * Validation class.
     */
    public static class Validation {

		/**
		 * Whether to enable LDAP schema validation.
		 */
		private boolean enabled = true;

		/**
		 * Path to the custom schema.
		 */
		private Resource schema;

		/**
         * Returns the current status of the validation.
         * 
         * @return true if the validation is enabled, false otherwise.
         */
        public boolean isEnabled() {
			return this.enabled;
		}

		/**
         * Sets the enabled status of the validation.
         * 
         * @param enabled the enabled status to be set
         */
        public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		/**
         * Returns the schema resource.
         *
         * @return the schema resource
         */
        public Resource getSchema() {
			return this.schema;
		}

		/**
         * Sets the schema resource for validation.
         * 
         * @param schema the schema resource to be set
         */
        public void setSchema(Resource schema) {
			this.schema = schema;
		}

	}

}
