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

package org.springframework.boot.autoconfigure.web.server.tomcat;

import org.apache.tomcat.websocket.server.WsSci;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.server.reactive.tomcat.TomcatReactiveWebServerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.server.servlet.tomcat.TomcatServletWebServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link Configuration Configuration} for a Tomcat-based reactive or servlet web server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see TomcatReactiveWebServerAutoConfiguration
 * @see TomcatServletWebServerAutoConfiguration
 */
@ConditionalOnNotWarDeployment
@Configuration(proxyBeanMethods = false)
public class TomcatWebServerConfiguration {

	@Bean
	TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
		return new TomcatWebServerFactoryCustomizer(environment, serverProperties);
	}

	@Bean
	@ConditionalOnThreading(Threading.VIRTUAL)
	TomcatVirtualThreadsWebServerFactoryCustomizer tomcatVirtualThreadsProtocolHandlerCustomizer() {
		return new TomcatVirtualThreadsWebServerFactoryCustomizer();
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WsSci.class)
	static class TomcatWebSocketConfiguration {

		@Bean
		WebSocketTomcatWebServerFactoryCustomizer webSocketWebServerCustomizer() {
			return new WebSocketTomcatWebServerFactoryCustomizer();
		}

	}

}
