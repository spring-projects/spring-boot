/*
 * Copyright 2012-2016 the original author or authors.
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

import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.SpringApplicationWebIntegrationTestTests.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IntegrationTest}
 *
 * @author Phillip Webb
 */
@SuppressWarnings("deprecation")
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringApplicationConfiguration(Config.class)
@WebIntegrationTest({ "server.port=0", "value=123" })
public class SpringApplicationWebIntegrationTestTests {

	@LocalServerPort
	private int port = 0;

	@Value("${value}")
	private int value = 0;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	@Test
	public void runAndTestHttpEndpoint() {
		assertThat(this.port).isNotEqualTo(8080).isNotEqualTo(0);
		String body = new RestTemplate()
				.getForObject("http://localhost:" + this.port + "/", String.class);
		assertThat(body).isEqualTo("Hello World");
	}

	@Test
	public void annotationAttributesOverridePropertiesFile() throws Exception {
		assertThat(this.value).isEqualTo(123);
	}

	@Test
	public void validateWebApplicationContextIsSet() {
		assertThat(this.context).isSameAs(
				WebApplicationContextUtils.getWebApplicationContext(this.servletContext));
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
