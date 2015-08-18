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

package org.springframework.boot.autoconfigure.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.RemappedErrorViewIntegrationTests.TestConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Controller;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertTrue;

/**
 * @author Dave Syer
 */
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@IntegrationTest({ "server.servletPath:/spring/*", "server.port:0" })
@DirtiesContext
public class RemappedErrorViewIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	private RestTemplate template = new TestRestTemplate();

	@Test
	public void directAccessToErrorPage() throws Exception {
		String content = this.template.getForObject("http://localhost:" + this.port
				+ "/spring/error", String.class);
		assertTrue("Wrong content: " + content, content.contains("error"));
		assertTrue("Wrong content: " + content, content.contains("999"));
	}

	@Test
	public void forwardToErrorPage() throws Exception {
		String content = this.template.getForObject("http://localhost:" + this.port
				+ "/spring/", String.class);
		assertTrue("Wrong content: " + content, content.contains("error"));
		assertTrue("Wrong content: " + content, content.contains("500"));
	}

	@Configuration
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, ErrorMvcAutoConfiguration.class })
	@Controller
	public static class TestConfiguration implements EmbeddedServletContainerCustomizer {

		@RequestMapping("/")
		public String home() {
			throw new RuntimeException("Planned!");
		}

		@Override
		public void customize(ConfigurableEmbeddedServletContainer container) {
			container.addErrorPages(new ErrorPage("/spring/error"));
		}

		// For manual testing
		public static void main(String[] args) {
			new SpringApplicationBuilder(TestConfiguration.class).properties(
					"server.servletPath:spring/*").run(args);
		}

	}

}
