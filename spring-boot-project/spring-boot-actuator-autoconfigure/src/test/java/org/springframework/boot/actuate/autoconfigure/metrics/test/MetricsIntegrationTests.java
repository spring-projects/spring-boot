/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.test;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.amqp.RabbitMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.cache.CacheMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.orm.jpa.HibernateMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.client.HttpClientMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.reactive.WebFluxMetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.web.servlet.WebMvcMetricsAutoConfiguration;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcMetricsFilter;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Integration tests for Metrics.
 *
 * @author Jon Schneider
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = MetricsIntegrationTests.MetricsApp.class,
		properties = "management.metrics.use-global-registry=false")
class MetricsIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RestTemplate external;

	@Autowired
	private TestRestTemplate loopback;

	@Autowired
	private MeterRegistry registry;

	@SuppressWarnings("unchecked")
	@Test
	void restTemplateIsInstrumented() {
		MockRestServiceServer server = MockRestServiceServer.bindTo(this.external).build();
		server.expect(once(), requestTo("/api/external")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("{\"message\": \"hello\"}", MediaType.APPLICATION_JSON));
		assertThat(this.external.getForObject("/api/external", Map.class)).containsKey("message");
		assertThat(this.registry.get("http.client.requests").timer().count()).isEqualTo(1);
	}

	@Test
	void requestMappingIsInstrumented() {
		this.loopback.getForObject("/api/people", Set.class);
		waitAtMost(Duration.ofSeconds(5)).untilAsserted(
				() -> assertThat(this.registry.get("http.server.requests").timer().count()).isEqualTo(1));

	}

	@Test
	void automaticallyRegisteredBinders() {
		assertThat(this.context.getBeansOfType(MeterBinder.class).values())
				.hasAtLeastOneElementOfType(LogbackMetrics.class).hasAtLeastOneElementOfType(JvmMemoryMetrics.class);
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void metricsFilterRegisteredForAsyncDispatches() {
		Map<String, FilterRegistrationBean> filterRegistrations = this.context
				.getBeansOfType(FilterRegistrationBean.class);
		assertThat(filterRegistrations).containsKey("webMvcMetricsFilter");
		FilterRegistrationBean registration = filterRegistrations.get("webMvcMetricsFilter");
		assertThat(registration.getFilter()).isInstanceOf(WebMvcMetricsFilter.class);
		assertThat((Set<DispatcherType>) ReflectionTestUtils.getField(registration, "dispatcherTypes"))
				.containsExactlyInAnyOrder(DispatcherType.REQUEST, DispatcherType.ASYNC);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration({ MetricsAutoConfiguration.class, JvmMetricsAutoConfiguration.class,
			LogbackMetricsAutoConfiguration.class, SystemMetricsAutoConfiguration.class,
			RabbitMetricsAutoConfiguration.class, CacheMetricsAutoConfiguration.class,
			DataSourcePoolMetricsAutoConfiguration.class, HibernateMetricsAutoConfiguration.class,
			HttpClientMetricsAutoConfiguration.class, WebFluxMetricsAutoConfiguration.class,
			WebMvcMetricsAutoConfiguration.class, JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, RestTemplateAutoConfiguration.class,
			WebMvcAutoConfiguration.class, DispatcherServletAutoConfiguration.class,
			ServletWebServerFactoryAutoConfiguration.class })
	@Import(PersonController.class)
	static class MetricsApp {

		@Primary
		@Bean
		MeterRegistry registry() {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock());
		}

		@Bean
		RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
			return restTemplateBuilder.build();
		}

		@Bean
		CyclicBarrier cyclicBarrier() {
			return new CyclicBarrier(2);
		}

	}

	@RestController
	static class PersonController {

		@GetMapping("/api/people")
		Set<String> personName() {
			return Collections.singleton("Jon");
		}

	}

}
