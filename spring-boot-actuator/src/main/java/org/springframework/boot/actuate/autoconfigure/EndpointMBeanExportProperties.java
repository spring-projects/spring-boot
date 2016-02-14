/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Configuration properties for JMX.
 *
 * @author Christian Dupuis
 */
@ConfigurationProperties(prefix = "endpoints.jmx")
public class EndpointMBeanExportProperties {

	/**
	 * JMX domain name. Initialized with the value of 'spring.jmx.default-domain' if set.
	 */
	@Value("${spring.jmx.default-domain:}")
	private String domain;

	/**
	 * Ensure that ObjectNames are modified in case of conflict.
	 */
	private boolean uniqueNames = false;

	/**
	 * Enable the JMX endpoints.
	 */
	private boolean enabled = true;

	/**
	 * Additional static properties to append to all ObjectNames of MBeans representing
	 * Endpoints.
	 */
	private Properties staticNames = new Properties();

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getDomain() {
		return this.domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public boolean isUniqueNames() {
		return this.uniqueNames;
	}

	public void setUniqueNames(boolean uniqueNames) {
		this.uniqueNames = uniqueNames;
	}

	public Properties getStaticNames() {
		return this.staticNames;
	}

	public void setStaticNames(String[] staticNames) {
		this.staticNames = StringUtils.splitArrayElementsIntoProperties(staticNames, "=");
	}
}
