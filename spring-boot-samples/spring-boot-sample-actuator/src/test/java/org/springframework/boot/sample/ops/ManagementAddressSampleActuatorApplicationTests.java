/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.sample.ops;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for separate management and main service ports.
 * 
 * @author Dave Syer
 */
public class ManagementAddressSampleActuatorApplicationTests {

	private static ConfigurableApplicationContext context;

	private static int port = 9010;
	private static int managementPort = 9011;

	@BeforeClass
	public static void start() throws Exception {
		final String[] args = new String[] { "--server.port=" + port,
				"--management.port=" + managementPort };
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return SpringApplication.run(
										SampleActuatorApplication.class, args);
							}
						});
		context = future.get(60, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testHome() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate("user", getPassword()).getForEntity(
				"http://localhost:" + port, Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Hello Phil", body.get("message"));
	}

	@Test
	@Ignore
	public void testMetrics() throws Exception {
		// FIXME broken because error page is no longer exposed on management port
		testHome(); // makes sure some requests have been made
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate().getForEntity(
				"http://localhost:" + managementPort + "/metrics", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	@Ignore
	public void testHealth() throws Exception {
		// FIXME broken because error page is no longer exposed on management port
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:" + managementPort + "/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("ok", entity.getBody());
	}

	@Test
	@Ignore
	public void testErrorPage() throws Exception {
		// FIXME broken because error page is no longer exposed on management port
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate().getForEntity(
				"http://localhost:" + managementPort + "/error", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals(999, body.get("status"));
	}

	private String getPassword() {
		return context.getBean(SecurityProperties.class).getUser().getPassword();
	}

	private RestTemplate getRestTemplate() {
		return getRestTemplate(null, null);
	}

	private RestTemplate getRestTemplate(final String username, final String password) {

		List<ClientHttpRequestInterceptor> interceptors = new ArrayList<ClientHttpRequestInterceptor>();

		if (username != null) {

			interceptors.add(new ClientHttpRequestInterceptor() {

				@Override
				public ClientHttpResponse intercept(HttpRequest request, byte[] body,
						ClientHttpRequestExecution execution) throws IOException {
					request.getHeaders().add(
							"Authorization",
							"Basic "
									+ new String(Base64
											.encode((username + ":" + password)
													.getBytes())));
					return execution.execute(request, body);
				}
			});
		}

		RestTemplate restTemplate = new RestTemplate(
				new InterceptingClientHttpRequestFactory(
						new SimpleClientHttpRequestFactory(), interceptors));
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

}
