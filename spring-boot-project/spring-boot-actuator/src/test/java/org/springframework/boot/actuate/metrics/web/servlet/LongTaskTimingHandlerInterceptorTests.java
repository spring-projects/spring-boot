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

package org.springframework.boot.actuate.metrics.web.servlet;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link LongTaskTimingHandlerInterceptor}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
class LongTaskTimingHandlerInterceptorTests {

	@Autowired
	private SimpleMeterRegistry registry;

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private CyclicBarrier callableBarrier;

	private MockMvc mvc;

	@BeforeEach
	public void setUpMockMvc() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	void asyncRequestThatThrowsUncheckedException() throws Exception {
		MvcResult result = this.mvc.perform(get("/api/c1/completableFutureException"))
				.andExpect(request().asyncStarted()).andReturn();
		assertThat(this.registry.get("my.long.request.exception").longTaskTimer().activeTasks()).isEqualTo(1);
		assertThatExceptionOfType(NestedServletException.class)
				.isThrownBy(() -> this.mvc.perform(asyncDispatch(result)))
				.withRootCauseInstanceOf(RuntimeException.class);
		assertThat(this.registry.get("my.long.request.exception").longTaskTimer().activeTasks()).isEqualTo(0);
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
		this.callableBarrier.await();
		assertThat(this.registry.get("my.long.request").tags("region", "test").longTaskTimer().activeTasks())
				.isEqualTo(1);
		this.callableBarrier.await();
		backgroundRequest.join();
		this.mvc.perform(asyncDispatch(result.get())).andExpect(status().isOk());
		assertThat(this.registry.get("my.long.request").tags("region", "test").longTaskTimer().activeTasks())
				.isEqualTo(0);
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	@Import(Controller1.class)
	static class MetricsInterceptorConfiguration {

		@Bean
		Clock micrometerClock() {
			return new MockClock();
		}

		@Bean
		SimpleMeterRegistry simple(Clock clock) {
			return new SimpleMeterRegistry(SimpleConfig.DEFAULT, clock);
		}

		@Bean
		CyclicBarrier callableBarrier() {
			return new CyclicBarrier(2);
		}

		@Bean
		WebMvcConfigurer handlerInterceptorConfigurer(MeterRegistry meterRegistry) {
			return new WebMvcConfigurer() {

				@Override
				public void addInterceptors(InterceptorRegistry registry) {
					registry.addInterceptor(
							new LongTaskTimingHandlerInterceptor(meterRegistry, new DefaultWebMvcTagsProvider()));
				}

			};
		}

	}

	@RestController
	@RequestMapping("/api/c1")
	static class Controller1 {

		@Autowired
		private CyclicBarrier callableBarrier;

		@Timed
		@Timed(value = "my.long.request", extraTags = { "region", "test" }, longTask = true)
		@GetMapping("/callable/{id}")
		public Callable<String> asyncCallable(@PathVariable Long id) throws Exception {
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
		@Timed(value = "my.long.request.exception", longTask = true)
		@GetMapping("/completableFutureException")
		CompletableFuture<String> asyncCompletableFutureException() {
			return CompletableFuture.supplyAsync(() -> {
				throw new RuntimeException("boom");
			});
		}

	}

}
