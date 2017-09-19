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

package org.springframework.boot.actuate.autoconfigure.endpoint;

/**
 * Determines if an endpoint is enabled or not.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class EndpointEnablement {

	private final boolean enabled;

	private final String reason;

	/**
	 * Creates a new instance.
	 * @param enabled whether or not the endpoint is enabled
	 * @param reason a human readable reason of the decision
	 */
	EndpointEnablement(boolean enabled, String reason) {
		this.enabled = enabled;
		this.reason = reason;
	}

	/**
	 * Return whether or not the endpoint is enabled.
	 * @return {@code true} if the endpoint is enabled, {@code false} otherwise
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * Return a human readable reason of the decision.
	 * @return the reason of the endpoint's enablement
	 */
	public String getReason() {
		return this.reason;
	}

}
