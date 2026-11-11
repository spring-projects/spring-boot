/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.invoke;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.InvalidEndpointRequestException;
import org.springframework.util.StringUtils;

/**
 * {@link RuntimeException} thrown when an endpoint invocation does not contain required
 * parameters.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class MissingParametersException extends InvalidEndpointRequestException {

	private final Set<OperationParameter> missingParameters;

	public MissingParametersException(Set<OperationParameter> missingParameters) {
		super("Failed to invoke operation because the following required parameters were missing: "
				+ StringUtils.collectionToCommaDelimitedString(missingParameters),
				"Missing parameters: "
						+ missingParameters.stream().map(OperationParameter::getName).collect(Collectors.joining(",")));
		this.missingParameters = missingParameters;
	}

	/**
	 * Returns the parameters that were missing.
	 * @return the parameters
	 */
	public Set<OperationParameter> getMissingParameters() {
		return this.missingParameters;
	}

}
