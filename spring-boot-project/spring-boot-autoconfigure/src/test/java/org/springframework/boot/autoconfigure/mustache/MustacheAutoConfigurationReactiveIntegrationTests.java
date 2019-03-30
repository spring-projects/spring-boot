/*
 * Copyright 2012-2019 the original author or authors.
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

import java.util.Date;

import com.samskivert.mustache.Mustache;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.reactive.result.view.MustacheView;
import org.springframework.boot.web.reactive.result.view.MustacheViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration}, {@link MustacheViewResolver}
 * and {@link MustacheView}.
 *
 * @author Brian Clozel
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.web-application-type=reactive")
public class MustacheAutoConfigurationReactiveIntegrationTests {

	@Autowired
	private WebTestClient client;

	@Test
	public void testHomePage() {
		String result = this.client.get().uri("/").exchange().expectStatus().isOk()
				.expectBody(String.class).returnResult().getResponseBody();
		assertThat(result).contains("Hello App").contains("Hello World");
	}

	@Test
	public void testPartialPage() {
		String result = this.client.get().uri("/partial").exchange().expectStatus().isOk()
				.expectBody(String.class).returnResult().getResponseBody();
		assertThat(result).contains("Hello App").contains("Hello World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ ReactiveWebServerFactoryAutoConfiguration.class,
			WebFluxAutoConfiguration.class, HttpHandlerAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	@Controller
	public static class Application {

		@RequestMapping("/")
		public String home(Model model) {
			model.addAttribute("time", new Date());
			model.addAttribute("message", "Hello World");
			model.addAttribute("title", "Hello App");
			return "home";
		}

		@RequestMapping("/partial")
		public String layout(Model model) {
			model.addAttribute("time", new Date());
			model.addAttribute("message", "Hello World");
			model.addAttribute("title", "Hello App");
			return "partial";
		}

		@Bean
		public MustacheViewResolver viewResolver() {
			Mustache.Compiler compiler = Mustache.compiler().withLoader(
					new MustacheResourceTemplateLoader("classpath:/mustache-templates/",
							".html"));
			MustacheViewResolver resolver = new MustacheViewResolver(compiler);
			resolver.setPrefix("classpath:/mustache-templates/");
			resolver.setSuffix(".html");
			return resolver;
		}

		public static void main(String[] args) {
			SpringApplication application = new SpringApplication(Application.class);
			application.setWebApplicationType(WebApplicationType.REACTIVE);
			application.run(args);
		}

	}

}
