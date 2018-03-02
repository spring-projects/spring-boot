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

package org.springframework.boot.actuate.management;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.endpoint.web.test.WebEndpointRunners;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HeapDumpWebEndpoint} exposed by Jersey, Spring MVC, and
 * WebFlux.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@RunWith(WebEndpointRunners.class)
public class HeapDumpWebEndpointWebIntegrationTests {

	private static WebTestClient client;

	private static ConfigurableApplicationContext context;

	private TestHeapDumpWebEndpoint endpoint;

	@Before
	public void configureEndpoint() {
		this.endpoint = context.getBean(TestHeapDumpWebEndpoint.class);
		this.endpoint.setAvailable(true);
	}

	@Test
	public void invokeWhenNotAvailableShouldReturnServiceUnavailableStatus() {
		this.endpoint.setAvailable(false);
		client.get().uri("/actuator/heapdump").exchange().expectStatus()
				.isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
	}

	@Test
	public void getRequestShouldReturnHeapDumpInResponseBody() throws Exception {
		client.get().uri("/actuator/heapdump").exchange().expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
				.expectBody(String.class).isEqualTo("HEAPDUMP");
		assertHeapDumpFileIsDeleted();
	}

	private void assertHeapDumpFileIsDeleted() throws InterruptedException {
		long end = System.currentTimeMillis() + 5000;
		while (System.currentTimeMillis() < end && this.endpoint.file.exists()) {
			Thread.sleep(100);
		}
		assertThat(this.endpoint.file.exists()).isFalse();
	}

	@Configuration
	public static class TestConfiguration {

		@Bean
		public HeapDumpWebEndpoint endpoint() {
			return new TestHeapDumpWebEndpoint();
		}

	}

	private static class TestHeapDumpWebEndpoint extends HeapDumpWebEndpoint {

		private boolean available;

		private String heapDump = "HEAPDUMP";

		private File file;

		TestHeapDumpWebEndpoint() {
			super(TimeUnit.SECONDS.toMillis(1));
			reset();
		}

		public void reset() {
			this.available = true;
		}

		@Override
		protected HeapDumper createHeapDumper() {
			return (file, live) -> {
				this.file = file;
				if (!TestHeapDumpWebEndpoint.this.available) {
					throw new HeapDumperUnavailableException("Not available", null);
				}
				if (file.exists()) {
					throw new IOException("File exists");
				}
				FileCopyUtils.copy(TestHeapDumpWebEndpoint.this.heapDump.getBytes(),
						file);
			};
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

	}

}
