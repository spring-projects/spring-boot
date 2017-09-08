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

package org.springframework.boot.web.embedded.tomcat;

import java.util.Arrays;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.http.server.reactive.HttpHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
public class TomcatReactiveWebServerFactoryTests
		extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected TomcatReactiveWebServerFactory getFactory() {
		return new TomcatReactiveWebServerFactory(0);
	}

	@Test
	public void tomcatCustomizers() throws Exception {
		TomcatReactiveWebServerFactory factory = getFactory();
		TomcatContextCustomizer[] listeners = new TomcatContextCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatContextCustomizer.class);
		}
		factory.setTomcatContextCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addContextCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatContextCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Context.class));
		}
	}

	@Test
	public void setNullConnectorCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.setTomcatConnectorCustomizers(null);
	}

	@Test
	public void addNullAddConnectorCustomizersShouldThrowException() {
		TomcatReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.addConnectorCustomizers((TomcatConnectorCustomizer[]) null);
	}

	@Test
	public void tomcatConnectorCustomizersShouldBeInvoked() throws Exception {
		TomcatReactiveWebServerFactory factory = getFactory();
		HttpHandler handler = mock(HttpHandler.class);
		TomcatConnectorCustomizer[] listeners = new TomcatConnectorCustomizer[4];
		for (int i = 0; i < listeners.length; i++) {
			listeners[i] = mock(TomcatConnectorCustomizer.class);
		}
		factory.setTomcatConnectorCustomizers(Arrays.asList(listeners[0], listeners[1]));
		factory.addConnectorCustomizers(listeners[2], listeners[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) listeners);
		for (TomcatConnectorCustomizer listener : listeners) {
			ordered.verify(listener).customize(any(Connector.class));
		}
	}

}
