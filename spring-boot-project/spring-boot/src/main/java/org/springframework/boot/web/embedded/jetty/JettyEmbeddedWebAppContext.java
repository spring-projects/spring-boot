/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.webapp.ClassMatcher;
import org.eclipse.jetty.ee10.webapp.WebAppContext;

/**
 * Jetty {@link WebAppContext} used by {@link JettyWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 */
class JettyEmbeddedWebAppContext extends WebAppContext {

	/**
     * Constructor for JettyEmbeddedWebAppContext.
     * Initializes the server class matcher with the specified class matcher.
     * 
     * @param serverClassMatcher the server class matcher to be set
     */
    JettyEmbeddedWebAppContext() {
		setServerClassMatcher(new ClassMatcher("org.springframework.boot.loader."));
		// setTempDirectory(WebInfConfiguration.getCanonicalNameForWebAppTmpDir(this));
	}

	/**
     * Creates a new instance of ServletHandler for this JettyEmbeddedWebAppContext.
     * 
     * @return the newly created ServletHandler
     */
    @Override
	protected ServletHandler newServletHandler() {
		return new JettyEmbeddedServletHandler();
	}

	/**
     * Performs deferred initialization of the JettyEmbeddedWebAppContext.
     * This method calls the deferredInitialize() method of the JettyEmbeddedServletHandler
     * to perform any necessary initialization tasks.
     *
     * @throws Exception if an error occurs during deferred initialization.
     */
    void deferredInitialize() throws Exception {
		((JettyEmbeddedServletHandler) getServletHandler()).deferredInitialize();
	}

	/**
     * JettyEmbeddedServletHandler class.
     */
    private static final class JettyEmbeddedServletHandler extends ServletHandler {

		/**
         * Initializes the JettyEmbeddedServletHandler.
         *
         * @throws Exception if an error occurs during initialization
         */
        @Override
		public void initialize() throws Exception {
		}

		/**
         * Performs deferred initialization of the JettyEmbeddedServletHandler.
         * This method calls the superclass's initialize method.
         *
         * @throws Exception if an error occurs during initialization
         */
        void deferredInitialize() throws Exception {
			super.initialize();
		}

	}

}
