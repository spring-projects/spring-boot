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

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.RestTemplates;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertEquals;

/**
 * Integration tests for switching off management endpoints.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleActuatorApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
@ActiveProfiles("nomanagement")
public class NoManagementSampleActuatorApplicationTests {

	@Autowired
	private SecurityProperties security;

	@Test
	public void testHome() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = RestTemplates.get("user", getPassword())
				.getForEntity("http://localhost:8080", Map.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertEquals("Hello Phil", body.get("message"));
	}

	@Test
	public void testMetricsNotAvailable() throws Exception {
		testHome(); // makes sure some requests have been made
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = RestTemplates.get("user", getPassword())
				.getForEntity("http://localhost:8080/metrics", Map.class);
		assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
	}

	private String getPassword() {
		return this.security.getUser().getPassword();
	}

}
