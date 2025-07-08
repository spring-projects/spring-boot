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

package org.springframework.boot.security.autoconfigure.servlet;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.h2console.autoconfigure.H2ConsoleProperties;
import org.springframework.boot.security.autoconfigure.StaticResourceLocation;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create a {@link RequestMatcher} for commonly used paths.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 4.0.0
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
	public static final class H2ConsoleRequestMatcher extends ApplicationContextRequestMatcher<ApplicationContext> {

		private volatile RequestMatcher delegate;

		private H2ConsoleRequestMatcher() {
			super(ApplicationContext.class);
		}

		@Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			return WebServerApplicationContext.hasServerNamespace(applicationContext, "management");
		}

		@Override
		protected void initialized(Supplier<ApplicationContext> context) {
			String path = context.get().getBean(H2ConsoleProperties.class).getPath();
			Assert.hasText(path, "'path' in H2ConsoleProperties must not be empty");
			this.delegate = PathPatternRequestMatcher.withDefaults().matcher(path + "/**");
		}

		@Override
		protected boolean matches(HttpServletRequest request, Supplier<ApplicationContext> context) {
			return this.delegate.matches(request);
		}

	}

}
