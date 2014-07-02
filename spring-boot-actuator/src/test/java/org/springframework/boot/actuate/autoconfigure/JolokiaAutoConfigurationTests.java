/*
 * Copyright 2013-2014 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.junit.After;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.mvc.JolokiaMvcEndpoint;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.MockEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link JolokiaAutoConfiguration}.
 *
 * @author Christian Dupuis
 */
public class JolokiaAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
		if (Config.containerFactory != null) {
			Config.containerFactory = null;
		}
	}

	@Test
	public void agentServletRegisteredWithAppContext() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context, "jolokia.config[key1]:value1",
				"jolokia.config[key2]:value2");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				JolokiaAutoConfiguration.class);
		this.context.refresh();
		assertEquals(1, this.context.getBeanNamesForType(JolokiaMvcEndpoint.class).length);
	}

	@Test
	public void agentDisabled() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"endpoints.jolokia.enabled:false");
		this.context.register(Config.class, WebMvcAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				JolokiaAutoConfiguration.class);
		this.context.refresh();
		assertEquals(0, this.context.getBeanNamesForType(JolokiaMvcEndpoint.class).length);
	}

	@Configuration
	@EnableConfigurationProperties
	protected static class Config {

		protected static MockEmbeddedServletContainerFactory containerFactory = null;

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			if (containerFactory == null) {
				containerFactory = new MockEmbeddedServletContainerFactory();
			}
			return containerFactory;
		}

		@Bean
		public EmbeddedServletContainerCustomizerBeanPostProcessor embeddedServletContainerCustomizerBeanPostProcessor() {
			return new EmbeddedServletContainerCustomizerBeanPostProcessor();
		}

	}

}
