/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.devtools.remote.server;

import org.springframework.http.server.ServerHttpRequest;

/**
 * Provides access control for a {@link Dispatcher}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public interface AccessManager {

	/**
	 * {@link AccessManager} that permits all requests.
	 */
	AccessManager PERMIT_ALL = new AccessManager() {

		@Override
		public boolean isAllowed(ServerHttpRequest request) {
			return true;
		}

	};

	/**
	 * Determine if the specific request is allowed to be handled by the
	 * {@link Dispatcher}.
	 * @param request the request to check
	 * @return {@code true} if access is allowed.
	 */
	boolean isAllowed(ServerHttpRequest request);

}
