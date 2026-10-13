/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat.autoconfigure;

import org.apache.tomcat.websocket.server.WsSci;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.tomcat.autoconfigure.reactive.TomcatReactiveWebServerAutoConfiguration;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
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
@EnableConfigurationProperties(WebProperties.class)
public class TomcatWebServerConfiguration {

	@Bean
	TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties, TomcatServerProperties tomcatProperties, WebProperties webProperties) {
		return new TomcatWebServerFactoryCustomizer(environment, serverProperties, tomcatProperties, webProperties);
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
