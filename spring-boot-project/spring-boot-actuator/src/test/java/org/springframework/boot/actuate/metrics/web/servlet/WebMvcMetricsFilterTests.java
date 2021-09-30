/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.servlet;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.core.lang.NonNull;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.metrics.AutoTimer;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link WebMvcMetricsFilter}.
 *
 * @author Jon Schneider
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
class WebMvcMetricsFilterTests {

	@Autowired
	private SimpleMeterRegistry registry;

	@Autowired
	private PrometheusMeterRegistry prometheusRegistry;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private WebMvcMetricsFilter filter;

	private MockMvc mvc;

	@Autowired
	@Qualifier("callableBarrier")
	private CyclicBarrier callableBarrier;

	@Autowired
	@Qualifier("completableFutureBarrier")
	private CyclicBarrier completableFutureBarrier;

	@Autowired
	private FaultyWebMvcTagsProvider tagsProvider;

	@BeforeEach
	void setupMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).addFilters(this.filter, new CustomBehaviorFilter())
				.build();
	}

	@Test
	void timedMethod() throws Exception {
		this.mvc.perform(get("/api/c1/10")).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests")
				.tags("status", "200", "uri", "/api/c1/{id}", "public", "true").timer().count()).isEqualTo(1);
	}

	@Test
	void subclassedTimedMethod() throws Exception {
		this.mvc.perform(get("/api/c1/metaTimed/10")).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("status", "200", "uri", "/api/c1/metaTimed/{id}")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void untimedMethod() throws Exception {
		this.mvc.perform(get("/api/c1/untimed/10")).andExpect(status().isOk());
		assertThat(this.registry.find("http.server.requests").tags("uri", "/api/c1/untimed/10").timer()).isNull();
	}

	@Test
	void timedControllerClass() throws Exception {
		this.mvc.perform(get("/api/c2/10")).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("status", "200").timer().count()).isEqualTo(1L);
	}

	@Test
	void badClientRequest() throws Exception {
		this.mvc.perform(get("/api/c1/oops")).andExpect(status().is4xxClientError());
		assertThat(this.registry.get("http.server.requests").tags("status", "400").timer().count()).isEqualTo(1L);
	}

	@Test
	void redirectRequest() throws Exception {
		this.mvc.perform(get("/api/redirect").header(CustomBehaviorFilter.TEST_STATUS_HEADER, "302"))
				.andExpect(status().is3xxRedirection());
		assertThat(this.registry.get("http.server.requests").tags("uri", "REDIRECTION").tags("status", "302").timer())
				.isNotNull();
	}

	@Test
	void notFoundRequest() throws Exception {
		this.mvc.perform(get("/api/not/found").header(CustomBehaviorFilter.TEST_STATUS_HEADER, "404"))
				.andExpect(status().is4xxClientError());
		assertThat(this.registry.get("http.server.requests").tags("uri", "NOT_FOUND").tags("status", "404").timer())
				.isNotNull();
	}

	@Test
	void unhandledError() {
		assertThatCode(() -> this.mvc.perform(get("/api/c1/unhandledError/10")).andExpect(status().isOk()))
				.hasRootCauseInstanceOf(RuntimeException.class);
		assertThat(this.registry.get("http.server.requests").tags("exception", "RuntimeException").timer().count())
				.isEqualTo(1L);
	}

	@Test
	void unhandledServletException() {
		assertThatCode(() -> this.mvc
				.perform(get("/api/filterError").header(CustomBehaviorFilter.TEST_SERVLET_EXCEPTION_HEADER, "throw"))
				.andExpect(status().isOk())).isInstanceOf(ServletException.class);
		Id meterId = this.registry.get("http.server.requests").tags("exception", "ServletException").timer().getId();
		assertThat(meterId.getTag("status")).isEqualTo("500");
	}

	@Test
	void streamingError() throws Exception {
		MvcResult result = this.mvc.perform(get("/api/c1/streamingError")).andExpect(request().asyncStarted())
				.andReturn();
		assertThatIOException().isThrownBy(() -> this.mvc.perform(asyncDispatch(result)).andReturn());
		Id meterId = this.registry.get("http.server.requests").tags("exception", "IOException").timer().getId();
		// Response is committed before error occurs so status is 200 (OK)
		assertThat(meterId.getTag("status")).isEqualTo("200");
	}

	@Test
	void whenMetricsRecordingFailsResponseIsUnaffected() throws Exception {
		this.tagsProvider.failOnce();
		this.mvc.perform(get("/api/c1/10")).andExpect(status().isOk());
	}

	@Test
	void anonymousError() {
		try {
			this.mvc.perform(get("/api/c1/anonymousError/10"));
		}
		catch (Throwable ignore) {
		}
		Id meterId = this.registry.get("http.server.requests").tag("uri", "/api/c1/anonymousError/{id}").timer()
				.getId();
		assertThat(meterId.getTag("exception")).endsWith("$1");
		assertThat(meterId.getTag("status")).isEqualTo("500");
	}

	@Test
	void asyncCallableRequest() throws Exception {
		AtomicReference<MvcResult> result = new AtomicReference<>();
		Thread backgroundRequest = new Thread(() -> {
			try {
				result.set(
						this.mvc.perform(get("/api/c1/callable/10")).andExpect(request().asyncStarted()).andReturn());
			}
			catch (Exception ex) {
				fail("Failed to execute async request", ex);
			}
		});
		backgroundRequest.start();
		assertThat(this.registry.find("http.server.requests").tags("uri", "/api/c1/async").timer())
				.describedAs("Request isn't prematurely recorded as complete").isNull();
		// once the mapping completes, we can gather information about status, etc.
		this.callableBarrier.await();
		MockClock.clock(this.registry).add(Duration.ofSeconds(2));
		this.callableBarrier.await();
		backgroundRequest.join();
		this.mvc.perform(asyncDispatch(result.get())).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("status", "200").tags("uri", "/api/c1/callable/{id}")
				.timer().totalTime(TimeUnit.SECONDS)).isEqualTo(2L);
	}

	@Test
	void asyncRequestThatThrowsUncheckedException() throws Exception {
		MvcResult result = this.mvc.perform(get("/api/c1/completableFutureException"))
				.andExpect(request().asyncStarted()).andReturn();
		assertThatExceptionOfType(NestedServletException.class)
				.isThrownBy(() -> this.mvc.perform(asyncDispatch(result)))
				.withRootCauseInstanceOf(RuntimeException.class);
		assertThat(this.registry.get("http.server.requests").tags("uri", "/api/c1/completableFutureException").timer()
				.count()).isEqualTo(1);
	}

	@Test
	void asyncCompletableFutureRequest() throws Exception {
		AtomicReference<MvcResult> result = new AtomicReference<>();
		Thread backgroundRequest = new Thread(() -> {
			try {
				result.set(this.mvc.perform(get("/api/c1/completableFuture/{id}", 1))
						.andExpect(request().asyncStarted()).andReturn());
			}
			catch (Exception ex) {
				fail("Failed to execute async request", ex);
			}
		});
		backgroundRequest.start();
		this.completableFutureBarrier.await();
		MockClock.clock(this.registry).add(Duration.ofSeconds(2));
		this.completableFutureBarrier.await();
		backgroundRequest.join();
		this.mvc.perform(asyncDispatch(result.get())).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("uri", "/api/c1/completableFuture/{id}").timer()
				.totalTime(TimeUnit.SECONDS)).isEqualTo(2);
	}

	@Test
	void endpointThrowsError() throws Exception {
		this.mvc.perform(get("/api/c1/error/10")).andExpect(status().is4xxClientError());
		assertThat(this.registry.get("http.server.requests").tags("status", "422", "exception", "IllegalStateException")
				.timer().count()).isEqualTo(1L);
	}

	@Test
	void regexBasedRequestMapping() throws Exception {
		this.mvc.perform(get("/api/c1/regex/.abc")).andExpect(status().isOk());
		assertThat(
				this.registry.get("http.server.requests").tags("uri", "/api/c1/regex/{id:\\.[a-z]+}").timer().count())
						.isEqualTo(1L);
	}

	@Test
	void recordQuantiles() throws Exception {
		this.mvc.perform(get("/api/c1/percentiles/10")).andExpect(status().isOk());
		assertThat(this.prometheusRegistry.scrape()).contains("quantile=\"0.5\"");
		assertThat(this.prometheusRegistry.scrape()).contains("quantile=\"0.95\"");
	}

	@Test
	void recordHistogram() throws Exception {
		this.mvc.perform(get("/api/c1/histogram/10")).andExpect(status().isOk());
		assertThat(this.prometheusRegistry.scrape()).contains("le=\"0.001\"");
		assertThat(this.prometheusRegistry.scrape()).contains("le=\"30.0\"");
	}

	@Test
	void trailingSlashShouldNotRecordDuplicateMetrics() throws Exception {
		this.mvc.perform(get("/api/c1/simple/10")).andExpect(status().isOk());
		this.mvc.perform(get("/api/c1/simple/10/")).andExpect(status().isOk());
		assertThat(this.registry.get("http.server.requests").tags("status", "200", "uri", "/api/c1/simple/{id}").timer()
				.count()).isEqualTo(2);
	}

	@Target({ ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Timed(percentiles = 0.95)
	@interface Timed95 {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@Import({ Controller1.class, Controller2.class })
	static class MetricsFilterApp {

		@Bean
		Clock micrometerClock() {
			return new MockClock();
		}

		@Primary
		@Bean
		MeterRegistry meterRegistry(Collection<MeterRegistry> registries, Clock clock) {
			CompositeMeterRegistry composite = new CompositeMeterRegistry(clock);
			registries.forEach(composite::add);
			return composite;
		}

		@Bean
		SimpleMeterRegistry simple(Clock clock) {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		}

		@Bean
		PrometheusMeterRegistry prometheus(Clock clock) {
			PrometheusMeterRegistry r = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT, new CollectorRegistry(),
					clock);
			r.config().meterFilter(new MeterFilter() {
				@Override
				@NonNull
				public MeterFilterReply accept(@NonNull Meter.Id id) {
					for (Tag tag : id.getTags()) {
						if (tag.getKey().equals("uri")
								&& (tag.getValue().contains("histogram") || tag.getValue().contains("percentiles"))) {
							return MeterFilterReply.ACCEPT;
						}
					}
					return MeterFilterReply.DENY;
				}
			});
			return r;
		}

		@Bean
		CustomBehaviorFilter redirectAndNotFoundFilter() {
			return new CustomBehaviorFilter();
		}

		@Bean(name = "callableBarrier")
		CyclicBarrier callableBarrier() {
			return new CyclicBarrier(2);
		}

		@Bean(name = "completableFutureBarrier")
		CyclicBarrier completableFutureBarrier() {
			return new CyclicBarrier(2);
		}

		@Bean
		WebMvcMetricsFilter webMetricsFilter(MeterRegistry registry, FaultyWebMvcTagsProvider tagsProvider,
				WebApplicationContext ctx) {
			return new WebMvcMetricsFilter(registry, tagsProvider, "http.server.requests", AutoTimer.ENABLED);
		}

		@Bean
		FaultyWebMvcTagsProvider faultyWebMvcTagsProvider() {
			return new FaultyWebMvcTagsProvider();
		}

	}

	@RestController
	@RequestMapping("/api/c1")
	static class Controller1 {

		@Autowired
		@Qualifier("callableBarrier")
		private CyclicBarrier callableBarrier;

		@Autowired
		@Qualifier("completableFutureBarrier")
		private CyclicBarrier completableFutureBarrier;

		@Timed(extraTags = { "public", "true" })
		@GetMapping("/{id}")
		String successfulWithExtraTags(@PathVariable Long id) {
			return id.toString();
		}

		@GetMapping("/simple/{id}")
		String simpleMapping(@PathVariable Long id) {
			return id.toString();
		}

		@Timed
		@Timed(value = "my.long.request", extraTags = { "region", "test" }, longTask = true)
		@GetMapping("/callable/{id}")
		Callable<String> asyncCallable(@PathVariable Long id) throws Exception {
			this.callableBarrier.await();
			return () -> {
				try {
					this.callableBarrier.await();
				}
				catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				}
				return id.toString();
			};
		}

		@Timed
		@GetMapping("/completableFuture/{id}")
		CompletableFuture<String> asyncCompletableFuture(@PathVariable Long id) throws Exception {
			this.completableFutureBarrier.await();
			return CompletableFuture.supplyAsync(() -> {
				try {
					this.completableFutureBarrier.await();
				}
				catch (InterruptedException | BrokenBarrierException ex) {
					throw new RuntimeException(ex);
				}
				return id.toString();
			});
		}

		@Timed
		@Timed(value = "my.long.request.exception", longTask = true)
		@GetMapping("/completableFutureException")
		CompletableFuture<String> asyncCompletableFutureException() {
			return CompletableFuture.supplyAsync(() -> {
				throw new RuntimeException("boom");
			});
		}

		@GetMapping("/untimed/{id}")
		String successfulButUntimed(@PathVariable Long id) {
			return id.toString();
		}

		@Timed
		@GetMapping("/error/{id}")
		String alwaysThrowsException(@PathVariable Long id) {
			throw new IllegalStateException("Boom on " + id + "!");
		}

		@Timed
		@GetMapping("/anonymousError/{id}")
		String alwaysThrowsAnonymousException(@PathVariable Long id) throws Exception {
			throw new Exception("this exception won't have a simple class name") {
			};
		}

		@Timed
		@GetMapping("/unhandledError/{id}")
		String alwaysThrowsUnhandledException(@PathVariable Long id) {
			throw new RuntimeException("Boom on " + id + "!");
		}

		@GetMapping("/streamingError")
		ResponseBodyEmitter streamingError() throws IOException {
			ResponseBodyEmitter emitter = new ResponseBodyEmitter();
			emitter.send("some data");
			emitter.send("some more data");
			emitter.completeWithError(new IOException("error while writing to the response"));
			return emitter;
		}

		@Timed
		@GetMapping("/regex/{id:\\.[a-z]+}")
		String successfulRegex(@PathVariable String id) {
			return id;
		}

		@Timed(percentiles = { 0.50, 0.95 })
		@GetMapping("/percentiles/{id}")
		String percentiles(@PathVariable String id) {
			return id;
		}

		@Timed(histogram = true)
		@GetMapping("/histogram/{id}")
		String histogram(@PathVariable String id) {
			return id;
		}

		@Timed95
		@GetMapping("/metaTimed/{id}")
		String meta(@PathVariable String id) {
			return id;
		}

		@ExceptionHandler(IllegalStateException.class)
		@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
		ModelAndView defaultErrorHandler(HttpServletRequest request, Exception e) {
			// this is done by ErrorAttributes implementations
			request.setAttribute(ErrorAttributes.ERROR_ATTRIBUTE, e);
			return new ModelAndView("myerror");
		}

	}

	@RestController
	@Timed
	@RequestMapping("/api/c2")
	static class Controller2 {

		@GetMapping("/{id}")
		String successful(@PathVariable Long id) {
			return id.toString();
		}

	}

	static class CustomBehaviorFilter extends OncePerRequestFilter {

		static final String TEST_STATUS_HEADER = "x-test-status";

		static final String TEST_SERVLET_EXCEPTION_HEADER = "x-test-servlet-exception";

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
				FilterChain filterChain) throws ServletException, IOException {
			String misbehaveStatus = request.getHeader(TEST_STATUS_HEADER);
			if (misbehaveStatus != null) {
				response.setStatus(Integer.parseInt(misbehaveStatus));
				return;
			}
			if (request.getHeader(TEST_SERVLET_EXCEPTION_HEADER) != null) {
				throw new ServletException();
			}
			filterChain.doFilter(request, response);
		}

	}

}
