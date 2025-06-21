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

import java.net.URI;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Adapts {@link HttpRedirects} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link RedirectStrategy}.
 *
 * @author Phillip Webb
 */
final class HttpComponentsRedirectStrategy {

	private HttpComponentsRedirectStrategy() {
	}

	static RedirectStrategy get(HttpRedirects redirects) {
		return switch (redirects) {
			case FOLLOW_WHEN_POSSIBLE, FOLLOW -> DefaultRedirectStrategy.INSTANCE;
			case DONT_FOLLOW -> NoFollowRedirectStrategy.INSTANCE;
		};
	}

	/**
	 * {@link RedirectStrategy} that never follows redirects.
	 */
	private static final class NoFollowRedirectStrategy implements RedirectStrategy {

		private static final RedirectStrategy INSTANCE = new NoFollowRedirectStrategy();

		private NoFollowRedirectStrategy() {
		}

		@Override
		public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
			return false;
		}

		@Override
		public URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) {
			return null;
		}

	}

}
