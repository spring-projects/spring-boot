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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.h2.H2ConsoleProperties;
import org.springframework.boot.autoconfigure.security.StaticResourceLocation;
import org.springframework.boot.security.servlet.ApplicationContextRequestMatcher;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.context.WebApplicationContext;

/**
 * Factory that can be used to create a {@link RequestMatcher} for commonly used paths.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @since 2.0.0
 */
public final class PathRequest {

	/**
     * Private constructor for the PathRequest class.
     */
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
	public static final class H2ConsoleRequestMatcher extends ApplicationContextRequestMatcher<H2ConsoleProperties> {

		private volatile RequestMatcher delegate;

		/**
         * Constructs a new H2ConsoleRequestMatcher.
         * 
         * This constructor is private and can only be accessed within the H2ConsoleRequestMatcher class.
         * 
         * @param properties the H2ConsoleProperties object used to configure the H2 console
         */
        private H2ConsoleRequestMatcher() {
			super(H2ConsoleProperties.class);
		}

		/**
         * Determines whether to ignore the given WebApplicationContext based on its server namespace.
         * 
         * @param applicationContext the WebApplicationContext to be checked
         * @return true if the given WebApplicationContext should be ignored, false otherwise
         */
        @Override
		protected boolean ignoreApplicationContext(WebApplicationContext applicationContext) {
			return WebServerApplicationContext.hasServerNamespace(applicationContext, "management");
		}

		/**
         * Initializes the H2ConsoleRequestMatcher with the given H2ConsoleProperties supplier.
         * 
         * @param h2ConsoleProperties the supplier for H2ConsoleProperties
         */
        @Override
		protected void initialized(Supplier<H2ConsoleProperties> h2ConsoleProperties) {
			this.delegate = new AntPathRequestMatcher(h2ConsoleProperties.get().getPath() + "/**");
		}

		/**
         * Determines if the given HttpServletRequest matches the H2ConsoleRequestMatcher.
         * 
         * @param request the HttpServletRequest to be matched
         * @param context a Supplier of H2ConsoleProperties for the current context
         * @return true if the request matches the H2ConsoleRequestMatcher, false otherwise
         */
        @Override
		protected boolean matches(HttpServletRequest request, Supplier<H2ConsoleProperties> context) {
			return this.delegate.matches(request);
		}

	}

}
