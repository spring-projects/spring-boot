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

package sample.actuator.ui;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

/**
 * Integration tests for separate management and main service ports.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=SampleActuatorUiApplication.class)
@WebAppConfiguration
@IntegrationTest
@DirtiesContext
@ActiveProfiles("management-port")
public class SampleActuatorUiApplicationPortTests {

	@Autowired
	private SecurityProperties security;

	@Value("${server.port}")
	private int port = 9010;

	@Value("${management.port}")
	private int managementPort = 9011;

	@Test
	public void testHome() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get().getForEntity(
				"http://localhost:" + port, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
	}

	@Test
	public void testMetrics() throws Exception {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = RestTemplates.get().getForEntity(
				"http://localhost:" + managementPort + "/metrics", Map.class);
		assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
	}

	@Test
	public void testHealth() throws Exception {
		ResponseEntity<String> entity = RestTemplates.get().getForEntity(
				"http://localhost:" + managementPort + "/health", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("ok", entity.getBody());
	}

}
