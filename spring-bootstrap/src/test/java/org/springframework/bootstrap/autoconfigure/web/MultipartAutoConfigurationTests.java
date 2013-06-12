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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.servlet.MultipartConfigElement;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.bootstrap.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.bootstrap.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.bootstrap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

/**
 * @author Greg Turnquist
 *
 */
public class MultipartAutoConfigurationTests {
	
	private AnnotationConfigEmbeddedWebApplicationContext context;
	
	@Test(expected=BeansException.class)
	public void containerWithNothingJetty() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNothing.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(StandardServletMultipartResolver.class);
	}

	@Test(expected=BeansException.class)
	public void containerWithNothingTomcat() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNothing.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		this.context.getBean(StandardServletMultipartResolver.class);
	}

	@Test(expected=BeansException.class)
	public void containerWithNoMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNoMultipartJetty.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			assertFalse(this.context.getBean(JettyEmbeddedServletContainerFactory.class).hasMultipart());
			this.context.getBean(StandardServletMultipartResolver.class);
		} finally {
			this.context.close();
		}
	}

	@Test(expected=BeansException.class)
	public void containerWithNoMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithNoMultipartTomcat.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			assertFalse(this.context.getBean(TomcatEmbeddedServletContainerFactory.class).hasMultipart());
			this.context.getBean(StandardServletMultipartResolver.class);
		} finally {
			this.context.close();
		}
	}

	@Test
	public void containerWithAutomatedMultipartJettyConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(
				ContainerWithEverythingJetty.class,
				MultipartAutoConfiguration.class);
		this.context.refresh();
		try {
			assertNotNull(this.context.getBean(MultipartConfigElement.class));
			assertNotNull(this.context.getBean(StandardServletMultipartResolver.class));
			assertTrue(this.context.getBean(JettyEmbeddedServletContainerFactory.class).hasMultipart());
		} finally {
			this.context.close();
		}
	}

	@Test
	public void containerWithAutomatedMultipartTomcatConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext(
				ContainerWithEverythingTomcat.class,
				MultipartAutoConfiguration.class);
		try {
			assertNotNull(this.context.getBean(MultipartConfigElement.class));
			assertNotNull(this.context.getBean(StandardServletMultipartResolver.class));
			assertTrue(this.context.getBean(TomcatEmbeddedServletContainerFactory.class).hasMultipart());
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

	@Configuration
	public static class ContainerWithEverythingTomcat {
		@Bean
		MultipartConfigElement multipartConfigElement() {
			return new MultipartConfigElement("");
		}
		
		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory();
		}
	}

}
