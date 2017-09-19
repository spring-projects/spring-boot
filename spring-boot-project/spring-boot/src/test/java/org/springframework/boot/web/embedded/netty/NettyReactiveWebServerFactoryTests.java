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

import org.junit.Test;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.boot.web.server.PortInUseException;

import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link NettyReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 */
public class NettyReactiveWebServerFactoryTests
		extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected AbstractReactiveWebServerFactory getFactory() {
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

}
