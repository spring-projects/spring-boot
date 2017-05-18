/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

/**
 * Security authorization modes as specified in {@link SecurityProperties}.
 *
 * @author Phillip Webb
 * @since 1.2.2
 */
public enum SecurityAuthorizeMode {

	/**
	 * Must be a member of one of the security roles.
	 */
	ROLE,

	/**
	 * Must be an authenticated user.
	 */
	AUTHENTICATED,

	/**
	 * No security authorization is setup.
	 */
	NONE

}
