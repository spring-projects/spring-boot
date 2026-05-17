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

package org.springframework.boot.web.server.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.event.PortBound;
import org.springframework.boot.web.server.WebServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebServerInitializedEvent}.
 *
 * @author Somil Jain
 */
class WebServerInitializedEventTests {

	@Test
	void implementsPortBoundAndExposesProperties() {
		WebServer webServer = mock(WebServer.class);
		given(webServer.getPort()).willReturn(8080);

		WebServerApplicationContext applicationContext = mock(WebServerApplicationContext.class);
		given(applicationContext.getServerNamespace()).willReturn("management");

		WebServerInitializedEvent event = new TestWebServerInitializedEvent(webServer, applicationContext);

		assertThat(event).isInstanceOf(PortBound.class);
		assertThat(event.getPort()).isEqualTo(8080);
		assertThat(event.getNamespace()).isEqualTo("management");
	}

	private static class TestWebServerInitializedEvent extends WebServerInitializedEvent {

		private final WebServerApplicationContext applicationContext;

		TestWebServerInitializedEvent(WebServer webServer, WebServerApplicationContext applicationContext) {
			super(webServer);
			this.applicationContext = applicationContext;
		}

		@Override
		public WebServerApplicationContext getApplicationContext() {
			return this.applicationContext;
		}

	}

}
