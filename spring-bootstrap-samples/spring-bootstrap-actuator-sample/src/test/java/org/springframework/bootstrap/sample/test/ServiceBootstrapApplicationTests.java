package org.springframework.bootstrap.sample.test;

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
import org.junit.Test;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.sample.service.ServiceBootstrapApplication;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Basic integration tests for service demo application.
 * 
 * @author Dave Syer
 * 
 */
public class ServiceBootstrapApplicationTests {

	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return (ConfigurableApplicationContext) SpringApplication
										.run(ServiceBootstrapApplication.class);
							}
						});
		context = future.get(30, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testHomeIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate().getForEntity(
				"http://localhost:8080", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Wrong body: " + body, "Unauthorized", body.get("error"));
		assertFalse(entity.getHeaders().containsKey("Set-Cookie"));
	}

	@Test
	public void testHome() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate("user", "password").getForEntity(
				"http://localhost:8080", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Hello Phil", body.get("message"));
	}

	@Test
	public void testMetrics() throws Exception {
		testHome(); // makes sure some requests have been made
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate("user", "password").getForEntity(
				"http://localhost:8080/metrics", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertTrue("Wrong body: " + body, body.containsKey("counter.status.200.root"));
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:8080/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("ok", entity.getBody());
	}

	@Test
	public void testErrorPage() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate("user", "password").getForEntity(
				"http://localhost:8080/foo", Map.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals(500, body.get("status"));
	}

	@Test
	public void testErrorPageDirectAccess() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate().getForEntity(
				"http://localhost:8080/error", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("None", body.get("error"));
		assertEquals(999, body.get("status"));
	}

	@Test
	public void testBeans() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = getRestTemplate("user", "password").getForEntity(
				"http://localhost:8080/beans", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, entity.getBody().size());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) entity.getBody().get(0);
		assertTrue("Wrong body: " + body,
				((String) body.get("context")).startsWith("org.springframework"));
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
