/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.security.Principal;

/**
 * Security context in which an endpoint is being invoked.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public interface SecurityContext {

	/**
	 * Empty security context.
	 */
	SecurityContext NONE = new SecurityContext() {

		@Override
		public Principal getPrincipal() {
			return null;
		}

		@Override
		public boolean isUserInRole(String role) {
			return false;
		}

	};

	/**
	 * Return the currently authenticated {@link Principal} or {@code null}.
	 * @return the principal or {@code null}
	 */
	Principal getPrincipal();

	/**
	 * Returns {@code true} if the currently authenticated user is in the given
	 * {@code role}, or false otherwise.
	 * @param role name of the role
	 * @return {@code true} if the user is in the given role
	 */
	boolean isUserInRole(String role);

}
