/*
 * Copyright 2012-2016 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link HeapdumpMvcEndpoint} OPTIONS call with security.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HeapdumpMvcEndpointSecureOptionsTests {

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
	public void invokeOptionsShouldReturnSize() throws Exception {
		this.mvc.perform(options("/heapdump")).andExpect(status().isOk());
	}

	@Import({ JacksonAutoConfiguration.class,
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

	}

}
