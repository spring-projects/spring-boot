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

package org.springframework.boot.actuate.endpoint;

import java.security.Principal;
import java.util.Collection;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * Options for showing data in endpoint responses.
 *
 * @author Madhura Bhave
 * @since 3.0.0
 */
public enum Show {

	/**
	 * Never show the item in the response.
	 */
	NEVER,

	/**
	 * Show the item in the response when accessed by an authorized user.
	 */
	WHEN_AUTHORIZED,

	/**
	 * Always show the item in the response.
	 */
	ALWAYS;

	/**
	 * Return if data should be shown when no {@link SecurityContext} is available.
	 * @param unauthorizedResult the result to used for an unauthorized user
	 * @return if data should be shown
	 */
	public boolean isShown(boolean unauthorizedResult) {
		return switch (this) {
			case NEVER -> false;
			case ALWAYS -> true;
			case WHEN_AUTHORIZED -> unauthorizedResult;
		};
	}

	/**
	 * Return if data should be shown.
	 * @param securityContext the security context
	 * @param roles the required roles
	 * @return if data should be shown
	 */
	public boolean isShown(SecurityContext securityContext, Collection<String> roles) {
		return switch (this) {
			case NEVER -> false;
			case ALWAYS -> true;
			case WHEN_AUTHORIZED -> isAuthorized(securityContext, roles);
		};
	}

	private boolean isAuthorized(SecurityContext securityContext, Collection<String> roles) {
		Principal principal = securityContext.getPrincipal();
		if (principal == null) {
			return false;
		}
		if (CollectionUtils.isEmpty(roles)) {
			return true;
		}
		boolean checkAuthorities = isSpringSecurityAuthentication(principal);
		for (String role : roles) {
			if (securityContext.isUserInRole(role)) {
				return true;
			}
			if (checkAuthorities) {
				Authentication authentication = (Authentication) principal;
				for (GrantedAuthority authority : authentication.getAuthorities()) {
					String name = authority.getAuthority();
					if (role.equals(name)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isSpringSecurityAuthentication(Principal principal) {
		return ClassUtils.isPresent("org.springframework.security.core.Authentication", null)
				&& (principal instanceof Authentication);
	}

}
