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

package org.springframework.boot.autoconfigure.jmx;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

	public boolean getEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isUniqueNames() {
		return this.uniqueNames;
	}

	public void setUniqueNames(boolean uniqueNames) {
		this.uniqueNames = uniqueNames;
	}

	public String getServer() {
		return this.server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getDefaultDomain() {
		return this.defaultDomain;
	}

	public void setDefaultDomain(String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

}
