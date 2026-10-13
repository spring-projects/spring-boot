/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.jmx;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

import org.jspecify.annotations.Nullable;

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
	private @Nullable String domain;

	/**
	 * Additional static properties to append to all ObjectNames of MBeans representing
	 * Endpoints.
	 */
	private final Properties staticNames = new Properties();

	public Exposure getExposure() {
		return this.exposure;
	}

	public @Nullable String getDomain() {
		return this.domain;
	}

	public void setDomain(@Nullable String domain) {
		this.domain = domain;
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
		 * Endpoint IDs that should be excluded or '*' for all.
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
