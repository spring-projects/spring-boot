/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.web.servlet.context;

import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerBeforeStartEvent;
import org.springframework.boot.web.server.WebServer;

/**
 * Event to be published before the {@link WebServer} starts.
 */
public class ServletWebServerBeforeStartEvent extends WebServerBeforeStartEvent {
	private final ServletWebServerApplicationContext applicationContext;

	public ServletWebServerBeforeStartEvent(WebServer webServer, ServletWebServerApplicationContext applicationContext) {
		super(webServer);
		this.applicationContext = applicationContext;
	}

	@Override
	public WebServerApplicationContext getApplicationContext() {
		return applicationContext;
	}
}
