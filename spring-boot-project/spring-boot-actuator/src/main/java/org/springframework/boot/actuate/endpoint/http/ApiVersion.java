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

import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;

/**
 * API versions supported for the actuator HTTP API. This enum may be injected into
 * actuator endpoints in order to return a response compatible with the requested version.
 *
 * @author Phillip Webb
 * @since 2.2.0
 */
public enum ApiVersion {

	/**
	 * Version 2 (supported by Spring Boot 2.0+).
	 */
	V2,

	/**
	 * Version 3 (supported by Spring Boot 2.2+).
	 */
	V3;

	private static final String MEDIA_TYPE_PREFIX = "application/vnd.spring-boot.actuator.";

	/**
	 * The latest API version.
	 */
	public static final ApiVersion LATEST = ApiVersion.V3;

	/**
	 * Return the {@link ApiVersion} to use based on the HTTP request headers. The version
	 * will be deduced based on the {@code Accept} header.
	 * @param headers the HTTP headers
	 * @return the API version to use
	 */
	public static ApiVersion fromHttpHeaders(Map<String, List<String>> headers) {
		ApiVersion version = null;
		List<String> accepts = headers.get("Accept");
		if (!CollectionUtils.isEmpty(accepts)) {
			for (String accept : accepts) {
				for (String type : MimeTypeUtils.tokenize(accept)) {
					version = mostRecent(version, forType(type));
				}
			}
		}
		return (version != null) ? version : LATEST;
	}

	private static ApiVersion forType(String type) {
		if (type.startsWith(MEDIA_TYPE_PREFIX)) {
			type = type.substring(MEDIA_TYPE_PREFIX.length());
			int suffixIndex = type.indexOf("+");
			type = (suffixIndex != -1) ? type.substring(0, suffixIndex) : type;
			try {
				return valueOf(type.toUpperCase());
			}
			catch (IllegalArgumentException ex) {
			}
		}
		return null;
	}

	private static ApiVersion mostRecent(ApiVersion existing, ApiVersion candidate) {
		int existingOrdinal = (existing != null) ? existing.ordinal() : -1;
		int candidateOrdinal = (candidate != null) ? candidate.ordinal() : -1;
		return (candidateOrdinal > existingOrdinal) ? candidate : existing;
	}

}
