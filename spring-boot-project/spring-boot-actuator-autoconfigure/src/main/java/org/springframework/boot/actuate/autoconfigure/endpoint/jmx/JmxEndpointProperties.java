/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

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
	private String domain = "org.springframework.boot";

	/**
	 * Whether to ensure that ObjectNames are modified in case of conflict.
	 */
	private boolean uniqueNames = false;

	/**
	 * Additional static properties to append to all ObjectNames of MBeans representing
	 * Endpoints.
	 */
	private final Properties staticNames = new Properties();

	public JmxEndpointProperties(Environment environment) {
		String defaultDomain = environment.getProperty("spring.jmx.default-domain");
		if (StringUtils.hasText(defaultDomain)) {
			this.domain = defaultDomain;
		}
	}

	public Exposure getExposure() {
		return this.exposure;
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

	public static class Exposure {

		/**
		 * Endpoint IDs that should be included or '*' for all.
		 */
		private Set<String> include = new LinkedHashSet<>();

		/**
		 * Endpoint IDs that should be excluded.
		 */
		private Set<String> exclude = new LinkedHashSet<>();

		public Set<String> getInclude() {
			return this.include;
		}

		public void setInclude(Set<String> include) {
			this.include = include;
		}

		public Set<String> getExclude() {
			return this.exclude;
		}

		public void setExclude(Set<String> exclude) {
			this.exclude = exclude;
		}

	}

}
