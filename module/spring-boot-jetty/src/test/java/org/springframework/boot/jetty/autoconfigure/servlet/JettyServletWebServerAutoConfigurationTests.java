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

package org.springframework.boot.jetty.autoconfigure.servlet;

import java.util.Map;

import jakarta.servlet.Filter;
import org.eclipse.jetty.ee10.websocket.servlet.WebSocketUpgradeFilter;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.autoconfigure.servlet.AbstractServletWebServerAutoConfigurationTests;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Base class for testing sub-classes of {@link JettyServletWebServerAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
class JettyServletWebServerAutoConfigurationTests extends AbstractServletWebServerAutoConfigurationTests {

	protected JettyServletWebServerAutoConfigurationTests() {
		super(JettyServletWebServerAutoConfiguration.class);
	}

	@Test
	void jettyServerCustomizerBeanIsAddedToFactory() {
		this.serverRunner.withUserConfiguration(JettyServerCustomizerConfiguration.class).run((context) -> {
			JettyServletWebServerFactory factory = context.getBean(JettyServletWebServerFactory.class);
			assertThat(factory.getServerCustomizers())
				.contains(context.getBean("serverCustomizer", JettyServerCustomizer.class));
		});
	}

	@Test
	void jettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		this.serverRunner.withUserConfiguration(DoubleRegistrationJettyServerCustomizerConfiguration.class)
			.run((context) -> {
				JettyServletWebServerFactory factory = context.getBean(JettyServletWebServerFactory.class);
				JettyServerCustomizer customizer = context.getBean("serverCustomizer", JettyServerCustomizer.class);
				assertThat(factory.getServerCustomizers()).contains(customizer);
				then(customizer).should().customize(any(Server.class));
			});
	}

	@Test
	void jettyWebSocketUpgradeFilterIsAddedToServletContex() {
		this.serverRunner.run((context) -> assertThat(
				context.getServletContext().getFilterRegistration(WebSocketUpgradeFilter.class.getName()))
			.isNotNull());
	}

	@Test
	@SuppressWarnings("rawtypes")
	void jettyWebSocketUpgradeFilterIsNotExposedAsABean() {
		this.serverRunner.run((context) -> {
			Map<String, Filter> filters = context.getBeansOfType(Filter.class);
			assertThat(filters.values()).noneMatch(WebSocketUpgradeFilter.class::isInstance);
			Map<String, AbstractFilterRegistrationBean> filterRegistrations = context
				.getBeansOfType(AbstractFilterRegistrationBean.class);
			assertThat(filterRegistrations.values()).extracting(AbstractFilterRegistrationBean::getFilter)
				.noneMatch(WebSocketUpgradeFilter.class::isInstance);
		});
	}

	@Test
	void jettyWebSocketUpgradeFilterServletContextInitializerBacksOffWhenBeanWithSameNameIsDefined() {
		this.serverRunner
			.withUserConfiguration(CustomWebSocketUpgradeFilterServletContextInitializerConfiguration.class)
			.run((context) -> {
				BeanDefinition definition = context.getBeanFactory()
					.getBeanDefinition("websocketUpgradeFilterServletContextInitializer");
				assertThat(definition.getFactoryBeanName())
					.contains("CustomWebSocketUpgradeFilterServletContextInitializerConfiguration");
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class JettyServerCustomizerConfiguration {

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return (server) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationJettyServerCustomizerConfiguration {

		private final JettyServerCustomizer customizer = mock(JettyServerCustomizer.class);

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyCustomizer() {
			return (jetty) -> jetty.addServerCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomWebSocketUpgradeFilterServletContextInitializerConfiguration {

		@Bean
		ServletContextInitializer websocketUpgradeFilterServletContextInitializer() {
			return (servletContext) -> {

			};
		}

	}

}
