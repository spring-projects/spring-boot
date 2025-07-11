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

	public JettyWebServerConfiguration(JettyServerProperties jettyProperties) {
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
