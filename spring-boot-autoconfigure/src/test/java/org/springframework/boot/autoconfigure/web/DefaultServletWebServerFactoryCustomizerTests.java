/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.io.File;
import java.net.URL;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.WebServerFactoryCustomizerBeanPostProcessor;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration tests for {@link DefaultServletWebServerFactoryCustomizer}.
 *
 * @author Dave Syer
 * @author Ivan Sopov
 */
public class DefaultServletWebServerFactoryCustomizerTests {

	private static AbstractServletWebServerFactory webServerFactory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigServletWebServerApplicationContext context;

	@Before
	public void init() {
		webServerFactory = mock(AbstractServletWebServerFactory.class);
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
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(Config.class, PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(server.getPort().intValue()).isEqualTo(9000);
		verify(webServerFactory).setPort(9000);
	}

	@Test
	public void tomcatProperties() throws Exception {
		webServerFactory = mock(TomcatServletWebServerFactory.class);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(Config.class, PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"server.tomcat.basedir:target/foo", "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(server.getTomcat().getBasedir()).isEqualTo(new File("target/foo"));
		verify(webServerFactory).setPort(9000);
	}

	@Test
	public void customizeWithJettyWebServerFactory() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(CustomJettyWebServerConfig.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		webServerFactory = this.context.getBean(AbstractServletWebServerFactory.class);
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		// The server.port environment property was not explicitly set so the server
		// factory should take precedence...
		assertThat(webServerFactory.getPort()).isEqualTo(3000);
	}

	@Test
	public void customizeWithUndertowWebServerFactory() throws Exception {
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(CustomUndertowWebServerConfig.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		webServerFactory = this.context.getBean(AbstractServletWebServerFactory.class);
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		assertThat(webServerFactory.getPort()).isEqualTo(3000);
	}

	@Test
	public void customizeTomcatWithCustomizer() throws Exception {
		webServerFactory = mock(TomcatServletWebServerFactory.class);
		this.context = new AnnotationConfigServletWebServerApplicationContext();
		this.context.register(Config.class, CustomizeConfig.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertThat(server).isNotNull();
		// The server.port environment property was not explicitly set so the server
		// customizer should take precedence...
		verify(webServerFactory).setPort(3000);
	}

	@Configuration
	@EnableConfigurationProperties(ServerProperties.class)
	protected static class Config {

		@Bean
		public DefaultServletWebServerFactoryCustomizer defaultServletWebServerFactoryCustomizer(
				ServerProperties properties) {
			return new DefaultServletWebServerFactoryCustomizer(properties);
		}

		@Bean
		public ServletWebServerFactory webServerFactory() {
			return DefaultServletWebServerFactoryCustomizerTests.webServerFactory;
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	@EnableConfigurationProperties(ServerProperties.class)
	protected static class CustomJettyWebServerConfig {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
			factory.setPort(3000);
			return factory;
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	@EnableConfigurationProperties(ServerProperties.class)
	protected static class CustomUndertowWebServerConfig {

		@Bean
		public ServletWebServerFactory webServerFactory() {
			UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
			factory.setPort(3000);
			return factory;
		}

		@Bean
		public WebServerFactoryCustomizerBeanPostProcessor ServletWebServerCustomizerBeanPostProcessor() {
			return new WebServerFactoryCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomizeConfig {

		@Bean
		public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
			return (serverFactory) -> serverFactory.setPort(3000);
		}

	}

}
