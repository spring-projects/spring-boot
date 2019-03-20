/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MustacheAutoConfiguration}.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"spring.mustache.prefix:classpath:/mustache-templates/" })
public class MustacheAutoConfigurationIntegrationTests {

	@Autowired
	private EmbeddedWebApplicationContext context;

	private int port;

	@Before
	public void init() {
		this.port = this.context.getEmbeddedServletContainer().getPort();
	}

	@Test
	public void testHomePage() throws Exception {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port,
				String.class);
		assertThat(body.contains("Hello World")).isTrue();
	}

	@Test
	public void testPartialPage() throws Exception {
		String body = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port + "/partial", String.class);
		assertThat(body.contains("Hello World")).isTrue();
	}

	@Configuration
	@MinimalWebConfiguration
	@Controller
	public static class Application {

		@RequestMapping("/")
		public String home(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "home";
		}

		@RequestMapping("/partial")
		public String layout(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "partial";
		}

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ MustacheAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

}
