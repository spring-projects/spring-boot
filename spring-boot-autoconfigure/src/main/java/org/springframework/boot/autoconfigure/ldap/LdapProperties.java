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

package org.springframework.boot.autoconfigure.ldap;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.ldap.core.LdapTemplate;
/**
 * Configuration properties to configure {@link LdapTemplate}.
 *
 * @author Eddú Meléndez
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.ldap")
public class LdapProperties {

	private static final int DEFAULT_PORT = 389;

	/**
	 * LDAP urls.
	 */
	private String[] urls = new String[0];

	/**
	 * Base suffix from which all operations should originate.
	 */
	private String base;

	/**
	 * Login user of the LDAP.
	 */
	private String username;

	/**
	 * Login password of the LDAP.
	 */
	private String password;

	/**
	 * LDAP custom environment properties.
	 */
	private Map<String, String> baseEnvironment = new HashMap<String, String>();

	public String[] getUrls() {
		return this.urls;
	}

	public void setUrls(String[] urls) {
		this.urls = urls;
	}

	public String getBase() {
		return this.base;
	}

	public void setBase(String base) {
		this.base = base;
	}

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

	public Map<String, String> getBaseEnvironment() {
		return this.baseEnvironment;
	}

	public void setBaseEnvironment(Map<String, String> baseEnvironment) {
		this.baseEnvironment = baseEnvironment;
	}

	public String[] determineUrls(Environment environment) {
		if (this.urls.length == 0) {
			String protocol = "ldap://";
			String host = "localhost";
			int port = determinePort(environment);
			String[] ldapUrls = new String[1];
			ldapUrls[0] = protocol + host + ":" + port;
			return ldapUrls;
		}
		return this.urls;
	}

	private int determinePort(Environment environment) {
		if (environment != null) {
			String localPort = environment.getProperty("local.ldap.port");
			if (localPort != null) {
				return Integer.valueOf(localPort);
			}
			else {
				return DEFAULT_PORT;
			}
		}
		throw new IllegalStateException(
				"No local ldap port configuration is available");
	}

}
