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

package org.springframework.boot.autoconfigure.jersey;

import java.nio.charset.StandardCharsets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.tomcat.util.buf.UDecoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfigurationServletContainerTests.Application;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testsupport.rule.OutputCapture;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify the behavior when deployed to a Servlet container where Jersey may
 * have already initialized itself.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(classes = Application.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@RunWith(SpringRunner.class)
public class JerseyAutoConfigurationServletContainerTests {

	@ClassRule
	public static final OutputCapture output = new OutputCapture();

	@Test
	public void existingJerseyServletIsAmended() {
		assertThat(output.toString())
				.contains("Configuring existing registration for Jersey servlet");
		assertThat(output.toString()).contains(
				"Servlet " + Application.class.getName() + " was not registered");
	}

	@ImportAutoConfiguration({ ServletWebServerFactoryAutoConfiguration.class,
			JerseyAutoConfiguration.class, PropertyPlaceholderAutoConfiguration.class })
	@Import(ContainerConfiguration.class)
	@Path("/hello")
	public static class Application extends ResourceConfig {

		@Value("${message:World}")
		private String msg;

		public Application() {
			register(Application.class);
		}

		@GET
		public String message() {
			return "Hello " + this.msg;
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class ContainerConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory() {

				@Override
				protected void postProcessContext(Context context) {
					Wrapper jerseyServlet = context.createWrapper();
					String servletName = Application.class.getName();
					jerseyServlet.setName(servletName);
					jerseyServlet.setServletClass(ServletContainer.class.getName());
					jerseyServlet.setServlet(new ServletContainer());
					jerseyServlet.setOverridable(false);
					context.addChild(jerseyServlet);
					String pattern = UDecoder.URLDecode("/*", StandardCharsets.UTF_8);
					context.addServletMappingDecoded(pattern, servletName);
				}

			};
		}

	}

}
