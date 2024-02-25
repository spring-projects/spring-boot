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

package org.springframework.boot.autoconfigure.jmx;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Configuration properties for JMX.
 *
 * @author Scott Frederick
 * @since 2.7.0
 */
@ConfigurationProperties(prefix = "spring.jmx")
public class JmxProperties {

	/**
	 * Expose management beans to the JMX domain.
	 */
	private boolean enabled = false;

	/**
	 * Whether unique runtime object names should be ensured.
	 */
	private boolean uniqueNames = false;

	/**
	 * MBeanServer bean name.
	 */
	private String server = "mbeanServer";

	/**
	 * JMX domain name.
	 */
	private String defaultDomain;

	/**
	 * JMX Registration policy.
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;

	/**
	 * Returns the value of the enabled property.
	 * @return true if the property is enabled, false otherwise.
	 */
	public boolean getEnabled() {
		return this.enabled;
	}

	/**
	 * Sets the enabled status of the JmxProperties.
	 * @param enabled the enabled status to be set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Returns a boolean value indicating whether the names are unique.
	 * @return true if the names are unique, false otherwise.
	 */
	public boolean isUniqueNames() {
		return this.uniqueNames;
	}

	/**
	 * Sets the flag indicating whether unique names should be used.
	 * @param uniqueNames the flag indicating whether unique names should be used
	 */
	public void setUniqueNames(boolean uniqueNames) {
		this.uniqueNames = uniqueNames;
	}

	/**
	 * Returns the server value.
	 * @return the server value
	 */
	public String getServer() {
		return this.server;
	}

	/**
	 * Sets the server for JMX properties.
	 * @param server the server to be set
	 */
	public void setServer(String server) {
		this.server = server;
	}

	/**
	 * Returns the default domain.
	 * @return the default domain
	 */
	public String getDefaultDomain() {
		return this.defaultDomain;
	}

	/**
	 * Sets the default domain for JMX properties.
	 * @param defaultDomain the default domain to be set
	 */
	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	/**
	 * Returns the registration policy of this JmxProperties object.
	 * @return the registration policy
	 */
	public RegistrationPolicy getRegistrationPolicy() {
		return this.registrationPolicy;
	}

	/**
	 * Sets the registration policy for the JmxProperties.
	 * @param registrationPolicy the registration policy to be set
	 */
	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		this.registrationPolicy = registrationPolicy;
	}

}
