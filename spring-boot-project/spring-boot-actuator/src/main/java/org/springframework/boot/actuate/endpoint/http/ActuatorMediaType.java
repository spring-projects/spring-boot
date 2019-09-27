/*
 * Copyright 2012-2019 the original author or authors.
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
 */
public final class ActuatorMediaType {

	/**
	 * Constant for the Actuator V1 media type.
	 * @deprecated since 2.2.0 as the v1 format is no longer supported
	 */
	@Deprecated
	public static final String V1_JSON = "application/vnd.spring-boot.actuator.v1+json";

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
