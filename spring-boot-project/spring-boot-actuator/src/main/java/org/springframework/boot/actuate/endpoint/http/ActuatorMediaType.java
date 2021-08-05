/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.http;

/**
 * Media types that can be consumed and produced by Actuator endpoints.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @since 2.0.0
 * @deprecated since 2.5.0 for removal in 2.7.0 in favor of
 * {@link org.springframework.boot.actuate.endpoint.ApiVersion#getProducedMimeType()}
 */
@Deprecated
public final class ActuatorMediaType {

	/**
	 * Constant for the Actuator {@link ApiVersion#V2 v2} media type.
	 */
	public static final String V2_JSON = "application/vnd.spring-boot.actuator.v2+json";

	/**
	 * Constant for the Actuator {@link ApiVersion#V3 v3} media type.
	 */
	public static final String V3_JSON = "application/vnd.spring-boot.actuator.v3+json";

	private ActuatorMediaType() {
	}

}
