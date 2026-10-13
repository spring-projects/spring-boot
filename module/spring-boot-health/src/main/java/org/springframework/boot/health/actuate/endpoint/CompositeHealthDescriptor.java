/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.boot.health.contributor.Status;
import org.springframework.util.Assert;

/**
 * Description of health that is composed of other {@link HealthDescriptor health
 * descriptors}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public sealed class CompositeHealthDescriptor extends HealthDescriptor permits SystemHealthDescriptor {

	private final ApiVersion apiVersion;

	private final Status status;

	private final @Nullable Map<String, HealthDescriptor> components;

	CompositeHealthDescriptor(ApiVersion apiVersion, Status status,
			@Nullable Map<String, HealthDescriptor> components) {
		Assert.notNull(apiVersion, "'apiVersion' must not be null");
		Assert.notNull(status, "'status' must not be null");
		this.apiVersion = apiVersion;
		this.status = status;
		this.components = (components != null) ? new TreeMap<>(components) : components;
	}

	@Override
	public Status getStatus() {
		return this.status;
	}

	@JsonInclude(Include.NON_EMPTY)
	public @Nullable Map<String, HealthDescriptor> getComponents() {
		return (this.apiVersion == ApiVersion.V3) ? this.components : null;
	}

	@JsonInclude(Include.NON_EMPTY)
	public @Nullable Map<String, HealthDescriptor> getDetails() {
		return (this.apiVersion == ApiVersion.V2) ? this.components : null;
	}

}
