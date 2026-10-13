/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.ReactiveHealthIndicator;
import org.springframework.boot.health.contributor.Status;

/**
 * Description of health obtained from a {@link HealthIndicator} or
 * {@link ReactiveHealthIndicator}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public final class IndicatedHealthDescriptor extends HealthDescriptor {

	static final IndicatedHealthDescriptor UP = new IndicatedHealthDescriptor(Health.up().build());

	private final Health health;

	IndicatedHealthDescriptor(Health health) {
		this.health = health;
	}

	@Override
	public Status getStatus() {
		return this.health.getStatus();
	}

	@JsonInclude(Include.NON_EMPTY)
	public Map<String, Object> getDetails() {
		return this.health.getDetails();
	}

}
