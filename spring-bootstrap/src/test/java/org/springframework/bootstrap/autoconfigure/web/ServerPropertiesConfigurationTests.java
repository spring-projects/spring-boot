/*
 * Copyright 2012-2013 the original author or authors.
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
package org.springframework.bootstrap.autoconfigure.web;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.bootstrap.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.bootstrap.properties.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 * 
 */
public class ServerPropertiesConfigurationTests {

	private static ConfigurableEmbeddedServletContainerFactory containerFactory;

	private AnnotationConfigEmbeddedWebApplicationContext context;

	private Map<String, Object> environment = new HashMap<String, Object>();

	@Before
	public void init() {
		containerFactory = Mockito
				.mock(ConfigurableEmbeddedServletContainerFactory.class);
	}

	@Test
	public void createFromConfigClass() throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(EmbeddedContainerConfiguration.class,
				ServerPropertiesConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.environment.put("server.port", "9000");
		this.context.getEnvironment().getPropertySources()
				.addFirst(new MapPropertySource("test", this.environment));
		this.context.refresh();
		ServerProperties server = this.context.getBean(ServerProperties.class);
		assertNotNull(server);
		assertEquals(9000, server.getPort());
	}

	@Configuration
	protected static class EmbeddedContainerConfiguration {

		@Bean
		public EmbeddedServletContainerFactory containerFactory() {
			return ServerPropertiesConfigurationTests.containerFactory;
		}

	}

}
