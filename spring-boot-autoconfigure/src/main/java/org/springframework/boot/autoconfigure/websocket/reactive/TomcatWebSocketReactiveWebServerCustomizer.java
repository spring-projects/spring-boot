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

package org.springframework.boot.autoconfigure.websocket.reactive;

import org.apache.tomcat.websocket.server.WsContextListener;

import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * WebSocket customizer for {@link TomcatReactiveWebServerFactory}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class TomcatWebSocketReactiveWebServerCustomizer
		implements WebServerFactoryCustomizer<TomcatReactiveWebServerFactory>, Ordered {

	@Override
	public void customize(TomcatReactiveWebServerFactory factory) {
		factory.addContextCustomizers((context) -> context
				.addApplicationListener(WsContextListener.class.getName()));
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
