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

package org.springframework.boot.web.servlet;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.web.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Integration tests for {@link ServletComponentScan}
 *
 * @author Andy Wilkinson
 */
public class ServletComponentScanIntegrationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@After
	public void cleanUp() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void componentsAreRegistered() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(TestConfiguration.class);
		new ServerPortInfoApplicationContextInitializer().initialize(this.context);
		this.context.refresh();
		String port = this.context.getEnvironment().getProperty("local.server.port");
		String response = new RestTemplate()
				.getForObject("http://localhost:" + port + "/test", String.class);
		assertThat(response, is(equalTo("alpha bravo")));
	}

	@Configuration
	@ServletComponentScan(basePackages = "org.springframework.boot.web.servlet.testcomponents")
	static class TestConfiguration {

		@Bean
		public TomcatEmbeddedServletContainerFactory servletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory(0);
		}

	}

}
