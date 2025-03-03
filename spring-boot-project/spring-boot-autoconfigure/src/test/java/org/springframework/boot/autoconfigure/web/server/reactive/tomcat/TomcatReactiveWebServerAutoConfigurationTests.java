/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.server.reactive.tomcat;

import jakarta.servlet.ServletContext;
import jakarta.websocket.server.ServerContainer;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.server.reactive.AbstractReactiveWebServerAutoConfigurationTests;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.reactive.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatReactiveWebServerAutoConfiguration}.
 *
 * @author Brian Clozel
 * @author Raheela Aslam
 * @author Madhura Bhave
 * @author Scott Frederick
 */
class TomcatReactiveWebServerAutoConfigurationTests extends AbstractReactiveWebServerAutoConfigurationTests {

	TomcatReactiveWebServerAutoConfigurationTests() {
		super(TomcatReactiveWebServerAutoConfiguration.class);
	}

	@Test
	void tomcatConnectorCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatConnectorCustomizerConfiguration.class).run((context) -> {
			TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
			TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
					TomcatConnectorCustomizer.class);
			assertThat(factory.getConnectorCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Connector.class));
		});
	}

	@Test
	void tomcatConnectorCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationTomcatConnectorCustomizerConfiguration.class)
			.run((context) -> {
				TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
				TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
						TomcatConnectorCustomizer.class);
				assertThat(factory.getConnectorCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Connector.class));
			});
	}

	@Test
	void tomcatContextCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatContextCustomizerConfiguration.class).run((context) -> {
			TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
			TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
			assertThat(factory.getContextCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Context.class));
		});
	}

	@Test
	void tomcatContextCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationTomcatContextCustomizerConfiguration.class)
			.run((context) -> {
				TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
				TomcatContextCustomizer customizer = context.getBean("contextCustomizer",
						TomcatContextCustomizer.class);
				assertThat(factory.getContextCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Context.class));
			});
	}

	@Test
	void tomcatProtocolHandlerCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatProtocolHandlerCustomizerConfiguration.class).run((context) -> {
			TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
			TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
					TomcatProtocolHandlerCustomizer.class);
			assertThat(factory.getProtocolHandlerCustomizers()).contains(customizer);
			then(customizer).should().customize(any());
		});
	}

	@Test
	void tomcatProtocolHandlerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration.class)
			.run((context) -> {
				TomcatReactiveWebServerFactory factory = context.getBean(TomcatReactiveWebServerFactory.class);
				TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
						TomcatProtocolHandlerCustomizer.class);
				assertThat(factory.getProtocolHandlerCustomizers()).contains(customizer);
				then(customizer).should().customize(any());
			});
	}

	@Test
	void webSocketServerContainerIsAvailableFromServletContext() {
		this.serverRunner.run((context) -> {
			WebServer webServer = ((ReactiveWebServerApplicationContext) context.getSourceApplicationContext())
				.getWebServer();
			ServletContext servletContext = findContext(((TomcatWebServer) webServer).getTomcat()).getServletContext();
			Object serverContainer = servletContext.getAttribute("jakarta.websocket.server.ServerContainer");
			assertThat(serverContainer).isInstanceOf(ServerContainer.class);
		});
	}

	private static Context findContext(Tomcat tomcat) {
		for (Container child : tomcat.getHost().findChildren()) {
			if (child instanceof Context context) {
				return context;
			}
		}
		throw new IllegalStateException("The Host does not contain a Context");
	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConnectorCustomizerConfiguration {

		@Bean
		TomcatConnectorCustomizer connectorCustomizer() {
			return mock(TomcatConnectorCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatConnectorCustomizerConfiguration {

		private final TomcatConnectorCustomizer customizer = mock(TomcatConnectorCustomizer.class);

		@Bean
		TomcatConnectorCustomizer connectorCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addConnectorCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatContextCustomizerConfiguration {

		@Bean
		TomcatContextCustomizer contextCustomizer() {
			return mock(TomcatContextCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatContextCustomizerConfiguration {

		private final TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);

		@Bean
		TomcatContextCustomizer contextCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addContextCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatProtocolHandlerCustomizerConfiguration {

		@Bean
		TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
			return mock(TomcatProtocolHandlerCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration {

		private final TomcatProtocolHandlerCustomizer<?> customizer = mock(TomcatProtocolHandlerCustomizer.class);

		@Bean
		TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addProtocolHandlerCustomizers(this.customizer);
		}

	}

}
