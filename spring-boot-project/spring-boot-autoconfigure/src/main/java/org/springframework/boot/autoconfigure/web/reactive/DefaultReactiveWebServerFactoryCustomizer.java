/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.reactive;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.jetty.JettyCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.tomcat.TomcatCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.undertow.UndertowCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;

/**
 * Default {@link WebServerFactoryCustomizer} for reactive servers.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class DefaultReactiveWebServerFactoryCustomizer implements
		WebServerFactoryCustomizer<ConfigurableReactiveWebServerFactory>, EnvironmentAware, Ordered {

	private final ServerProperties serverProperties;

	private Environment environment;

	public DefaultReactiveWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void customize(ConfigurableReactiveWebServerFactory factory) {
		if (this.serverProperties.getPort() != null) {
			factory.setPort(this.serverProperties.getPort());
		}
		if (this.serverProperties.getAddress() != null) {
			factory.setAddress(this.serverProperties.getAddress());
		}
		if (this.serverProperties.getSsl() != null) {
			factory.setSsl(this.serverProperties.getSsl());
		}
		if (this.serverProperties.getCompression() != null) {
			factory.setCompression(this.serverProperties.getCompression());
		}
		if (this.serverProperties.getHttp2() != null) {
			factory.setHttp2(this.serverProperties.getHttp2());
		}
		if (factory instanceof TomcatReactiveWebServerFactory) {
			TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment,
					(TomcatReactiveWebServerFactory) factory);
		}
		if (factory instanceof JettyReactiveWebServerFactory) {
			JettyCustomizer.customizeJetty(this.serverProperties, this.environment,
					(JettyReactiveWebServerFactory) factory);
		}
		if (factory instanceof UndertowReactiveWebServerFactory) {
			UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment,
					(UndertowReactiveWebServerFactory) factory);
		}
	}

}
