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

package org.springframework.boot.autoconfigure.web.reactive;

import io.undertow.Undertow;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.reactor.netty.ReactorNettyConfigurations;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.netty.NettyRouteProvider;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * Configuration classes for reactive web servers
 * <p>
 * Those should be {@code @Import} in a regular auto-configuration class to guarantee
 * their order of execution.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Sergey Serdyuk
 */
abstract class ReactiveWebServerFactoryConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ HttpServer.class })
	@Import(ReactorNettyConfigurations.ReactorResourceFactoryConfiguration.class)
	static class EmbeddedNetty {

		@Bean
		NettyReactiveWebServerFactory nettyReactiveWebServerFactory(ReactorResourceFactory resourceFactory,
				ObjectProvider<NettyRouteProvider> routes, ObjectProvider<NettyServerCustomizer> serverCustomizers) {
			NettyReactiveWebServerFactory serverFactory = new NettyReactiveWebServerFactory();
			serverFactory.setResourceFactory(resourceFactory);
			routes.orderedStream().forEach(serverFactory::addRouteProviders);
			serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
			return serverFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ org.apache.catalina.startup.Tomcat.class })
	static class EmbeddedTomcat {

		@Bean
		TomcatReactiveWebServerFactory tomcatReactiveWebServerFactory(
				ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
				ObjectProvider<TomcatContextCustomizer> contextCustomizers,
				ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
			TomcatReactiveWebServerFactory factory = new TomcatReactiveWebServerFactory();
			factory.getTomcatConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
			factory.getTomcatContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
			factory.getTomcatProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
			return factory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ org.eclipse.jetty.server.Server.class, ServletHolder.class })
	static class EmbeddedJetty {

		@Bean
		JettyReactiveWebServerFactory jettyReactiveWebServerFactory(
				ObjectProvider<JettyServerCustomizer> serverCustomizers) {
			JettyReactiveWebServerFactory serverFactory = new JettyReactiveWebServerFactory();
			serverFactory.getServerCustomizers().addAll(serverCustomizers.orderedStream().toList());
			return serverFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ReactiveWebServerFactory.class)
	@ConditionalOnClass({ Undertow.class })
	static class EmbeddedUndertow {

		@Bean
		UndertowReactiveWebServerFactory undertowReactiveWebServerFactory(
				ObjectProvider<UndertowBuilderCustomizer> builderCustomizers) {
			UndertowReactiveWebServerFactory factory = new UndertowReactiveWebServerFactory();
			factory.getBuilderCustomizers().addAll(builderCustomizers.orderedStream().toList());
			return factory;
		}

	}

}
