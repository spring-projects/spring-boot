/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.mustache.autoconfigure;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.mustache.servlet.view.MustacheView;
import org.springframework.boot.mustache.servlet.view.MustacheViewResolver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration}, {@link MustacheViewResolver}
 * and {@link MustacheView}.
 *
 * @author Dave Syer
 * @author Moritz Halbritter
 */
@DirtiesContext
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class MustacheAutoConfigurationServletIntegrationTests {

	@Autowired
	private ServletWebServerApplicationContext context;

	private int port;

	@BeforeEach
	void init() {
		this.port = this.context.getWebServer().getPort();
	}

	@Test
	void shouldRenderTemplate() {
		String source = "Hello {{arg}}!";
		Template tmpl = Mustache.compiler().compile(source);
		Map<String, String> context = new HashMap<>();
		context.put("arg", "world");
		Assertions.assertThat(tmpl.execute(context)).isEqualTo("Hello world!");
	}

	@Test
	void testHomePage() {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port, String.class);
		assertThat(body).contains("Hello World");
	}

	@Test
	void testPartialPage() {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port + "/partial", String.class);
		assertThat(body).contains("Hello World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import(MinimalWebConfiguration.class)
	@Controller
	static class Application {

		@RequestMapping("/")
		String home(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "home";
		}

		@RequestMapping("/partial")
		String layout(Map<String, Object> model) {
			model.put("time", new Date());
			model.put("message", "Hello World");
			model.put("title", "Hello App");
			return "partial";
		}

		@Bean
		MustacheViewResolver viewResolver() {
			Mustache.Compiler compiler = Mustache.compiler()
				.withLoader(new MustacheResourceTemplateLoader(
						"classpath:/org/springframework/boot/mustache/autoconfigure/", ".html"));
			MustacheViewResolver resolver = new MustacheViewResolver(compiler);
			resolver.setPrefix("classpath:/org/springframework/boot/mustache/autoconfigure/");
			resolver.setSuffix(".html");
			return resolver;
		}

		static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TomcatServletWebServerAutoConfiguration.class })
	static class MinimalWebConfiguration {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

}
