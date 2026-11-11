/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jetty.autoconfigure;

import java.io.IOException;

import jakarta.servlet.GenericServlet;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;

import org.springframework.boot.jetty.autoconfigure.AutoConfigureWebServerJettyServletTests.ServletConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.test.AutoConfigureWebServer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AutoConfigureWebServer @AutoConfigureWebServer} with Jetty.
 *
 * @author Phillip Webb
 */
@SpringBootTest(classes = ServletConfiguration.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebServer
class AutoConfigureWebServerJettyServletTests {

	@LocalServerPort
	private int port;

	@Test
	void startsTestServer() {
		RestTestClient restClient = RestTestClient.bindToServer().baseUrl("http://localhost:" + this.port).build();
		assertThat(RestTestClientResponse.from(restClient.get().exchange())).bodyText().isEqualTo("hello");
	}

	@Configuration
	static class ServletConfiguration {

		@Bean
		Servlet servlet() {
			return new GenericServlet() {

				@Override
				public void service(ServletRequest request, ServletResponse response)
						throws ServletException, IOException {
					response.getWriter().append("hello").flush();
				}

			};
		}

	}

}
