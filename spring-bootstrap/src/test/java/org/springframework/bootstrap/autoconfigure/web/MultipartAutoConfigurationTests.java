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
package org.springframework.bootstrap.autoconfigure.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.context.embedded.ServletRegistrationBean;
import org.springframework.bootstrap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Greg Turnquist
 *
 */
public class MultipartAutoConfigurationTests {
	
	private AnnotationConfigEmbeddedWebApplicationContext context;
	
	@Test(expected=BeansException.class)
	@Ignore
	public void containerWithNothingJetty() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNothing.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(StandardServletMultipartResolver.class);
	}

	@Test(expected=BeansException.class)
	@Ignore
	public void containerWithNothingTomcat() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNothing.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(StandardServletMultipartResolver.class);
	}

	@Test(expected=BeansException.class)
	@Ignore
	public void containerWithNoMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNoMultipartJetty.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			//assertFalse(this.context.getBean(JettyEmbeddedServletContainerFactory.class).hasMultipart());
			this.context.getBean(StandardServletMultipartResolver.class);
		} finally {
			this.context.close();
		}
	}

	@Test(expected=BeansException.class)
	@Ignore
	public void containerWithNoMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNoMultipartTomcat.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			//assertFalse(this.context.getBean(TomcatEmbeddedServletContainerFactory.class).hasMultipart());
			this.context.getBean(StandardServletMultipartResolver.class);
		} finally {
			this.context.close();
		}
	}

	@Test
	@Ignore
	public void containerWithAutomatedMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithEverythingJetty.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			assertNotNull(this.context.getBean(MultipartConfigElement.class));
			assertNotNull(this.context.getBean(StandardServletMultipartResolver.class));
			//assertTrue(this.context.getBean(JettyEmbeddedServletContainerFactory.class).hasMultipart());
		} finally {
			this.context.close();
		}
	}

	@Configuration
	public static class ContainerWithNothing {
		
	}

	@Configuration
	public static class ContainerWithNoMultipartJetty {
		@Bean
		JettyEmbeddedServletContainerFactory containerFactory() {
			return new JettyEmbeddedServletContainerFactory();
		}
	}	

	@Configuration
	public static class ContainerWithNoMultipartTomcat {
		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
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
	}

	@Test
	public void containerWithAutomatedMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingTomcat.class,
				WebMvcAutoConfiguration.class,
				MultipartAutoConfiguration.class);
		try {
			assertNotNull(this.context.getBean(MultipartConfigElement.class));
			assertNotNull(this.context.getBean(StandardServletMultipartResolver.class));
			assertNotNull(this.context.getBean(ContainerWithEverythingTomcat.WebController.class));
			Servlet servlet = this.context.getBean(Servlet.class);
			//ServletRegistrationBean servletRegistrationBean = this.context.getBean(ServletRegistrationBean.class);
			RestTemplate restTemplate = new RestTemplate();
			assertEquals(restTemplate.getForObject("http://localhost:8080/", String.class), "Hello");
		} finally {
			this.context.close();
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
		
		@Controller
		public static class WebController {
			@RequestMapping("/")
			public @ResponseBody String index() {
				return "Hello";
			}
		}
	}

}
