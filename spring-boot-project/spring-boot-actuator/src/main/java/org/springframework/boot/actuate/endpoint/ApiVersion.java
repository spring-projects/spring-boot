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

package org.springframework.boot.actuate.endpoint;

import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

/**
 * API versions supported for the actuator API. This enum may be injected into actuator
 * endpoints in order to return a response compatible with the requested version.
 *
 * @author Phillip Webb
 * @since 2.4.0
 */
public enum ApiVersion implements Producible<ApiVersion> {

	/**
	 * Version 2 (supported by Spring Boot 2.0+).
	 */
	V2("application/vnd.spring-boot.actuator.v2+json"),

	/**
	 * Version 3 (supported by Spring Boot 2.2+).
	 */
	V3("application/vnd.spring-boot.actuator.v3+json");

	/**
	 * The latest API version.
	 */
	public static final ApiVersion LATEST = ApiVersion.V3;

	private final MimeType mimeType;

	ApiVersion(String mimeType) {
		this.mimeType = MimeTypeUtils.parseMimeType(mimeType);
	}

	@Override
	public MimeType getProducedMimeType() {
		return this.mimeType;
	}

}
