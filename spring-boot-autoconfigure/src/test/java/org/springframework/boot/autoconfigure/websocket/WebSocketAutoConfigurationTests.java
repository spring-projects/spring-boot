/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import java.net.URL;

import javax.websocket.server.ServerContainer;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

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

	@BeforeClass
	@AfterClass
	public static void uninstallUrlStreamHandlerFactory() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance",
				null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
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
		Object serverContainer = this.context.getServletContext()
				.getAttribute("javax.websocket.server.ServerContainer");
		assertThat(serverContainer).isInstanceOf(ServerContainer.class);

	}

	@Configuration
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
