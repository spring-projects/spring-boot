/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.http.client;

import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.jspecify.annotations.Nullable;

/**
 * Adapts {@link HttpCookies} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * cookie spec identifier.
 *
 * @author Apoorv Darshan
 */
final class HttpComponentsCookieSpec {

	private HttpComponentsCookieSpec() {
	}

	static @Nullable String get(@Nullable HttpCookies cookies) {
		if (cookies == null) {
			return null;
		}
		return switch (cookies) {
			case ENABLE_WHEN_POSSIBLE, ENABLE -> StandardCookieSpec.STRICT;
			case DISABLE -> StandardCookieSpec.IGNORE;
		};
	}

}
