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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.EndpointWebMvcAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.ManagementServerPropertiesAutoConfiguration;
import org.springframework.boot.actuate.endpoint.ShutdownEndpoint;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link ShutdownMvcEndpoint}.
 *
 * @author Dave Syer
 *
 */
@SpringBootTest(properties = { "management.security.enabled=false",
		"endpoints.shutdown.enabled=true" })
@RunWith(SpringRunner.class)
public class ShutdownMvcEndpointTests {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context).build();
	}

	@Test
	public void contentTypeDefaultsToActuatorV1Json() throws Exception {
		this.mvc.perform(post("/shutdown")).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						"application/vnd.spring-boot.actuator.v1+json;charset=UTF-8"));
		assertThat(this.context.getBean(CountDownLatch.class).await(30, TimeUnit.SECONDS))
				.isTrue();
	}

	@Test
	public void contentTypeCanBeApplicationJson() throws Exception {
		this.mvc.perform(post("/shutdown").header(HttpHeaders.ACCEPT,
				MediaType.APPLICATION_JSON_VALUE)).andExpect(status().isOk())
				.andExpect(header().string("Content-Type",
						MediaType.APPLICATION_JSON_UTF8_VALUE));
		assertThat(this.context.getBean(CountDownLatch.class).await(30, TimeUnit.SECONDS))
				.isTrue();
	}

	@Configuration
	@Import({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			EndpointWebMvcAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class })
	public static class TestConfiguration {

		@Bean
		public TestShutdownEndpoint endpoint() {
			return new TestShutdownEndpoint(contextCloseLatch());
		}

		@Bean
		public CountDownLatch contextCloseLatch() {
			return new CountDownLatch(1);
		}

	}

	private static class TestShutdownEndpoint extends ShutdownEndpoint {

		private final CountDownLatch contextCloseLatch;

		TestShutdownEndpoint(CountDownLatch contextCloseLatch) {
			this.contextCloseLatch = contextCloseLatch;
		}

		@Override
		public void setApplicationContext(ApplicationContext context)
				throws BeansException {
			ConfigurableApplicationContext mockContext = mock(
					ConfigurableApplicationContext.class);
			willAnswer(new Answer<Void>() {

				@Override
				public Void answer(InvocationOnMock invocation) throws Throwable {
					TestShutdownEndpoint.this.contextCloseLatch.countDown();
					return null;
				}

			}).given(mockContext).close();
			super.setApplicationContext(mockContext);
		}

	}

}
