package org.springframework.boot.sample.jsp;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;

/**
 * Basic integration tests for JSP application.
 * 
 * @author Phillip Webb
 */
public class SampleWebJspApplicationTests {

	private static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {
		Future<ConfigurableApplicationContext> future = Executors
				.newSingleThreadExecutor().submit(
						new Callable<ConfigurableApplicationContext>() {
							@Override
							public ConfigurableApplicationContext call() throws Exception {
								return SpringApplication
										.run(SampleWebJspApplication.class);
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
	public void testJspWithEl() throws Exception {
		ResponseEntity<String> entity = getRestTemplate().getForEntity(
				"http://localhost:8080", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body:\n" + entity.getBody(),
				entity.getBody().contains("/resources/text.txt"));
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
