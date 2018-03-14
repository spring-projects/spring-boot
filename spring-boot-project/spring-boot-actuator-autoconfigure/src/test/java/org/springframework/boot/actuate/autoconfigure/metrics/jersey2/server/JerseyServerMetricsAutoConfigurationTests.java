/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.jersey2.server;

import java.net.URI;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.jersey2.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JerseyServerMetricsAutoConfiguration}.
 *
 * @author Michael Weirauch
 */
public class JerseyServerMetricsAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
					.withConfiguration(
							AutoConfigurations.of(JerseyAutoConfiguration.class,
									JerseyServerMetricsAutoConfiguration.class,
									ServletWebServerFactoryAutoConfiguration.class,
									SimpleMetricsExportAutoConfiguration.class,
									MetricsAutoConfiguration.class))
					.withUserConfiguration(ResourceConfiguration.class)
					.withPropertyValues("server.port:0");

	@Test
	public void httpRequestsAreTimed() {
		this.contextRunner.run((context) -> {
			doRequest(context);

			MeterRegistry registry = context.getBean(MeterRegistry.class);
			Timer timer = registry.get("http.server.requests").tag("uri", "/users/{id}")
					.timer();
			assertThat(timer.count()).isEqualTo(1);
		});
	}

	@Test
	public void noHttpRequestsTimedWhenJerseyInstrumentationMissingFromClasspath() {
		this.contextRunner
				.withClassLoader(
						new FilteredClassLoader(MetricsApplicationEventListener.class))
				.run((context) -> {
					doRequest(context);

					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("http.server.requests").timer()).isNull();
				});
	}

	private static void doRequest(AssertableWebApplicationContext context) {
		int port = context
				.getSourceApplicationContext(
						AnnotationConfigServletWebServerApplicationContext.class)
				.getWebServer().getPort();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getForEntity(URI.create("http://localhost:" + port + "/users/3"),
				String.class);
	}

	@Configuration
	@ApplicationPath("/")
	static class ResourceConfiguration {

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig();
		}

		@Bean
		ResourceConfigCustomizer resourceConfigCustomizer() {
			return (config) -> config.register(new TestResource());
		}

		@Path("/users")
		public class TestResource {

			@GET
			@Path("/{id}")
			public String getUser(@PathParam("id") String id) {
				return id;
			}

		}

	}

}
