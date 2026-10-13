/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint.expose;

/**
 * Technologies that can be used to expose an endpoint.
 *
 * @author Phillip Webb
 * @since 2.6.0
 */
public enum EndpointExposure {

	/**
	 * Exposed over a JMX endpoint.
	 */
	JMX("health"),

	/**
	 * Exposed over a web endpoint.
	 */
	WEB("health");

	private final String[] defaultIncludes;

	EndpointExposure(String... defaultIncludes) {
		this.defaultIncludes = defaultIncludes;
	}

	/**
	 * Return the default set of include patterns.
	 * @return the default includes
	 */
	public String[] getDefaultIncludes() {
		return this.defaultIncludes;
	}

}
