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

package org.springframework.boot.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.SpringApplicationIntegrationTestTests.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link IntegrationTest}
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Config.class)
@WebAppConfiguration
@IntegrationTest
public class SpringApplicationIntegrationTestTests {

	@Test
	public void nestedConfigClasses() {
		String body = new RestTemplate().getForObject("http://localhost:8080/",
				String.class);
		assertEquals("Hello World", body);
	}

	@Configuration
	@EnableWebMvc
	@RestController
	protected static class Config {

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public EmbeddedServletContainerFactory embeddedServletContainer() {
			return new TomcatEmbeddedServletContainerFactory();
		}

		@RequestMapping("/")
		public String home() {
			return "Hello World";
		}

	}

}
