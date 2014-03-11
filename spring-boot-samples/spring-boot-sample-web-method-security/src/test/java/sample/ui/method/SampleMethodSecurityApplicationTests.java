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

package sample.ui.method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.RestTemplates;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Basic integration tests for demo application.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=SampleMethodSecurityApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
public class SampleMethodSecurityApplicationTests {

	@Test
	public void testHome() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = RestTemplates.get().exchange(
				"http://localhost:8080", HttpMethod.GET, new HttpEntity<Void>(headers),
				String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body (title doesn't match):\n" + entity.getBody(), entity
				.getBody().contains("<title>Login"));
	}

	@Test
	public void testLogin() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("username", "admin");
		form.set("password", "admin");
		getCsrf(form, headers);
		ResponseEntity<String> entity = RestTemplates.get().exchange(
				"http://localhost:8080/login", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.FOUND, entity.getStatusCode());
		assertEquals("http://localhost:8080/", entity.getHeaders().getLocation()
				.toString());
	}

	@Test
	public void testDenied() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();
		form.set("username", "user");
		form.set("password", "user");
		getCsrf(form, headers);
		ResponseEntity<String> entity = RestTemplates.get().exchange(
				"http://localhost:8080/login", HttpMethod.POST,
				new HttpEntity<MultiValueMap<String, String>>(form, headers),
				String.class);
		assertEquals(HttpStatus.FOUND, entity.getStatusCode());
		String cookie = entity.getHeaders().getFirst("Set-Cookie");
		headers.set("Cookie", cookie);
		ResponseEntity<String> page = RestTemplates.get().exchange(
				entity.getHeaders().getLocation(), HttpMethod.GET,
				new HttpEntity<Void>(headers), String.class);
		assertEquals(HttpStatus.FORBIDDEN, page.getStatusCode());
		assertTrue("Wrong body (message doesn't match):\n" + entity.getBody(), page
				.getBody().contains("Access denied"));
	}

	private void getCsrf(MultiValueMap<String, String> form, HttpHeaders headers) {
		ResponseEntity<String> page = RestTemplates.get().getForEntity(
				"http://localhost:8080/login", String.class);
		String cookie = page.getHeaders().getFirst("Set-Cookie");
		headers.set("Cookie", cookie);
		String body = page.getBody();
		Matcher matcher = Pattern.compile("(?s).*name=\"_csrf\".*?value=\"([^\"]+).*")
				.matcher(body);
		matcher.find();
		form.set("_csrf", matcher.group(1));
	}

}
