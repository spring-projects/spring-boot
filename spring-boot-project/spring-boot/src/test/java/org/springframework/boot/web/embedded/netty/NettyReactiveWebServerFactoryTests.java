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

package org.springframework.boot.web.embedded.netty;

import java.util.Arrays;

import org.junit.Test;
import org.mockito.InOrder;
import reactor.ipc.netty.http.server.HttpServerOptions;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.boot.web.server.PortInUseException;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public class NettyReactiveWebServerFactoryTests
		extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected NettyReactiveWebServerFactory getFactory() {
		return new NettyReactiveWebServerFactory(0);
	}

	@Test
	public void portInUseExceptionIsThrownWhenPortIsAlreadyInUse() throws Exception {
		AbstractReactiveWebServerFactory factory = getFactory();
		factory.setPort(0);
		this.webServer = factory.getWebServer(new EchoHandler());
		this.webServer.start();
		factory.setPort(this.webServer.getPort());
		this.thrown.expect(PortInUseException.class);
		this.thrown.expectMessage(
				equalTo("Port " + this.webServer.getPort() + " is already in use"));
		factory.getWebServer(new EchoHandler()).start();
	}

	@Test
	public void nettyCustomizers() throws Exception {
		NettyReactiveWebServerFactory factory = getFactory();
		NettyServerCustomizer[] customizers = new NettyServerCustomizer[2];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(NettyServerCustomizer.class);
		}
		factory.setNettyServerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		this.webServer = factory.getWebServer(new EchoHandler());
		InOrder ordered = inOrder((Object[]) customizers);
		for (NettyServerCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(HttpServerOptions.Builder.class));
		}
	}

}
