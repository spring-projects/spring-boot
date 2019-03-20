/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.fusesource.hawtbuf.ByteArrayInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.AuditAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link HeapdumpMvcEndpoint}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = "management.security.enabled=false")
public class HeapdumpMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Autowired
	private TestHeapdumpMvcEndpoint endpoint;

	@Before
	public void setup() {
		this.context.getBean(HeapdumpMvcEndpoint.class).setEnabled(true);
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@After
	public void reset() {
		this.endpoint.reset();
	}

	@Test
	public void invokeWhenDisabledShouldReturnNotFoundStatus() throws Exception {
		this.endpoint.setEnabled(false);
		this.mvc.perform(get("/heapdump")).andExpect(status().isNotFound());
	}

	@Test
	public void invokeWhenNotAvailableShouldReturnServiceUnavailableStatus()
			throws Exception {
		this.endpoint.setAvailable(false);
		this.mvc.perform(get("/heapdump")).andExpect(status().isServiceUnavailable());
	}

	@Test
	public void invokeWhenLockedShouldReturnTooManyRequestsStatus() throws Exception {
		this.endpoint.setLocked(true);
		this.mvc.perform(get("/heapdump")).andExpect(status().isTooManyRequests());
		assertThat(Thread.interrupted()).isTrue();
	}

	@Test
	public void invokeShouldReturnGzipContent() throws Exception {
		MvcResult result = this.mvc.perform(get("/heapdump")).andExpect(status().isOk())
				.andReturn();
		byte[] bytes = result.getResponse().getContentAsByteArray();
		GZIPInputStream stream = new GZIPInputStream(new ByteArrayInputStream(bytes));
		byte[] uncompressed = FileCopyUtils.copyToByteArray(stream);
		assertThat(uncompressed).isEqualTo("HEAPDUMP".getBytes());
	}

	@Test
	public void invokeOptionsShouldReturnSize() throws Exception {
		this.mvc.perform(options("/heapdump")).andExpect(status().isOk());
	}

	@Import({ JacksonAutoConfiguration.class, AuditAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	@Configuration
	public static class TestConfiguration {

		@Bean
		public HeapdumpMvcEndpoint endpoint() {
			return new TestHeapdumpMvcEndpoint();
		}

	}

	private static class TestHeapdumpMvcEndpoint extends HeapdumpMvcEndpoint {

		private boolean available;

		private boolean locked;

		private String heapDump;

		TestHeapdumpMvcEndpoint() {
			super(TimeUnit.SECONDS.toMillis(1));
			reset();
		}

		public void reset() {
			this.available = true;
			this.locked = false;
			this.heapDump = "HEAPDUMP";
		}

		@Override
		protected HeapDumper createHeapDumper() {
			return new HeapDumper() {

				@Override
				public void dumpHeap(File file, boolean live)
						throws IOException, InterruptedException {
					if (!TestHeapdumpMvcEndpoint.this.available) {
						throw new HeapDumperUnavailableException("Not available", null);
					}
					if (TestHeapdumpMvcEndpoint.this.locked) {
						throw new InterruptedException();
					}
					if (file.exists()) {
						throw new IOException("File exists");
					}
					FileCopyUtils.copy(TestHeapdumpMvcEndpoint.this.heapDump.getBytes(),
							file);
				}

			};
		}

		public void setAvailable(boolean available) {
			this.available = available;
		}

		public void setLocked(boolean locked) {
			this.locked = locked;
		}

	}

}
