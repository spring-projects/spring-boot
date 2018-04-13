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

package org.springframework.boot.web.embedded.undertow;

import java.util.Arrays;

import io.undertow.Undertow;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactoryTests;
import org.springframework.http.server.reactive.HttpHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UndertowReactiveWebServerFactory} and {@link UndertowWebServer}.
 *
 * @author Brian Clozel
 * @author Madhura Bhave
 */
public class UndertowReactiveWebServerFactoryTests
		extends AbstractReactiveWebServerFactoryTests {

	@Override
	protected UndertowReactiveWebServerFactory getFactory() {
		return new UndertowReactiveWebServerFactory(0);
	}

	@Test
	public void setNullBuilderCustomizersShouldThrowException() {
		UndertowReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.setBuilderCustomizers(null);
	}

	@Test
	public void addNullBuilderCustomizersShouldThrowException() {
		UndertowReactiveWebServerFactory factory = getFactory();
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Customizers must not be null");
		factory.addBuilderCustomizers((UndertowBuilderCustomizer[]) null);
	}

	@Test
	public void builderCustomizersShouldBeInvoked() {
		UndertowReactiveWebServerFactory factory = getFactory();
		HttpHandler handler = mock(HttpHandler.class);
		UndertowBuilderCustomizer[] customizers = new UndertowBuilderCustomizer[4];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(UndertowBuilderCustomizer.class);
		}
		factory.setBuilderCustomizers(Arrays.asList(customizers[0], customizers[1]));
		factory.addBuilderCustomizers(customizers[2], customizers[3]);
		this.webServer = factory.getWebServer(handler);
		InOrder ordered = inOrder((Object[]) customizers);
		for (UndertowBuilderCustomizer customizer : customizers) {
			ordered.verify(customizer).customize(any(Undertow.Builder.class));
		}
	}

}
