/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.jmx;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.jmx.support.RegistrationPolicy;

/**
 * Configuration properties for JMX.
 *
 * @author Scott Frederick
 * @since 2.7.0
 */
@ConfigurationProperties("spring.jmx")
public class JmxProperties {

	/**
	 * Expose Spring's management beans to the JMX domain.
	 */
	private boolean enabled;

	/**
	 * Whether unique runtime object names should be ensured.
	 */
	private boolean uniqueNames;

	/**
	 * MBeanServer bean name.
	 */
	private String server = "mbeanServer";

	/**
	 * JMX domain name.
	 */
	private @Nullable String defaultDomain;

	/**
	 * JMX Registration policy.
	 */
	private RegistrationPolicy registrationPolicy = RegistrationPolicy.FAIL_ON_EXISTING;

	public boolean isEnabled() {
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

	public @Nullable String getDefaultDomain() {
		return this.defaultDomain;
	}

	public void setDefaultDomain(@Nullable String defaultDomain) {
		this.defaultDomain = defaultDomain;
	}

	public RegistrationPolicy getRegistrationPolicy() {
		return this.registrationPolicy;
	}

	public void setRegistrationPolicy(RegistrationPolicy registrationPolicy) {
		this.registrationPolicy = registrationPolicy;
	}

}
