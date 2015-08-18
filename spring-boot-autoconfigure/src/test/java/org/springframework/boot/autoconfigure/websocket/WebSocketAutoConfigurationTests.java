/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import javax.websocket.server.ServerContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link WebSocketAutoConfiguration}
 *
 * @author Andy Wilkinson
 */
public class WebSocketAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Before
	public void createContext() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void tomcatServerContainerIsAvailableFromTheServletContext() {
		serverContainerIsAvailableFromTheServletContext(TomcatConfiguration.class,
				WebSocketAutoConfiguration.TomcatWebSocketConfiguration.class);
	}

	@Test
	public void jettyServerContainerIsAvailableFromTheServletContext() {
		serverContainerIsAvailableFromTheServletContext(JettyConfiguration.class,
				WebSocketAutoConfiguration.JettyWebSocketConfiguration.class);
	}

	private void serverContainerIsAvailableFromTheServletContext(
			Class<?>... configuration) {
		this.context.register(configuration);
		this.context.refresh();
		Object serverContainer = this.context.getServletContext().getAttribute(
				"javax.websocket.server.ServerContainer");
		assertThat(serverContainer, is(instanceOf(ServerContainer.class)));

	}

	static class CommonConfiguration {

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	static class TomcatConfiguration extends CommonConfiguration {

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory = new TomcatEmbeddedServletContainerFactory();
			tomcatEmbeddedServletContainerFactory.setPort(0);
			return tomcatEmbeddedServletContainerFactory;
		}

	}

	@Configuration
	static class JettyConfiguration extends CommonConfiguration {

		@Bean
		public EmbeddedServletContainerFactory servletContainerFactory() {
			JettyEmbeddedServletContainerFactory jettyEmbeddedServletContainerFactory = new JettyEmbeddedServletContainerFactory();
			jettyEmbeddedServletContainerFactory.setPort(0);
			return jettyEmbeddedServletContainerFactory;
		}

	}

}
