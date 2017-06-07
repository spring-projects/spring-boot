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

package org.springframework.boot.autoconfigure.web.reactive;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link DefaultReactiveWebServerCustomizer}.
 *
 * @author Brian Clozel
 */
public class DefaultReactiveWebServerCustomizerTests {

	private final ServerProperties properties = new ServerProperties();

	private DefaultReactiveWebServerCustomizer customizer;

	@Before
	public void setup() throws Exception {
		this.customizer = new DefaultReactiveWebServerCustomizer(this.properties);
	}

	@Test
	public void testCustomizeServerPort() throws Exception {
		ConfigurableReactiveWebServerFactory factory = mock(
				ConfigurableReactiveWebServerFactory.class);
		this.properties.setPort(9000);
		this.customizer.customize(factory);
		verify(factory).setPort(9000);
	}

	@Test
	public void testCustomizeServerAddress() throws Exception {
		ConfigurableReactiveWebServerFactory factory = mock(
				ConfigurableReactiveWebServerFactory.class);
		InetAddress address = mock(InetAddress.class);
		this.properties.setAddress(address);
		this.customizer.customize(factory);
		verify(factory).setAddress(address);
	}

}
