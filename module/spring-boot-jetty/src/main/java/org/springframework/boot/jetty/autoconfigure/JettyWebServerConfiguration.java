/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.jetty.autoconfigure.reactive.JettyReactiveWebServerAutoConfiguration;
import org.springframework.boot.jetty.autoconfigure.servlet.JettyServletWebServerAutoConfiguration;
import org.springframework.boot.thread.Threading;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link Configuration Configuration} for a Jetty-based reactive or servlet web server.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 * @see JettyReactiveWebServerAutoConfiguration
 * @see JettyServletWebServerAutoConfiguration
 */
@ConditionalOnNotWarDeployment
@Configuration(proxyBeanMethods = false)
public class JettyWebServerConfiguration {

	private final JettyServerProperties jettyProperties;

	JettyWebServerConfiguration(JettyServerProperties jettyProperties) {
		this.jettyProperties = jettyProperties;
	}

	@Bean
	JettyWebServerFactoryCustomizer jettyWebServerFactoryCustomizer(Environment environment,
			ServerProperties serverProperties) {
		return new JettyWebServerFactoryCustomizer(environment, serverProperties, this.jettyProperties);
	}

	@Bean
	@ConditionalOnThreading(Threading.VIRTUAL)
	JettyVirtualThreadsWebServerFactoryCustomizer jettyVirtualThreadsWebServerFactoryCustomizer() {
		return new JettyVirtualThreadsWebServerFactoryCustomizer(this.jettyProperties);
	}

}
