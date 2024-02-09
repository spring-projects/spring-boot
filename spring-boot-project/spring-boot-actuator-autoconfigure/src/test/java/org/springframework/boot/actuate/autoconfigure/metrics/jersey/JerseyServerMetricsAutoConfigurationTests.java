/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.jersey;

import java.net.URI;
import java.util.Set;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.glassfish.jersey.micrometer.server.DefaultJerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.JerseyTagsProvider;
import org.glassfish.jersey.micrometer.server.MetricsApplicationEventListener;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.jersey.JerseyServerMetricsAutoConfiguration.JerseyTagsProviderAdapter;
import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jersey.JerseyAutoConfiguration;
import org.springframework.boot.autoconfigure.jersey.ResourceConfigCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
 * @author Michael Simons
 */
class JerseyServerMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
		.withConfiguration(AutoConfigurations.of(JerseyServerMetricsAutoConfiguration.class));

	private final WebApplicationContextRunner webContextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
		.withConfiguration(
				AutoConfigurations.of(JerseyAutoConfiguration.class, JerseyServerMetricsAutoConfiguration.class,
						ServletWebServerFactoryAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
						ObservationAutoConfiguration.class, MetricsAutoConfiguration.class))
		.withUserConfiguration(ResourceConfiguration.class)
		.withPropertyValues("server.port:0");

	@Test
	void shouldOnlyBeActiveInWebApplicationContext() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ResourceConfigCustomizer.class));
	}

	@Test
	void shouldProvideAllNecessaryBeans() {
		this.webContextRunner.run((context) -> assertThat(context).hasSingleBean(DefaultJerseyTagsProvider.class)
			.hasSingleBean(ResourceConfigCustomizer.class));
	}

	@Test
	void shouldHonorExistingTagProvider() {
		this.webContextRunner.withUserConfiguration(CustomJerseyTagsProviderConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(CustomJerseyTagsProvider.class));
	}

	@Test
	@Deprecated(since = "3.3.0", forRemoval = true)
	void shouldHonorExistingMicrometerTagProvider() {
		this.webContextRunner.withUserConfiguration(CustomMicrometerJerseyTagsProviderConfiguration.class)
			.run((context) -> {
				assertThat(context).hasSingleBean(CustomMicrometerJerseyTagsProvider.class);
				ResourceConfig config = new ResourceConfig();
				context.getBean(ResourceConfigCustomizer.class).customize(config);
				Set<Object> instances = config.getInstances();
				assertThat(instances).hasSize(1)
					.first(InstanceOfAssertFactories.type(MetricsApplicationEventListener.class))
					.satisfies((listener) -> assertThat(listener).extracting("tagsProvider")
						.isInstanceOf(JerseyTagsProviderAdapter.class));
			});
	}

	@Test
	void httpRequestsAreTimed() {
		this.webContextRunner.run((context) -> {
			doRequest(context);
			MeterRegistry registry = context.getBean(MeterRegistry.class);
			Timer timer = registry.get("http.server.requests").tag("uri", "/users/{id}").timer();
			assertThat(timer.count()).isOne();
		});
	}

	@Test
	void noHttpRequestsTimedWhenJerseyInstrumentationMissingFromClasspath() {
		this.webContextRunner.withClassLoader(new FilteredClassLoader(MetricsApplicationEventListener.class))
			.run((context) -> {
				doRequest(context);

				MeterRegistry registry = context.getBean(MeterRegistry.class);
				assertThat(registry.find("http.server.requests").timer()).isNull();
			});
	}

	private static void doRequest(AssertableWebApplicationContext context) {
		int port = context.getSourceApplicationContext(AnnotationConfigServletWebServerApplicationContext.class)
			.getWebServer()
			.getPort();
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getForEntity(URI.create("http://localhost:" + port + "/users/3"), String.class);
	}

	@Configuration(proxyBeanMethods = false)
	static class ResourceConfiguration {

		@Bean
		ResourceConfig resourceConfig() {
			return new ResourceConfig().register(new TestResource());
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

	@Configuration(proxyBeanMethods = false)
	static class CustomJerseyTagsProviderConfiguration {

		@Bean
		JerseyTagsProvider customJerseyTagsProvider() {
			return new CustomJerseyTagsProvider();
		}

	}

	static class CustomJerseyTagsProvider implements JerseyTagsProvider {

		@Override
		public Iterable<Tag> httpRequestTags(RequestEvent event) {
			return null;
		}

		@Override
		public Iterable<Tag> httpLongRequestTags(RequestEvent event) {
			return null;
		}

	}

	@SuppressWarnings("deprecation")
	@Configuration(proxyBeanMethods = false)
	static class CustomMicrometerJerseyTagsProviderConfiguration {

		@Bean
		io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider customJerseyTagsProvider() {
			return new CustomMicrometerJerseyTagsProvider();
		}

	}

	@SuppressWarnings("deprecation")
	static class CustomMicrometerJerseyTagsProvider
			implements io.micrometer.core.instrument.binder.jersey.server.JerseyTagsProvider {

		@Override
		public Iterable<Tag> httpRequestTags(RequestEvent event) {
			return null;
		}

		@Override
		public Iterable<Tag> httpLongRequestTags(RequestEvent event) {
			return null;
		}

	}

}
