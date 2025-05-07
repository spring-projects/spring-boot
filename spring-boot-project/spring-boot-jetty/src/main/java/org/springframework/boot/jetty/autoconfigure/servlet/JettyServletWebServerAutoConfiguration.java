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

package org.springframework.boot.jetty.autoconfigure.servlet;

import java.util.EnumSet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration.Dynamic;
import jakarta.servlet.ServletRequest;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.boot.jetty.autoconfigure.JettyServerProperties;
import org.springframework.boot.jetty.autoconfigure.JettyWebServerConfiguration;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.servlet.ServletWebServerConfiguration;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for a Jetty-based servlet web
 * server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration
@ConditionalOnClass({ ServletRequest.class, Server.class, Loader.class, WebAppContext.class })
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(JettyServerProperties.class)
@Import({ JettyWebServerConfiguration.class, ServletWebServerConfiguration.class })
public class JettyServletWebServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
	JettyServletWebServerFactory jettyServletWebServerFactory(ObjectProvider<JettyServerCustomizer> serverCustomizers) {
		JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
		factory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JakartaWebSocketServletContainerInitializer.class)
	static class JettyWebSocketConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "websocketServletWebServerCustomizer")
		WebSocketJettyServletWebServerFactoryCustomizer websocketServletWebServerCustomizer() {
			return new WebSocketJettyServletWebServerFactoryCustomizer();
		}

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

}
