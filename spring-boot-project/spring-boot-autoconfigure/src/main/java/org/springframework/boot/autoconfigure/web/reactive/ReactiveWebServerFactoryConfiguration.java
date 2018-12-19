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

import java.util.stream.Collectors;

import io.undertow.Undertow;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.JettyResourceFactory;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * Configuration classes for reactive web servers
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 */
abstract class ReactiveWebServerFactoryConfiguration {

	@Configuration
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ HttpServer.class })
	static class EmbeddedNetty {

		@Bean
		@ConditionalOnMissingBean
		public ReactorResourceFactory reactorServerResourceFactory() {
			return new ReactorResourceFactory();
		}

		@Bean
		public NettyReactiveWebServerFactory nettyReactiveWebServerFactory(
				ReactorResourceFactory resourceFactory) {
			NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory();
			serverFactory.setResourceFactory(resourceFactory);
			return serverFactory;
		}

	}

	@Configuration
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ org.apache.catalina.startup.Tomcat.class })
	static class EmbeddedTomcat {

		@Bean
		public TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory(
				ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
				ObjectProvider<TomcatContextCustomizer> contextCustomizers) {
			TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
			factory.getTomcatConnectorCustomizers().addAll(
					connectorCustomizers.orderedStream().collect(Collectors.toList()));
			factory.getTomcatContextCustomizers().addAll(
					contextCustomizers.orderedStream().collect(Collectors.toList()));
			return factory;
		}

	}

	@Configuration
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ org.eclipse.jetty.server.Server.class })
	static class EmbeddedJetty {

		@Bean
		@ConditionalOnMissingBean
		public JettyResourceFactory jettyServerResourceFactory() {
			return new JettyResourceFactory();
		}

		@Bean
		public JettyReactiveWebServerFactory jettyReactiveWebServerFactory(
				JettyResourceFactory resourceFactory) {
			JettyReactiveWebServerFactory serverFactory = new JettyReactiveWebServerFactory();
			serverFactory.setResourceFactory(resourceFactory);
			return serverFactory;
		}

	}

	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ Undertow.class })
	static class EmbeddedUndertow {

		@Bean
		public UndertowReactiveWebServerFactory undertowReactiveWebServerFactory() {
			return new UndertowReactiveWebServerFactory();
		}

	}

}
