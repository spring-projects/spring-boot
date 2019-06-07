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

package org.springframework.boot.web.embedded.jetty;

import java.net.InetAddress;
import java.util.Arrays;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.http.server.reactive.HttpHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyReactiveWebServerFactory} and {@link JettyWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
public class JettyReactiveWebServerFactoryTests extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected JettyReactiveWebServerFactory getFactory() {
		return new JettyReactiveWebServerFactory(0);
	}

	@Test
	public void setNullServerCustomizersShouldThrowException() {
		JettyReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.setServerCustomizers(null);
	}

	@Test
	public void addNullServerCustomizersShouldThrowException() {
		JettyReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.addServerCustomizers((JettyServerCustomizer[]) null);
	}

	@Test
	public void jettyCustomizersShouldBeInvoked() {
		HttpHandler handler = mock(HttpHandler.class);
		JettyReactiveWebServerFactory factory = getFactory();
		JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
		Arrays.setAll(configurations, (i) -> mock(JettyServerCustomizer.class));
		factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
		factory.addServerCustomizers(configurations[2], configurations[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) configurations);
		for (JettyServerCustomizer configuration : configurations) {
			ordered.verify(configuration).customize(any(Server.class));
		}
	}

	@Test
	public void specificIPAddressNotReverseResolved() throws Exception {
		JettyReactiveWebServerFactory factory = getFactory();
		InetAddress localhost = InetAddress.getLocalHost();
		factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
		this.webServer = factory.getWebServer(mock(HttpHandler.class));
		this.webServer.start();
		Connector connector = ((JettyWebServer) this.webServer).getServer().getConnectors()[0];
		assertThat(((ServerConnector) connector).getHost()).isEqualTo(localhost.getHostAddress());
	}

}
