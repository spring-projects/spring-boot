/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded.jetty;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.junit.runner.classpath.ClassPathExclusions;
import org.springframework.boot.junit.runner.classpath.ClassPathOverrides;
import org.springframework.boot.junit.runner.classpath.ModifiedClassPathRunner;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JettyEmbeddedServletContainerFactory} with Jetty 8.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions({ "jetty-*.jar", "tomcat-embed-jasper-*.jar" })
@ClassPathOverrides({ "org.eclipse.jetty:jetty-servlets:8.1.15.v20140411",
		"org.eclipse.jetty:jetty-webapp:8.1.15.v20140411" })
public class Jetty8JettyEmbeddedServletContainerFactoryTests {

	@Test
	public void errorHandling() {
		JettyEmbeddedServletContainerFactory factory = new JettyEmbeddedServletContainerFactory(
				0);
		factory.addErrorPages(new ErrorPage("/error"));
		EmbeddedServletContainer jetty = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {

					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContext.addServlet("test", new TestServlet())
								.addMapping("/test");
						servletContext.addServlet("error", new ErrorPageServlet())
								.addMapping("/error");
					}

				});
		jetty.start();
		try {
			int port = jetty.getPort();
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.setErrorHandler(new ResponseErrorHandler() {

				@Override
				public boolean hasError(ClientHttpResponse response) throws IOException {
					return false;
				}

				@Override
				public void handleError(ClientHttpResponse response) throws IOException {
				}

			});
			ResponseEntity<String> response = restTemplate
					.getForEntity("http://localhost:" + port, String.class);
			assertThat(response.getBody()).isEqualTo("An error occurred");
		}
		finally {
			jetty.stop();
		}
	}

	private static final class TestServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws ServletException, IOException {
			throw new RuntimeException("boom");
		}

	}

	private static final class ErrorPageServlet extends HttpServlet {

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp)
				throws IOException {
			resp.getWriter().print("An error occurred");
			resp.flushBuffer();
		}

	}

}
