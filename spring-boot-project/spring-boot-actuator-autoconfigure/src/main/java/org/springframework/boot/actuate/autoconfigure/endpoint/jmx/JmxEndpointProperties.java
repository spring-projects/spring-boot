/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JMX export of endpoints.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ConfigurationProperties("management.endpoints.jmx")
public class JmxEndpointProperties {

	private final Exposure exposure = new Exposure();

	/**
	 * Endpoints JMX domain name. Fallback to 'spring.jmx.default-domain' if set.
	 */
	private String domain;

	/**
	 * Additional static properties to append to all ObjectNames of MBeans representing
	 * Endpoints.
	 */
	private final Properties staticNames = new Properties();

	/**
     * Returns the exposure of the JmxEndpointProperties.
     *
     * @return the exposure of the JmxEndpointProperties
     */
    public Exposure getExposure() {
		return this.exposure;
	}

	/**
     * Returns the domain of the JMX endpoint.
     *
     * @return the domain of the JMX endpoint
     */
    public String getDomain() {
		return this.domain;
	}

	/**
     * Sets the domain for the JMX endpoint.
     * 
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
		this.domain = domain;
	}

	/**
     * Returns the staticNames property of the JmxEndpointProperties class.
     * 
     * @return the staticNames property
     */
    public Properties getStaticNames() {
		return this.staticNames;
	}

	/**
     * Exposure class.
     */
    public static class Exposure {

		/**
		 * Endpoint IDs that should be included or '*' for all.
		 */
		private Set<String> include = new LinkedHashSet<>();

		/**
		 * Endpoint IDs that should be excluded or '*' for all.
		 */
		private Set<String> exclude = new LinkedHashSet<>();

		/**
         * Returns the set of strings representing the include values.
         *
         * @return the set of strings representing the include values
         */
        public Set<String> getInclude() {
			return this.include;
		}

		/**
         * Sets the include set.
         * 
         * @param include the set of strings to be included
         */
        public void setInclude(Set<String> include) {
			this.include = include;
		}

		/**
         * Returns the set of excluded strings.
         *
         * @return the set of excluded strings
         */
        public Set<String> getExclude() {
			return this.exclude;
		}

		/**
         * Sets the set of strings to exclude.
         * 
         * @param exclude the set of strings to exclude
         */
        public void setExclude(Set<String> exclude) {
			this.exclude = exclude;
		}

	}

}
