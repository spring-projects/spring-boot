/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.MinimalActuatorHypermediaApplication;
import org.springframework.boot.actuate.endpoint.mvc.HalBrowserMvcEndpointServerContextPathIntegrationTests.SpringBootHypermediaApplication;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

/**
 * Integration tests for {@link HalBrowserMvcEndpoint} when a custom server context
 * path has been configured.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(SpringBootHypermediaApplication.class)
@WebAppConfiguration
@IntegrationTest({ "server.port=0", "server.contextPath=/spring" })
@DirtiesContext
public class HalBrowserMvcEndpointServerContextPathIntegrationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void linksAddedToHomePage() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/spring/", HttpMethod.GET,
				new HttpEntity<Void>(null, headers), String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("\"_links\":"));
	}

	@Test
	public void actuatorBrowser() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML));
		ResponseEntity<String> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/spring/actuator/", HttpMethod.GET,
				new HttpEntity<Void>(null, headers), String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("<title"));
	}

	@Test
	public void actuatorLinks() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		ResponseEntity<String> entity = new TestRestTemplate().exchange(
				"http://localhost:" + this.port + "/spring/actuator", HttpMethod.GET,
				new HttpEntity<Void>(null, headers), String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertTrue("Wrong body: " + entity.getBody(),
				entity.getBody().contains("\"_links\":"));
	}

	@MinimalActuatorHypermediaApplication
	@RestController
	public static class SpringBootHypermediaApplication {

		@RequestMapping("")
		public ResourceSupport home() {
			ResourceSupport resource = new ResourceSupport();
			resource.add(linkTo(SpringBootHypermediaApplication.class).slash("/")
					.withSelfRel());
			return resource;
		}

	}

}
