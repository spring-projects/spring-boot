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
		super("Failed to invoke operation because the following required "
				+ "parameters were missing: "
				+ StringUtils.collectionToCommaDelimitedString(missingParameters),
				"Missing parameters: "
						+ missingParameters.stream().map(OperationParameter::getName)
								.collect(Collectors.joining(",")));
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
