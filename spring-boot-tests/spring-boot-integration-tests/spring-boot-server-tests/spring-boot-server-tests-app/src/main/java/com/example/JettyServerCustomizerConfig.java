/*
 * Copyright 2012-2022 the original author or authors.
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

package com.example;

import org.eclipse.jetty.http.UriCompliance;
import org.eclipse.jetty.server.AllowedResourceAliasChecker;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link JettyServerCustomizer} that:
 * <ul>
 * <li>Approves all aliases (Used for Windows CI on
 * Concourse)
 * <li>Relaxes URI compliance to allow access to static resources with {@code %} in their file name.
 * </ul>
 *
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
@ConditionalOnClass(name = {"org.eclipse.jetty.server.handler.ContextHandler"})
@Configuration(proxyBeanMethods = false)
public class JettyServerCustomizerConfig {

	@Bean
	public JettyServerCustomizer jettyServerCustomizer() {
		return (server) -> {
			ContextHandler handler = (ContextHandler) server.getHandler();
			handler.addAliasCheck((path, resource) -> true);
			
			for (Connector connector : server.getConnectors()) {
				connector.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration()
						.setUriCompliance(UriCompliance.LEGACY);
			}
		};
	}

}
