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

package sample.actuator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Basic integration tests for service demo application.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleActuatorApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class SampleActuatorApplicationTests {

	@Autowired
	private SecurityProperties security;

	@Value("${local.server.port}")
	private int port;

	@Test
	public void testHomeIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port, Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Wrong body: " + body, "Unauthorized", body.get("error"));
		assertFalse("Wrong headers: " + entity.getHeaders(), entity.getHeaders()
				.containsKey("Set-Cookie"));
	}

	@Test
	public void testMetricsIsSecure() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/metrics", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics/", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics/foo", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
		entity = new TestRestTemplate().getForEntity("http://localhost:" + this.port
				+ "/metrics.json", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	public void testHome() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port, Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Hello Phil", body.get("message"));
	}

	@Test
	public void testMetrics() throws Exception {
		testHome(); // makes sure some requests have been made
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/metrics", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertTrue("Wrong body: " + body, body.containsKey("counter.status.200.root"));
	}

	@Test
	public void testEnv() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/env", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertTrue("Wrong body: " + body, body.containsKey("systemProperties"));
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("ok", entity.getBody());
	}

	@Test
	public void testErrorPage() throws Exception {
		ResponseEntity<String> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/foo", String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull(body);
		assertTrue("Wrong body: " + body, body.contains("\"error\":"));
	}

	@Test
	public void testHtmlErrorPage() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		HttpEntity<?> request = new HttpEntity<Void>(headers);
		ResponseEntity<String> entity = new TestRestTemplate("user", getPassword())
				.exchange("http://localhost:" + this.port + "/foo", HttpMethod.GET,
						request, String.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
		String body = entity.getBody();
		assertNotNull("Body was null", body);
		assertTrue("Wrong body: " + body,
				body.contains("This application has no explicit mapping for /error"));
	}

	@Test
	public void testTrace() throws Exception {
		new TestRestTemplate().getForEntity("http://localhost:" + this.port + "/health",
				String.class);
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/trace", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> list = entity.getBody();
		Map<String, Object> trace = list.get(list.size() - 1);
		@SuppressWarnings("unchecked")
		Map<String, Object> map = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) trace
				.get("info")).get("headers")).get("response");
		assertEquals("200", map.get("status"));
	}

	@Test
	public void testErrorPageDirectAccess() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = new TestRestTemplate().getForEntity(
				"http://localhost:" + this.port + "/error", Map.class);
		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("None", body.get("error"));
		assertEquals(999, body.get("status"));
	}

	@Test
	public void testBeans() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<List> entity = new TestRestTemplate("user", getPassword())
				.getForEntity("http://localhost:" + this.port + "/beans", List.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals(1, entity.getBody().size());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) entity.getBody().get(0);
		assertTrue("Wrong body: " + body,
				((String) body.get("context")).startsWith("application"));
	}

	private String getPassword() {
		return this.security.getUser().getPassword();
	}

}
