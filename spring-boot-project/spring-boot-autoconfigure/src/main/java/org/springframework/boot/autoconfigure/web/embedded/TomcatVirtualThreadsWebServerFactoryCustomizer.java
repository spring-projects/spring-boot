/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import org.apache.coyote.ProtocolHandler;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;

import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.core.Ordered;

/**
 * Activates {@link VirtualThreadExecutor} on {@link ProtocolHandler Tomcat's protocol
 * handler}.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
public class TomcatVirtualThreadsWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

	@Override
	public void customize(ConfigurableTomcatWebServerFactory factory) {
		factory.addProtocolHandlerCustomizers(
				(protocolHandler) -> protocolHandler.setExecutor(new VirtualThreadExecutor("tomcat-handler-")));
	}

	@Override
	public int getOrder() {
		return TomcatWebServerFactoryCustomizer.ORDER + 1;
	}

}
