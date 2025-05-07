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

package org.springframework.boot.webmvc.autoconfigure.error;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.boot.web.error.ErrorPage;
import org.springframework.boot.web.error.ErrorPageRegistrar;
import org.springframework.boot.web.error.ErrorPageRegistry;
import org.springframework.boot.web.server.test.LocalServerPort;
import org.springframework.boot.web.server.test.client.TestRestTemplate;
import org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for remapped error pages.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "spring.mvc.servlet.path:/spring/")
@DirtiesContext
class RemappedErrorViewIntegrationTests {

	@LocalServerPort
	private int port;

	private final TestRestTemplate template = new TestRestTemplate();

	@Test
	void directAccessToErrorPage() {
		String content = this.template.getForObject("http://localhost:" + this.port + "/spring/error", String.class);
		assertThat(content).contains("error");
		assertThat(content).contains("999");
	}

	@Test
	void forwardToErrorPage() {
		String content = this.template.getForObject("http://localhost:" + this.port + "/spring/", String.class);
		assertThat(content).contains("error");
		assertThat(content).contains("500");
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ PropertyPlaceholderAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, TomcatServletWebServerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
	@Controller
	static class TestConfiguration implements ErrorPageRegistrar {

		@RequestMapping("/")
		String home() {
			throw new RuntimeException("Planned!");
		}

		@Override
		public void registerErrorPages(ErrorPageRegistry errorPageRegistry) {
			errorPageRegistry.addErrorPages(new ErrorPage("/spring/error"));
		}

		// For manual testing
		static void main(String[] args) {
			new SpringApplicationBuilder(TestConfiguration.class).properties("spring.mvc.servlet.path:spring/*")
				.run(args);
		}

	}

}
