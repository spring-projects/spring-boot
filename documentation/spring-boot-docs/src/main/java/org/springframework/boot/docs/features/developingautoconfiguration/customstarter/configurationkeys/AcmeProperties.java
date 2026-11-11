/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.developingautoconfiguration.customstarter.configurationkeys;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("acme")
public class AcmeProperties {

	/**
	 * Whether to check the location of acme resources.
	 */
	private boolean checkLocation = true;

	/**
	 * Timeout for establishing a connection to the acme server.
	 */
	private Duration loginTimeout = Duration.ofSeconds(3);

	// @fold:on // getters/setters ...
	public boolean isCheckLocation() {
		return this.checkLocation;
	}

	public void setCheckLocation(boolean checkLocation) {
		this.checkLocation = checkLocation;
	}

	public Duration getLoginTimeout() {
		return this.loginTimeout;
	}

	public void setLoginTimeout(Duration loginTimeout) {
		this.loginTimeout = loginTimeout;
	}
	// @fold:off

}
