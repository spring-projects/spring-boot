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

package org.springframework.boot.autoconfigure.web;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link ServerPropertiesAutoConfiguration}.
 * 
 * @author Dave Syer
 */
public class ServerPropertiesAutoConfigurationTests {

	private static ConfigurableEmbeddedServletContainerFactory containerFactory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Before
	public void init() {
		containerFactory = Mockito
				.mock(ConfigurableEmbeddedServletContainerFactory.class);
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context, "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
		assertEquals(9000, server.getPort().intValue());
		Mockito.verify(containerFactory).setPort(9000);
	}

	@Test
	public void tomcatProperties() throws Exception {
		containerFactory = Mockito.mock(TomcatEmbeddedServletContainerFactory.class);
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"server.tomcat.basedir:target/foo", "server.port:9000");
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
		assertEquals(new File("target/foo"), server.getTomcat().getBasedir());
		Mockito.verify(containerFactory).setPort(9000);
	}

	@Test
	public void customizeWithContainerFactory() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(CustomContainerConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		containerFactory = this.context
				.getBean(ConfigurableEmbeddedServletContainerFactory.class);
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
		// The server.port environment property was not explicitly set so the container
		// factory should take precedence...
		assertEquals(3000,
				((AbstractEmbeddedServletContainerFactory) containerFactory).getPort());
	}

	@Test
	public void customizeTomcatWithCustomizer() throws Exception {
		containerFactory = Mockito.mock(TomcatEmbeddedServletContainerFactory.class);
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, CustomizeConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
		// The server.port environment property was not explicitly set so the container
		// customizer should take precedence...
		Mockito.verify(containerFactory).setPort(3000);
	}

	@Test
	public void testAccidentalMultipleServerPropertiesBeans() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Config.class, MutiServerPropertiesBeanConfig.class,
				ServerPropertiesAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.thrown.expect(ApplicationContextException.class);
		this.thrown.expectMessage("Multiple ServerProperties");
		this.context.refresh();
	}

	@Configuration
	protected static class Config {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return ServerPropertiesAutoConfigurationTests.containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomContainerConfig {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory();
			factory.setPort(3000);
			return factory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

	@Configuration
	protected static class CustomizeConfig {

		@Bean
		public EmbeddedServletContainerCustomizer containerCustomizer() {
			return new EmbeddedServletContainerCustomizer() {

				@Override
				public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
					factory.setPort(3000);
				}
			};
		}

	}

	@Configuration
	protected static class MutiServerPropertiesBeanConfig {

		@Bean
		public ServerProperties serverPropertiesOne() {
			return new ServerProperties();
		}

		@Bean
		public ServerProperties serverPropertiesTwo() {
			return new ServerProperties();
		}

	}

}
