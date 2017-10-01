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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsUtils;

/**
 * Security interceptor to validate the cloud foundry token.
 *
 * @author Madhura Bhave
 */
class CloudFoundrySecurityInterceptor {

	private static final Log logger = LogFactory
			.getLog(CloudFoundrySecurityInterceptor.class);

	private final TokenValidator tokenValidator;

	private final CloudFoundrySecurityService cloudFoundrySecurityService;

	private final String applicationId;

	private static SecurityResponse SUCCESS = SecurityResponse.success();

	CloudFoundrySecurityInterceptor(TokenValidator tokenValidator,
			CloudFoundrySecurityService cloudFoundrySecurityService,
			String applicationId) {
		this.tokenValidator = tokenValidator;
		this.cloudFoundrySecurityService = cloudFoundrySecurityService;
		this.applicationId = applicationId;
	}

	SecurityResponse preHandle(HttpServletRequest request, String endpointId) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return SecurityResponse.success();
		}
		try {
			if (!StringUtils.hasText(this.applicationId)) {
				throw new CloudFoundryAuthorizationException(
						CloudFoundryAuthorizationException.Reason.SERVICE_UNAVAILABLE,
						"Application id is not available");
			}
			if (this.cloudFoundrySecurityService == null) {
				throw new CloudFoundryAuthorizationException(
						CloudFoundryAuthorizationException.Reason.SERVICE_UNAVAILABLE,
						"Cloud controller URL is not available");
			}
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				return SUCCESS;
			}
			check(request, endpointId);
		}
		catch (Exception ex) {
			logger.error(ex);
			if (ex instanceof CloudFoundryAuthorizationException) {
				CloudFoundryAuthorizationException cfException = (CloudFoundryAuthorizationException) ex;
				return new SecurityResponse(cfException.getStatusCode(),
						"{\"security_error\":\"" + cfException.getMessage() + "\"}");
			}
			return new SecurityResponse(HttpStatus.INTERNAL_SERVER_ERROR,
					ex.getMessage());
		}
		return SecurityResponse.success();
	}

	private void check(HttpServletRequest request, String path) throws Exception {
		Token token = getToken(request);
		this.tokenValidator.validate(token);
		AccessLevel accessLevel = this.cloudFoundrySecurityService
				.getAccessLevel(token.toString(), this.applicationId);
		if (!accessLevel.isAccessAllowed(path)) {
			throw new CloudFoundryAuthorizationException(
					CloudFoundryAuthorizationException.Reason.ACCESS_DENIED,
					"Access denied");
		}
		accessLevel.put(request);
	}

	private Token getToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		String bearerPrefix = "bearer ";
		if (authorization == null
				|| !authorization.toLowerCase().startsWith(bearerPrefix)) {
			throw new CloudFoundryAuthorizationException(
					CloudFoundryAuthorizationException.Reason.MISSING_AUTHORIZATION,
					"Authorization header is missing or invalid");
		}
		return new Token(authorization.substring(bearerPrefix.length()));
	}

	/**
	 * Response from the security interceptor.
	 */
	static class SecurityResponse {

		private final HttpStatus status;

		private final String message;

		SecurityResponse(HttpStatus status) {
			this(status, null);
		}

		SecurityResponse(HttpStatus status, String message) {
			this.status = status;
			this.message = message;
		}

		public HttpStatus getStatus() {
			return this.status;
		}

		public String getMessage() {
			return this.message;
		}

		static SecurityResponse success() {
			return new SecurityResponse(HttpStatus.OK);
		}

	}

}
