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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry;

import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;

/**
 * Hamcrest matcher to check the {@link AuthorizationExceptionMatcher} {@link Reason}.
 *
 * @author Madhura Bhave
 */
public final class AuthorizationExceptionMatcher {

	private AuthorizationExceptionMatcher() {
	}

	public static Matcher<?> withReason(Reason reason) {
		return new CustomMatcher<Object>("CloudFoundryAuthorizationException with " + reason + " reason") {

			@Override
			public boolean matches(Object object) {
				return ((object instanceof CloudFoundryAuthorizationException)
						&& ((CloudFoundryAuthorizationException) object).getReason() == reason);
			}

		};
	}

}
