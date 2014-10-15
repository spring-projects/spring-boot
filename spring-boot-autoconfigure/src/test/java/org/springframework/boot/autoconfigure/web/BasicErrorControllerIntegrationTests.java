/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.net.URI;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerIntegrationTests.TestConfiguration;
import org.springframework.boot.autoconfigure.web.BasicErrorControllerMockMvcTests.MinimalWebConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractView;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link BasicErrorController} using {@link IntegrationTest} that hit a real
 * HTTP server.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
@WebAppConfiguration
@DirtiesContext
@IntegrationTest("server.port=0")
public class BasicErrorControllerIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	@SuppressWarnings("rawtypes")
	public void testErrorForMachineClient() throws Exception {
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertThat(entity.getBody().toString(), endsWith("status=500, "
				+ "error=Internal Server Error, "
				+ "exception=java.lang.IllegalStateException, " + "message=Expected!, "
				+ "path=/}"));
	}

	@Test
	@SuppressWarnings("rawtypes")
	public void testBindingExceptionForMachineClient() throws Exception {
		RequestEntity request = RequestEntity
				.get(URI.create("http://localhost:" + this.port + "/bind"))
				.accept(MediaType.APPLICATION_JSON).build();
		ResponseEntity<Map> entity = new TestRestTemplate().exchange(request, Map.class);
		String resp = entity.getBody().toString();
		assertThat(resp, containsString("Error count: 1"));
		assertThat(resp, containsString("errors=[{"));
		assertThat(resp, containsString("codes=["));
		assertThat(resp, containsString("org.springframework.validation.BindException"));
	}

	@Configuration
	@MinimalWebConfiguration
	public static class TestConfiguration {

		// For manual testing
		public static void main(String[] args) {
			SpringApplication.run(TestConfiguration.class, args);
		}

		@Bean
		public View error() {
			return new AbstractView() {
				@Override
				protected void renderMergedOutputModel(Map<String, Object> model,
						HttpServletRequest request, HttpServletResponse response)
						throws Exception {
					response.getWriter().write("ERROR_BEAN");
				}
			};
		}

		@RestController
		protected static class Errors {

			public String getFoo() {
				return "foo";
			}

			@RequestMapping("/")
			public String home() {
				throw new IllegalStateException("Expected!");
			}

			@RequestMapping("/bind")
			public String bind(HttpServletRequest request) throws Exception {
				BindException error = new BindException(this, "test");
				error.rejectValue("foo", "bar.error");
				throw error;
			}

		}

	}

}
