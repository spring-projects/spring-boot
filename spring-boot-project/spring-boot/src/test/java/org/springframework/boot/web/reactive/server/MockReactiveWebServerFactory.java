/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.reactive.server;

import java.util.Map;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.http.server.reactive.HttpHandler;

import static org.mockito.Mockito.spy;

/**
 * Mock {@link ReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public class MockReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	private MockReactiveWebServer webServer;

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		this.webServer = spy(new MockReactiveWebServer(httpHandler, getPort()));
		return this.webServer;
	}

	public MockReactiveWebServer getWebServer() {
		return this.webServer;
	}

	public static class MockReactiveWebServer implements WebServer {

		private final int port;

		private HttpHandler httpHandler;

		private Map<String, HttpHandler> httpHandlerMap;

		public MockReactiveWebServer(HttpHandler httpHandler, int port) {
			this.httpHandler = httpHandler;
			this.port = port;
		}

		public MockReactiveWebServer(Map<String, HttpHandler> httpHandlerMap, int port) {
			this.httpHandlerMap = httpHandlerMap;
			this.port = port;
		}

		public HttpHandler getHttpHandler() {
			return this.httpHandler;
		}

		public Map<String, HttpHandler> getHttpHandlerMap() {
			return this.httpHandlerMap;
		}

		@Override
		public void start() throws WebServerException {

		}

		@Override
		public void stop() throws WebServerException {

		}

		@Override
		public int getPort() {
			return this.port;
		}

	}

}
