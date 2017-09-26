/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.servlet;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MetricsHandlerInterceptor}.
 *
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class MetricsHandlerInterceptorTests {

	private static final CountDownLatch longRequestCountDown = new CountDownLatch(1);

	@Autowired
	private MeterRegistry registry;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void timedMethod() throws Exception {
		this.mvc.perform(get("/api/c1/10")).andExpect(status().isOk());
		assertThat(this.registry.find("http.server.requests")
				.tags("status", "200", "uri", "/api/c1/{id}", "public", "true")
				.value(Statistic.Count, 1.0).timer()).isPresent();
	}

	@Test
	public void untimedMethod() throws Exception {
		this.mvc.perform(get("/api/c1/untimed/10")).andExpect(status().isOk());
		assertThat(this.registry.find("http.server.requests")
				.tags("uri", "/api/c1/untimed/10").timer()).isEmpty();
	}

	@Test
	public void timedControllerClass() throws Exception {
		this.mvc.perform(get("/api/c2/10")).andExpect(status().isOk());
		assertThat(
				this.registry.find("http.server.requests").tags("status", "200").timer())
						.hasValueSatisfying((t) -> assertThat(t.count()).isEqualTo(1));
	}

	@Test
	public void badClientRequest() throws Exception {
		this.mvc.perform(get("/api/c1/oops")).andExpect(status().is4xxClientError());
		assertThat(
				this.registry.find("http.server.requests").tags("status", "400").timer())
						.hasValueSatisfying((t) -> assertThat(t.count()).isEqualTo(1));
	}

	@Test
	public void unhandledError() throws Exception {
		assertThatCode(() -> this.mvc.perform(get("/api/c1/unhandledError/10"))
				.andExpect(status().isOk())).hasCauseInstanceOf(RuntimeException.class);
		assertThat(this.registry.find("http.server.requests")
				.tags("exception", "RuntimeException").value(Statistic.Count, 1.0)
				.timer()).isPresent();
	}

	@Test
	public void longRunningRequest() throws Exception {
		MvcResult result = this.mvc.perform(get("/api/c1/long/10"))
				.andExpect(request().asyncStarted()).andReturn();

		// while the mapping is running, it contributes to the activeTasks count
		assertThat(this.registry.find("my.long.request").tags("region", "test")
				.value(Statistic.Count, 1.0).longTaskTimer()).isPresent();

		// once the mapping completes, we can gather information about status, etc.
		longRequestCountDown.countDown();

		this.mvc.perform(asyncDispatch(result)).andExpect(status().isOk());

		assertThat(this.registry.find("http.server.requests").tags("status", "200")
				.value(Statistic.Count, 1.0).timer()).isPresent();
	}

	@Test
	public void endpointThrowsError() throws Exception {
		this.mvc.perform(get("/api/c1/error/10")).andExpect(status().is4xxClientError());
		assertThat(this.registry.find("http.server.requests").tags("status", "422")
				.value(Statistic.Count, 1.0).timer()).isPresent();
	}

	@Test
	public void regexBasedRequestMapping() throws Exception {
		this.mvc.perform(get("/api/c1/regex/.abc")).andExpect(status().isOk());
		assertThat(this.registry.find("http.server.requests")
				.tags("uri", "/api/c1/regex/{id:\\.[a-z]+}").value(Statistic.Count, 1.0)
				.timer()).isPresent();
	}

	@Test
	public void recordQuantiles() throws Exception {
		this.mvc.perform(get("/api/c1/quantiles/10")).andExpect(status().isOk());

		assertThat(this.registry.find("http.server.requests").tags("quantile", "0.5")
				.gauge()).isNotEmpty();
		assertThat(this.registry.find("http.server.requests").tags("quantile", "0.95")
				.gauge()).isNotEmpty();
	}

	@Test
	public void recordPercentiles() throws Exception {
		this.mvc.perform(get("/api/c1/percentiles/10")).andExpect(status().isOk());

		assertThat(this.registry.find("http.server.requests").meters()
				.stream().flatMap((m) -> StreamSupport
						.stream(m.getId().getTags().spliterator(), false))
				.map(Tag::getKey)).contains("bucket");
	}

	@Configuration
	@EnableWebMvc
	@Import({ Controller1.class, Controller2.class })
	static class TestConfiguration {

		@Bean
		MeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

		@Bean
		WebMvcMetrics webMvcMetrics(MeterRegistry meterRegistry) {
			return new WebMvcMetrics(meterRegistry, new DefaultWebMvcTagsProvider(),
					"http.server.requests", true, true);
		}

		@Configuration
		static class HandlerInterceptorConfiguration implements WebMvcConfigurer {

			private final WebMvcMetrics webMvcMetrics;

			HandlerInterceptorConfiguration(WebMvcMetrics webMvcMetrics) {
				this.webMvcMetrics = webMvcMetrics;
			}

			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(
						new MetricsHandlerInterceptor(this.webMvcMetrics));
			}

		}

	}

	@RestController
	@RequestMapping("/api/c1")
	static class Controller1 {

		@Timed(extraTags = { "public", "true" })
		@GetMapping("/{id}")
		public String successfulWithExtraTags(@PathVariable Long id) {
			return id.toString();
		}

		@Timed // contains dimensions for status, etc. that can't be known until after the
				// response is sent
		@Timed(value = "my.long.request", extraTags = { "region",
				"test" }, longTask = true) // in progress metric
		@GetMapping("/long/{id}")
		public Callable<String> takesLongTimeToSatisfy(@PathVariable Long id) {
			return () -> {
				try {
					longRequestCountDown.await();
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				return id.toString();
			};
		}

		@GetMapping("/untimed/{id}")
		public String successfulButUntimed(@PathVariable Long id) {
			return id.toString();
		}

		@Timed
		@GetMapping("/error/{id}")
		public String alwaysThrowsException(@PathVariable Long id) {
			throw new IllegalStateException("Boom on $id!");
		}

		@Timed
		@GetMapping("/unhandledError/{id}")
		public String alwaysThrowsUnhandledException(@PathVariable Long id) {
			throw new RuntimeException("Boom on $id!");
		}

		@Timed
		@GetMapping("/regex/{id:\\.[a-z]+}")
		public String successfulRegex(@PathVariable String id) {
			return id;
		}

		@Timed(quantiles = { 0.5, 0.95 })
		@GetMapping("/quantiles/{id}")
		public String quantiles(@PathVariable String id) {
			return id;
		}

		@Timed(percentiles = true)
		@GetMapping("/percentiles/{id}")
		public String percentiles(@PathVariable String id) {
			return id;
		}

		@ExceptionHandler(IllegalStateException.class)
		@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
		ModelAndView defaultErrorHandler(HttpServletRequest request, Exception e) {
			return new ModelAndView("myerror");
		}

	}

	@RestController
	@Timed
	@RequestMapping("/api/c2")
	static class Controller2 {

		@GetMapping("/{id}")
		public String successful(@PathVariable Long id) {
			return id.toString();
		}

	}

}
