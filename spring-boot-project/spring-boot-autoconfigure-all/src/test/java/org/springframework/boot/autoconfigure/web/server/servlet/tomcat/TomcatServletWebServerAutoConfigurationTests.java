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

package org.springframework.boot.autoconfigure.web.server.servlet.tomcat;

import jakarta.servlet.Filter;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.server.servlet.AbstractServletWebServerAutoConfigurationTests;
import org.springframework.boot.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TomcatServletWebServerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
class TomcatServletWebServerAutoConfigurationTests extends AbstractServletWebServerAutoConfigurationTests {

	TomcatServletWebServerAutoConfigurationTests() {
		super(TomcatServletWebServerAutoConfiguration.class);
	}

	@Test
	void tomcatConnectorCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatConnectorCustomizerConfiguration.class).run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
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
				TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
				TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
						TomcatConnectorCustomizer.class);
				assertThat(factory.getConnectorCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Connector.class));
			});
	}

	@Test
	void tomcatContextCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatContextCustomizerConfiguration.class).run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
			assertThat(factory.getContextCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Context.class));
		});
	}

	@Test
	void tomcatContextCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationTomcatContextCustomizerConfiguration.class)
			.run((context) -> {
				TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
				TomcatContextCustomizer customizer = context.getBean("contextCustomizer",
						TomcatContextCustomizer.class);
				assertThat(factory.getContextCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Context.class));
			});
	}

	@Test
	void tomcatProtocolHandlerCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(TomcatProtocolHandlerCustomizerConfiguration.class).run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
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
				TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
				TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
						TomcatProtocolHandlerCustomizer.class);
				assertThat(factory.getProtocolHandlerCustomizers()).contains(customizer);
				then(customizer).should().customize(any());
			});
	}

	@Test
	void whenUsingFrameworkForwardHeadersStrategyAndRelativeRedirectsAreEnabledThenFilterIsConfiguredToUseRelativeRedirects() {
		this.serverRunner
			.withPropertyValues("server.forward-headers-strategy=framework",
					"server.tomcat.use-relative-redirects=true", "server.port=0")
			.run((context) -> {
				Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
				assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
				assertThat(filter).extracting("relativeRedirects").isEqualTo(true);
			});
	}

	@Test
	void whenUsingFrameworkForwardHeadersStrategyAndNotUsingRelativeRedirectsThenFilterIsNotConfiguredToUseRelativeRedirects() {
		this.serverRunner
			.withPropertyValues("server.forward-headers-strategy=framework",
					"server.tomcat.use-relative-redirects=false", "server.port=0")
			.run((context) -> {
				Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
				assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
				assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
			});
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
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
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
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
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
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addProtocolHandlerCustomizers(this.customizer);
		}

	}

}
