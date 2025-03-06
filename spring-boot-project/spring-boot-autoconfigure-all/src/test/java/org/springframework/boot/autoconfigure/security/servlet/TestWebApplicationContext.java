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

package org.springframework.boot.autoconfigure.security.servlet;

import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.web.context.support.StaticWebApplicationContext;

/**
 * Test {@link StaticWebApplicationContext} that also implements
 * {@link WebServerApplicationContext}.
 *
 * @author Phillip Webb
 */
class TestWebApplicationContext extends StaticWebApplicationContext implements WebServerApplicationContext {

	private final String serverNamespace;

	TestWebApplicationContext(String serverNamespace) {
		this.serverNamespace = serverNamespace;
	}

	@Override
	public WebServer getWebServer() {
		return null;
	}

	@Override
	public String getServerNamespace() {
		return this.serverNamespace;
	}

}
