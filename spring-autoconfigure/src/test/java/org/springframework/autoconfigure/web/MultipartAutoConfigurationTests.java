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

package org.springframework.autoconfigure.web;

import javax.servlet.MultipartConfigElement;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Tests for {@link MultipartAutoConfiguration}. Tests an empty configuration, no
 * multipart configuration, and a multipart configuration (with both Jetty and Tomcat).
 * 
 * @author Greg Turnquist
 * @author Dave Syer
 */
public class MultipartAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void containerWithNothing() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNothing.class,
				EmbeddedServletContainerAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertNull(servlet.getMultipartResolver());
		assertEquals(0,
				this.context.getBeansOfType(StandardServletMultipartResolver.class)
						.size());
		assertEquals(0, this.context.getBeansOfType(MultipartResolver.class).size());
	}

	@Configuration
	public static class ContainerWithNothing {
	}

	@Test
	public void containerWithNoMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNoMultipartJetty.class,
				EmbeddedServletContainerAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertNull(servlet.getMultipartResolver());
		assertEquals(0,
				this.context.getBeansOfType(StandardServletMultipartResolver.class)
						.size());
		assertEquals(0, this.context.getBeansOfType(MultipartResolver.class).size());
		verifyServletWorks();
	}

	@Configuration
	public static class ContainerWithNoMultipartJetty {
		@Bean
		JettyEmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}
	}

	@Test
	public void containerWithNoMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithNoMultipartTomcat.class,
				EmbeddedServletContainerAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		DispatcherServlet servlet = this.context.getBean(DispatcherServlet.class);
		assertNull(servlet.getMultipartResolver());
		assertEquals(0,
				this.context.getBeansOfType(StandardServletMultipartResolver.class)
						.size());
		assertEquals(0, this.context.getBeansOfType(MultipartResolver.class).size());
		verifyServletWorks();
	}

	@Test
	public void containerWithAutomatedMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingJetty.class,
				EmbeddedServletContainerAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		assertSame(this.context.getBean(DispatcherServlet.class).getMultipartResolver(),
				this.context.getBean(StandardServletMultipartResolver.class));
		verifyServletWorks();
	}

	@Test
	public void containerWithAutomatedMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingTomcat.class,
				EmbeddedServletContainerAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		this.context.getBean(MultipartConfigElement.class);
		assertSame(this.context.getBean(DispatcherServlet.class).getMultipartResolver(),
				this.context.getBean(StandardServletMultipartResolver.class));
		verifyServletWorks();
	}

	private void verifyServletWorks() {
		RestTemplate restTemplate = new RestTemplate();
		assertEquals(restTemplate.getForObject("http://localhost:8080/", String.class),
				"Hello");
	}

	@Configuration
	public static class ContainerWithNoMultipartTomcat {

		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

		@Bean
		WebController controller() {
			return new WebController();
		}
	}

	@Configuration
	public static class ContainerWithEverythingJetty {
		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		JettyEmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}
	}

	@Configuration
	@EnableWebMvc
	public static class ContainerWithEverythingTomcat {
		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}

		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}

		@Bean
		WebController webController() {
			return new WebController();
		}
	}

	@Controller
	public static class WebController {
		@RequestMapping("/")
		public @ResponseBody
		String index() {
			return "Hello";
		}
	}

}
