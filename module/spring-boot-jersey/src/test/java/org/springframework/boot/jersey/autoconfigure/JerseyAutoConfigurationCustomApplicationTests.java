/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jersey.autoconfigure;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.boot.tomcat.autoconfigure.servlet.TomcatServletWebServerAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyAutoConfiguration} when using a custom {@link Application}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
class JerseyAutoConfigurationCustomApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		String uri = LocalTestWebServer.obtain(this.applicationContext).uri("/test/hello");
		ResponseEntity<String> response = RestClient.create().get().uri(uri).retrieve().toEntity(String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@ApplicationPath("/test")
	static class TestApplication extends Application {

	}

	@Path("/hello")
	public static class TestController {

		@GET
		public String message() {
			return "Hello World";
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ TomcatServletWebServerAutoConfiguration.class, JerseyAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class TestConfiguration {

		@Configuration(proxyBeanMethods = false)
		public class JerseyConfiguration {

			@Bean
			public TestApplication testApplication() {
				return new TestApplication();
			}

			@Bean
			public ResourceConfig conf(TestApplication app) {
				ResourceConfig config = ResourceConfig.forApplication(app);
				config.register(TestController.class);
				return config;
			}

		}

	}

}
