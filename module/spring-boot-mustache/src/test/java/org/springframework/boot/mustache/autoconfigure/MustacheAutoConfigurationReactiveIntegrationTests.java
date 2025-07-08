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

import com.samskivert.mustache.Mustache;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.mustache.reactive.view.MustacheView;
import org.springframework.boot.mustache.reactive.view.MustacheViewResolver;
import org.springframework.boot.reactor.netty.autoconfigure.NettyReactiveWebServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.ViewResolverRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for {@link MustacheAutoConfiguration}, {@link MustacheViewResolver}
 * and {@link MustacheView}.
 *
 * @author Brian Clozel
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.main.web-application-type=reactive")
class MustacheAutoConfigurationReactiveIntegrationTests {

	@Autowired
	private WebTestClient client;

	@Test
	void testHomePage() {
		String result = this.client.get()
			.uri("/")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();
		assertThat(result).contains("Hello App").contains("Hello World");
	}

	@Test
	void testPartialPage() {
		String result = this.client.get()
			.uri("/partial")
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();
		assertThat(result).contains("Hello App").contains("Hello World");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ NettyReactiveWebServerAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	@Controller
	@EnableWebFlux
	static class Application {

		@Bean
		WebFluxConfigurer registerViewResolvers(ObjectProvider<ViewResolver> viewResolvers) {
			return new WebFluxConfigurer() {
				@Override
				public void configureViewResolvers(ViewResolverRegistry registry) {
					viewResolvers.orderedStream().forEach(registry::viewResolver);
				}
			};
		}

		@Bean
		HttpHandler httpHandler(ApplicationContext context) {
			return WebHttpHandlerBuilder.applicationContext(context).build();
		}

		@RequestMapping("/")
		String home(Model model) {
			model.addAttribute("time", new Date());
			model.addAttribute("message", "Hello World");
			model.addAttribute("title", "Hello App");
			return "home";
		}

		@RequestMapping("/partial")
		String layout(Model model) {
			model.addAttribute("time", new Date());
			model.addAttribute("message", "Hello World");
			model.addAttribute("title", "Hello App");
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
			SpringApplication application = new SpringApplication(Application.class);
			application.setWebApplicationType(WebApplicationType.REACTIVE);
			application.run(args);
		}

	}

}
