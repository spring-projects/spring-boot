/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * Exposed via JMX endpoint.
	 */
	JMX("*"),

	/**
	 * Exposed via a web endpoint.
	 */
	WEB("health"),

	/**
	 * Exposed on Cloud Foundry via `/cloudfoundryapplication`.
	 * @since 2.6.4
	 */
	CLOUD_FOUNDRY("*");

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
