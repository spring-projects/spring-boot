/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.reflect;

import java.util.Set;

import org.springframework.util.StringUtils;

/**
 * {@link RuntimeException} thrown when an endpoint invocation does not contain required
 * parameters.
 *
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ParametersMissingException extends RuntimeException {

	private final Set<String> parameters;

	public ParametersMissingException(Set<String> parameters) {
		super("Failed to invoke operation because the following required "
				+ "parameters were missing: "
				+ StringUtils.collectionToCommaDelimitedString(parameters));
		this.parameters = parameters;
	}

	/**
	 * Returns the parameters that were missing.
	 * @return the parameters
	 */
	public Set<String> getParameters() {
		return this.parameters;
	}
}
