/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Factory that can be used to create a {@link RequestMatcher} for commonly used paths.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class PathRequest {

	private PathRequest() {
	}

	/**
	 * Returns a {@link StaticResourceRequest} that can be used to create a matcher for
	 * {@link StaticResourceLocation locations}.
	 * @return a {@link StaticResourceRequest}
	 */
	public static StaticResourceRequest toStaticResources() {
		return StaticResourceRequest.INSTANCE;
	}

	/**
	 * Returns a matcher that includes the H2 console location. For example:
	 * <pre class="code">
	 * PathRequest.toH2Console()
	 * </pre>
	 * @return the configured {@link RequestMatcher}
	 */
	public static H2ConsoleRequestMatcher toH2Console() {
		return new H2ConsoleRequestMatcher();
	}

	/**
	 * The request matcher used to match against h2 console path.
	 */
	public static final class H2ConsoleRequestMatcher
			extends ApplicationContextRequestMatcher<H2ConsoleProperties> {

		private volatile RequestMatcher delegate;

		private H2ConsoleRequestMatcher() {
			super(H2ConsoleProperties.class);
		}

		@Override
		protected void initialized(Supplier<H2ConsoleProperties> h2ConsoleProperties) {
			this.delegate = new AntPathRequestMatcher(
					h2ConsoleProperties.get().getPath() + "/**");
		}

		@Override
		protected boolean matches(HttpServletRequest request,
				Supplier<H2ConsoleProperties> context) {
			return this.delegate.matches(request);
		}

	}

}
