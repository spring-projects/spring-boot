/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.health.actuate.endpoint;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.health.contributor.Status;

/**
 * Description of health including a status.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
public abstract sealed class HealthDescriptor implements OperationResponseBody
		permits IndicatedHealthDescriptor, CompositeHealthDescriptor {

	/**
	 * Return the status of the component.
	 * @return the component status
	 */
	@JsonUnwrapped
	public abstract Status getStatus();

}
