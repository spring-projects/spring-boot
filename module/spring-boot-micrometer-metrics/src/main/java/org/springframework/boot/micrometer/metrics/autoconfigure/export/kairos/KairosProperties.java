/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.export.kairos;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.properties.StepRegistryProperties;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for configuring KairosDB
 * metrics export.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("management.kairos.metrics.export")
public class KairosProperties extends StepRegistryProperties {

	/**
	 * URI of the KairosDB server.
	 */
	private String uri = "http://localhost:8080/api/v1/datapoints";

	/**
	 * Login user of the KairosDB server.
	 */
	private @Nullable String userName;

	/**
	 * Login password of the KairosDB server.
	 */
	private @Nullable String password;

	public String getUri() {
		return this.uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public @Nullable String getUserName() {
		return this.userName;
	}

	public void setUserName(@Nullable String userName) {
		this.userName = userName;
	}

	public @Nullable String getPassword() {
		return this.password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

}
