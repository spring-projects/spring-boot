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
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.util.ClassMatcher;

/**
 * Jetty {@link WebAppContext} used by {@link JettyWebServer} to support deferred
 * initialization.
 *
 * @author Phillip Webb
 */
class JettyEmbeddedWebAppContext extends WebAppContext {

	JettyEmbeddedWebAppContext() {
		setHiddenClassMatcher(new ClassMatcher("org.springframework.boot.loader."));
	}

	@Override
	protected ServletHandler newServletHandler() {
		return new JettyEmbeddedServletHandler();
	}

	void deferredInitialize() throws Exception {
		((JettyEmbeddedServletHandler) getServletHandler()).deferredInitialize();
	}

	private static final class JettyEmbeddedServletHandler extends ServletHandler {

		@Override
		public void initialize() throws Exception {
		}

		void deferredInitialize() throws Exception {
			super.initialize();
		}

	}

}
