/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.SecurityResponse;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.Token;
import org.springframework.boot.actuate.endpoint.EndpointId;
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

	private static final Log logger = LogFactory.getLog(CloudFoundrySecurityInterceptor.class);

	private final TokenValidator tokenValidator;

	private final CloudFoundrySecurityService cloudFoundrySecurityService;

	private final String applicationId;

	private static final SecurityResponse SUCCESS = SecurityResponse.success();

	/**
	 * Constructs a new CloudFoundrySecurityInterceptor with the specified TokenValidator,
	 * CloudFoundrySecurityService, and applicationId.
	 * @param tokenValidator the TokenValidator used for validating tokens
	 * @param cloudFoundrySecurityService the CloudFoundrySecurityService used for
	 * handling security operations
	 * @param applicationId the ID of the application
	 */
	CloudFoundrySecurityInterceptor(TokenValidator tokenValidator,
			CloudFoundrySecurityService cloudFoundrySecurityService, String applicationId) {
		this.tokenValidator = tokenValidator;
		this.cloudFoundrySecurityService = cloudFoundrySecurityService;
		this.applicationId = applicationId;
	}

	/**
	 * Pre-handle method for CloudFoundrySecurityInterceptor class.
	 *
	 * This method is responsible for handling the security checks before processing the
	 * request. It checks if the request is a pre-flight request and returns success if it
	 * is. It then checks if the application id and cloud controller URL are available,
	 * and throws a CloudFoundryAuthorizationException if not. If the request method is
	 * OPTIONS, it returns success. It then calls the check method to perform additional
	 * security checks. If any exception occurs during the process, it logs the error and
	 * returns an appropriate SecurityResponse.
	 * @param request The HttpServletRequest object representing the incoming request.
	 * @param endpointId The EndpointId object representing the endpoint id.
	 * @return A SecurityResponse object indicating the result of the security checks.
	 */
	SecurityResponse preHandle(HttpServletRequest request, EndpointId endpointId) {
		if (CorsUtils.isPreFlightRequest(request)) {
			return SecurityResponse.success();
		}
		try {
			if (!StringUtils.hasText(this.applicationId)) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Application id is not available");
			}
			if (this.cloudFoundrySecurityService == null) {
				throw new CloudFoundryAuthorizationException(Reason.SERVICE_UNAVAILABLE,
						"Cloud controller URL is not available");
			}
			if (HttpMethod.OPTIONS.matches(request.getMethod())) {
				return SUCCESS;
			}
			check(request, endpointId);
		}
		catch (Exception ex) {
			logger.error(ex);
			if (ex instanceof CloudFoundryAuthorizationException cfException) {
				return new SecurityResponse(cfException.getStatusCode(),
						"{\"security_error\":\"" + cfException.getMessage() + "\"}");
			}
			return new SecurityResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
		}
		return SecurityResponse.success();
	}

	/**
	 * Checks the access level for a given request and endpoint ID.
	 * @param request the HttpServletRequest object representing the current request
	 * @param endpointId the EndpointId object representing the ID of the endpoint to
	 * check access for
	 * @throws CloudFoundryAuthorizationException if access is denied
	 */
	private void check(HttpServletRequest request, EndpointId endpointId) {
		Token token = getToken(request);
		this.tokenValidator.validate(token);
		AccessLevel accessLevel = this.cloudFoundrySecurityService.getAccessLevel(token.toString(), this.applicationId);
		if (!accessLevel.isAccessAllowed((endpointId != null) ? endpointId.toLowerCaseString() : "")) {
			throw new CloudFoundryAuthorizationException(Reason.ACCESS_DENIED, "Access denied");
		}
		request.setAttribute(AccessLevel.REQUEST_ATTRIBUTE, accessLevel);
	}

	/**
	 * Retrieves the token from the Authorization header in the HttpServletRequest.
	 * @param request The HttpServletRequest object containing the request information.
	 * @return The Token object extracted from the Authorization header.
	 * @throws CloudFoundryAuthorizationException if the Authorization header is missing
	 * or invalid.
	 */
	private Token getToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		String bearerPrefix = "bearer ";
		if (authorization == null || !authorization.toLowerCase(Locale.ENGLISH).startsWith(bearerPrefix)) {
			throw new CloudFoundryAuthorizationException(Reason.MISSING_AUTHORIZATION,
					"Authorization header is missing or invalid");
		}
		return new Token(authorization.substring(bearerPrefix.length()));
	}

}
