package org.springframework.zero.sample.actuator.ui;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.zero.SpringApplication;
import org.springframework.zero.sample.actuator.ui.SampleActuatorUiApplication;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for separate management and main service ports.
 * 
 * @author Dave Syer
 * 
 */
public class SampleActuatorUiApplicationPortTests {

	private static ConfigurableApplicationContext context;

	private static int port = 9000;
	private static int managementPort = 9001;

	@BeforeClass
	public static void start() throws Exception {
		final String[] args = new String[] { "--server.port=" + port,
				"--management.port=" + managementPort, "--management.address=127.0.0.1" };
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return (ConfigurableApplicationContext) SpringApplication
										.run(SampleActuatorUiApplication.class, args);
							}
						});
		context = future.get(10, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void stop() {
		if (context != null) {
			context.close();
		}
	}

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:" + port, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	@Ignore
	public void testMetrics() throws Exception {
		// FIXME broken since error page is not rendered
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = getRestTemplate().getForEntity(
				"http://localhost:" + managementPort + "/metrics", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:" + managementPort + "/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("ok", entity.getBody());
	}

	private RestTemplate getRestTemplate() {

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
			@Override
			public void handleError(ClientHttpResponse response) throws IOException {
			}
		});
		return restTemplate;

	}

}
