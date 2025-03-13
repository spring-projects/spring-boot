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

package org.springframework.boot.undertow.reactive;

import java.util.List;

import io.undertow.Undertow;

import org.springframework.boot.undertow.ConfigurableUndertowWebServerFactory;
import org.springframework.boot.undertow.HttpHandlerFactory;
import org.springframework.boot.undertow.UndertowWebServer;
import org.springframework.boot.undertow.UndertowWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.http.server.reactive.UndertowHttpHandlerAdapter;

/**
 * {@link ReactiveWebServerFactory} that can be used to create {@link UndertowWebServer}s.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 4.0.0
 */
public class UndertowReactiveWebServerFactory extends UndertowWebServerFactory
		implements ConfigurableUndertowWebServerFactory, ConfigurableReactiveWebServerFactory {

	/**
	 * Create a new {@link UndertowReactiveWebServerFactory} instance.
	 */
	public UndertowReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link UndertowReactiveWebServerFactory} that listens for requests
	 * using the specified port.
	 * @param port the port to listen on
	 */
	public UndertowReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(org.springframework.http.server.reactive.HttpHandler httpHandler) {
		Undertow.Builder builder = createBuilder(this, this::getSslBundle, this::getServerNameSslBundles);
		List<HttpHandlerFactory> httpHandlerFactories = createHttpHandlerFactories(this,
				(next) -> new UndertowHttpHandlerAdapter(httpHandler));
		return new UndertowWebServer(builder, httpHandlerFactories, getPort() >= 0);
	}

}
