/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.test;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.SpringApplicationWebIntegrationTestTests.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Tests for {@link IntegrationTest}
 *
 * @author Phillip Webb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Config.class)
@WebIntegrationTest({ "server.port=0", "value=123" })
public class SpringApplicationWebIntegrationTestTests {

	@Value("${local.server.port}")
	private int port = 0;

	@Value("${value}")
	private int value = 0;

	@Test
	public void runAndTestHttpEndpoint() {
		assertNotEquals(8080, this.port);
		assertNotEquals(0, this.port);
		String body = new RestTemplate()
				.getForObject("http://localhost:" + this.port + "/", String.class);
		assertEquals("Hello World", body);
	}

	@Test
	public void annotationAttributesOverridePropertiesFile() throws Exception {
		assertEquals(123, this.value);
	}

	@Configuration
	@EnableWebMvc
	@RestController
	protected static class Config {

		@Value("${server.port:8080}")
		private int port = 8080;

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public EmbeddedServletContainerFactory embeddedServletContainer() {
			TomcatEmbeddedServletContainerFactory factory = new TomcatEmbeddedServletContainerFactory();
			factory.setPort(this.port);
			return factory;
		}

		@Bean
		public static PropertySourcesPlaceholderConfigurer propertyPlaceholder() {
			return new PropertySourcesPlaceholderConfigurer();
		}

		@RequestMapping("/")
		public String home() {
			return "Hello World";
		}

	}

}
