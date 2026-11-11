/*
 * Copyright 2012-present the original author or authors.
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
