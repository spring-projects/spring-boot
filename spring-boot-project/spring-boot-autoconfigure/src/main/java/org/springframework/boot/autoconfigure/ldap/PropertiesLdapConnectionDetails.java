/*
 * Copyright 2012-2024 the original author or authors.
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

import org.springframework.core.env.Environment;

/**
 * Adapts {@link LdapProperties} to {@link LdapConnectionDetails}.
 *
 * @author Philipp Kessler
 * @since 3.3.0
 */
public class PropertiesLdapConnectionDetails implements LdapConnectionDetails {

	private final LdapProperties properties;

	private final Environment environment;

	/**
     * Constructs a new instance of PropertiesLdapConnectionDetails with the specified LdapProperties and Environment.
     * 
     * @param properties the LdapProperties object containing the LDAP connection details
     * @param environment the Environment object containing the environment details
     */
    PropertiesLdapConnectionDetails(LdapProperties properties, Environment environment) {
		this.properties = properties;
		this.environment = environment;
	}

	/**
     * Returns an array of URLs determined by the properties and environment.
     * 
     * @return an array of URLs
     */
    @Override
	public String[] getUrls() {
		return this.properties.determineUrls(this.environment);
	}

	/**
     * Returns the base of the LDAP connection details.
     * 
     * @return the base of the LDAP connection details
     */
    @Override
	public String getBase() {
		return this.properties.getBase();
	}

	/**
     * Returns the username associated with this PropertiesLdapConnectionDetails object.
     *
     * @return the username associated with this PropertiesLdapConnectionDetails object
     */
    @Override
	public String getUsername() {
		return this.properties.getUsername();
	}

	/**
     * Returns the password for the LDAP connection.
     * 
     * @return the password for the LDAP connection
     */
    @Override
	public String getPassword() {
		return this.properties.getPassword();
	}

}
