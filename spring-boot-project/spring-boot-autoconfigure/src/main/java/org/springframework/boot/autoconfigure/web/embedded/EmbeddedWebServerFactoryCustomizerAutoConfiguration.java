/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.embedded;

import io.undertow.Undertow;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.xnio.SslClientAuthMode;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWarDeployment;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.VirtualThreadTaskExecutor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded servlet and reactive
 * web servers customizations.
 *
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
@AutoConfiguration
@ConditionalOnNotWarDeployment
@ConditionalOnWebApplication
@EnableConfigurationProperties(ServerProperties.class)
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {

	/**
	 * Nested configuration if Tomcat is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })
	public static class TomcatWebServerFactoryCustomizerConfiguration {

		/**
		 * Creates a new instance of TomcatWebServerFactoryCustomizer.
		 * @param environment the environment object
		 * @param serverProperties the server properties object
		 * @return the TomcatWebServerFactoryCustomizer instance
		 */
		@Bean
		public TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment,
				ServerProperties serverProperties) {
			return new TomcatWebServerFactoryCustomizer(environment, serverProperties);
		}

		/**
		 * Creates a customizer for the TomcatVirtualThreadsWebServerFactory. This
		 * customizer is only applied if the threading mode is set to virtual. It
		 * configures the TomcatVirtualThreadsWebServerFactory with the necessary protocol
		 * handler customizer.
		 * @return The TomcatVirtualThreadsWebServerFactoryCustomizer instance.
		 */
		@Bean
		@ConditionalOnThreading(Threading.VIRTUAL)
		TomcatVirtualThreadsWebServerFactoryCustomizer tomcatVirtualThreadsProtocolHandlerCustomizer() {
			return new TomcatVirtualThreadsWebServerFactoryCustomizer();
		}

	}

	/**
	 * Nested configuration if Jetty is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Server.class, Loader.class, WebAppContext.class })
	public static class JettyWebServerFactoryCustomizerConfiguration {

		/**
		 * Creates a new instance of JettyWebServerFactoryCustomizer.
		 * @param environment the environment object
		 * @param serverProperties the server properties object
		 * @return the JettyWebServerFactoryCustomizer instance
		 */
		@Bean
		public JettyWebServerFactoryCustomizer jettyWebServerFactoryCustomizer(Environment environment,
				ServerProperties serverProperties) {
			return new JettyWebServerFactoryCustomizer(environment, serverProperties);
		}

		/**
		 * Creates a JettyVirtualThreadsWebServerFactoryCustomizer bean if the threading
		 * mode is set to virtual. This customizer is responsible for configuring the
		 * Jetty web server factory with virtual threads.
		 * @param serverProperties the server properties bean used for customization
		 * @return the JettyVirtualThreadsWebServerFactoryCustomizer bean
		 */
		@Bean
		@ConditionalOnThreading(Threading.VIRTUAL)
		JettyVirtualThreadsWebServerFactoryCustomizer jettyVirtualThreadsWebServerFactoryCustomizer(
				ServerProperties serverProperties) {
			return new JettyVirtualThreadsWebServerFactoryCustomizer(serverProperties);
		}

	}

	/**
	 * Nested configuration if Undertow is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ Undertow.class, SslClientAuthMode.class })
	public static class UndertowWebServerFactoryCustomizerConfiguration {

		/**
		 * Creates a new instance of UndertowWebServerFactoryCustomizer with the specified
		 * environment and server properties.
		 * @param environment the environment object used to access application properties
		 * and profiles
		 * @param serverProperties the server properties object used to configure the
		 * Undertow web server factory
		 * @return a new instance of UndertowWebServerFactoryCustomizer
		 */
		@Bean
		public UndertowWebServerFactoryCustomizer undertowWebServerFactoryCustomizer(Environment environment,
				ServerProperties serverProperties) {
			return new UndertowWebServerFactoryCustomizer(environment, serverProperties);
		}

		/**
		 * Returns an UndertowDeploymentInfoCustomizer bean that is conditionally created
		 * based on the threading type. If the threading type is set to VIRTUAL, the bean
		 * is created and configured with a VirtualThreadTaskExecutor.
		 * @return the UndertowDeploymentInfoCustomizer bean
		 */
		@Bean
		@ConditionalOnThreading(Threading.VIRTUAL)
		UndertowDeploymentInfoCustomizer virtualThreadsUndertowDeploymentInfoCustomizer() {
			return (deploymentInfo) -> deploymentInfo.setExecutor(new VirtualThreadTaskExecutor("undertow-"));
		}

	}

	/**
	 * Nested configuration if Netty is being used.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(HttpServer.class)
	public static class NettyWebServerFactoryCustomizerConfiguration {

		/**
		 * Creates a new instance of {@link NettyWebServerFactoryCustomizer} with the
		 * given environment and server properties.
		 * @param environment the environment object used to access application properties
		 * @param serverProperties the server properties object used to configure the web
		 * server
		 * @return a new instance of {@link NettyWebServerFactoryCustomizer}
		 */
		@Bean
		public NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(Environment environment,
				ServerProperties serverProperties) {
			return new NettyWebServerFactoryCustomizer(environment, serverProperties);
		}

	}

}
