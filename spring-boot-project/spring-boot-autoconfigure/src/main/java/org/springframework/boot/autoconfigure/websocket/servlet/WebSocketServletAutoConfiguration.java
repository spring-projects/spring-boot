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

package org.springframework.boot.autoconfigure.websocket.servlet;

import javax.servlet.Servlet;
import javax.websocket.server.ServerContainer;

import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration for WebSocket servlet server in embedded Tomcat, Jetty or Undertow.
 * Requires the appropriate WebSocket modules to be on the classpath.
 * <p>
 * If Tomcat's WebSocket support is detected on the classpath we add a customizer that
 * installs the Tomcat WebSocket initializer. In a non-embedded server it should already
 * be there.
 * <p>
 * If Jetty's WebSocket support is detected on the classpath we add a configuration that
 * configures the context with WebSocket support. In a non-embedded server it should
 * already be there.
 * <p>
 * If Undertow's WebSocket support is detected on the classpath we add a customizer that
 * installs the Undertow WebSocket DeploymentInfo Customizer. In a non-embedded server it
 * should already be there.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass({ Servlet.class, ServerContainer.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@AutoConfigureBefore(ServletWebServerFactoryAutoConfiguration.class)
public class WebSocketServletAutoConfiguration {

	@Configuration
	@ConditionalOnClass(name = "org.apache.tomcat.websocket.server.WsSci", value = Tomcat.class)
	static class TomcatWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		public TomcatWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new TomcatWebSocketServletWebServerCustomizer();
		}

	}

	@Configuration
	@ConditionalOnClass(WebSocketServerContainerInitializer.class)
	static class JettyWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		public JettyWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new JettyWebSocketServletWebServerCustomizer();
		}

	}

	@Configuration
	@ConditionalOnClass(io.undertow.websockets.jsr.Bootstrap.class)
	static class UndertowWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		public UndertowWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new UndertowWebSocketServletWebServerCustomizer();
		}

	}

}
