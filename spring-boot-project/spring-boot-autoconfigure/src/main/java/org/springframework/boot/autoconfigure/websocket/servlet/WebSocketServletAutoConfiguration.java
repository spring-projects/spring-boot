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

package org.springframework.boot.autoconfigure.websocket.servlet;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.Servlet;
import jakarta.websocket.server.ServerContainer;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

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
@AutoConfiguration(before = ServletWebServerFactoryAutoConfiguration.class)
@ConditionalOnClass({ Servlet.class, ServerContainer.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
public class WebSocketServletAutoConfiguration {

	/**
     * TomcatWebSocketConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Tomcat.class, WsSci.class })
	static class TomcatWebSocketConfiguration {

		/**
         * Creates a new instance of {@link TomcatWebSocketServletWebServerCustomizer} if no bean with the name "websocketServletWebServerCustomizer" is found.
         * 
         * @return the {@link TomcatWebSocketServletWebServerCustomizer} bean
         */
        @Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		TomcatWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new TomcatWebSocketServletWebServerCustomizer();
		}

	}

	/**
     * JettyWebSocketConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JakartaWebSocketServletContainerInitializer.class)
	static class JettyWebSocketConfiguration {

		/**
         * Creates a new instance of JettyWebSocketServletWebServerCustomizer if there is no existing bean with the name "websocketServletWebServerCustomizer".
         * This customizer is used to configure the Jetty WebSocket servlet for the web server.
         *
         * @return the JettyWebSocketServletWebServerCustomizer instance
         */
        @Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		JettyWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new JettyWebSocketServletWebServerCustomizer();
		}

		/**
         * Customizes the Jetty servlet web server factory to add a WebSocket upgrade filter.
         * This method is conditionally executed based on the deployment not being a war deployment,
         * and if there is no existing bean with the name "websocketUpgradeFilterWebServerCustomizer".
         * The customizer is added with the lowest precedence.
         *
         * @return The web server factory customizer for the WebSocket upgrade filter.
         */
        @Bean
		@ConditionalOnNotWarDeployment
		@Order(Ordered.LOWEST_PRECEDENCE)
		@ConditionalOnMissingBean(name = "websocketUpgradeFilterWebServerCustomizer")
		WebServerFactoryCustomizer<JettyServletWebServerFactory> websocketUpgradeFilterWebServerCustomizer() {
			return (factory) -> {
				factory.addInitializers((servletContext) -> {
					Dynamic registration = servletContext.addFilter(WebSocketUpgradeFilter.class.getName(),
							new WebSocketUpgradeFilter());
					registration.setAsyncSupported(true);
					registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
				});
			};
		}

	}

	/**
     * UndertowWebSocketConfiguration class.
     */
    @Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(io.undertow.websockets.jsr.Bootstrap.class)
	static class UndertowWebSocketConfiguration {

		/**
         * Creates a new instance of UndertowWebSocketServletWebServerCustomizer if there is no existing bean with the name "websocketServletWebServerCustomizer".
         * This customizer is used to configure the Undertow web server for WebSocket support.
         *
         * @return the UndertowWebSocketServletWebServerCustomizer instance
         */
        @Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		UndertowWebSocketServletWebServerCustomizer websocketServletWebServerCustomizer() {
			return new UndertowWebSocketServletWebServerCustomizer();
		}

	}

}
