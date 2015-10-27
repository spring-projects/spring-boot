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

package org.springframework.boot.autoconfigure.mustache.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheAutoConfiguration;
import org.springframework.boot.autoconfigure.mustache.MustacheResourceTemplateLoader;
import org.springframework.boot.autoconfigure.mustache.web.MustacheWebIntegrationTests.Application;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Integration Tests for {@link MustacheAutoConfiguration}, {@link MustacheViewResolver}
 * and {@link MustacheView}.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(Application.class)
@IntegrationTest("server.port:0")
@WebAppConfiguration
public class MustacheWebIntegrationTests {

	@Autowired
	private EmbeddedWebApplicationContext context;
	private int port;

	@Before
	public void init() {
		this.port = this.context.getEmbeddedServletContainer().getPort();
	}

	@Test
	public void contextLoads() {
		String source = "Hello {{arg}}!";
		Template tmpl = Mustache.compiler().compile(source);
		Map<String, String> context = new HashMap<String, String>();
		context.put("arg", "world");
		assertEquals("Hello world!", tmpl.execute(context)); // returns "Hello world!"
	}

	@Test
	public void testHomePage() throws Exception {
		String body = new TestRestTemplate().getForObject("http://localhost:" + this.port,
				String.class);
		assertTrue(body.contains("Hello World"));
	}

	@Test
	public void testPartialPage() throws Exception {
		String body = new TestRestTemplate()
				.getForObject("http://localhost:" + this.port + "/partial", String.class);
		assertTrue(body.contains("Hello World"));
	}

	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {
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

		@Bean
		public MustacheViewResolver viewResolver() {
			MustacheViewResolver resolver = new MustacheViewResolver();
			resolver.setPrefix("classpath:/mustache-templates/");
			resolver.setSuffix(".html");
			resolver.setCompiler(
					Mustache.compiler().withLoader(new MustacheResourceTemplateLoader(
							"classpath:/mustache-templates/", ".html")));
			return resolver;
		}

		public static void main(String[] args) {
			SpringApplication.run(Application.class, args);
		}

	}

}
