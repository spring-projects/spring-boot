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
 *
 * @author Monica Granbois
 */

package sample.data.jdbc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;
import sample.data.jdbc.domain.User;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

@RunWith (SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration (classes = SampleDataJdbcApplication.class)
@WebAppConfiguration
@IntegrationTest ("server.port:0")
public class SampleDataJdbcApplicationIntegrationTests {

	@Value ("${local.server.port}")
	private int port;

	@Test
	public void testBadPath() throws Exception {
		ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + this.port + "/junk", String.class);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());

	}

	@Test
	public void testGetUser() throws Exception {
		User user = new TestRestTemplate().getForObject("http://localhost:" + this.port + "/users/user/1", User.class);
		assertNotNull(user);
		assertEquals("Fred", user.getFirstName());
		assertEquals("Flintstone", user.getLastName());

	}

	@Test
	public void testUserDoesNotExist() throws Exception {
		ResponseEntity<String> response = new TestRestTemplate().getForEntity("http://localhost:" + this.port + "/users/user/40001", String.class);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
	}

	@Test
	public void testListUsers() throws Exception {
		ResponseEntity<User[]> results = new TestRestTemplate().getForEntity("http://localhost:" + this.port + "/users", User[].class);
		assertNotNull(results);
		assertEquals(5, results.getBody().length);
	}


	@Test
	public void testCreateUser() throws Exception {
		RestTemplate restTemplate = new TestRestTemplate();

		User user = new User();
		user.setFirstName("BARNEY");
		user.setLastName("RUBBLE");

		assertNull(user.getId());
		User result = restTemplate.postForObject("http://localhost:" + this.port + "/users/user", user, User.class);
		assertNotNull(result.getId());
		assertTrue(result.getId() > 0);
		assertEquals("Barney", result.getFirstName());
		assertEquals("Rubble", result.getLastName());


		ResponseEntity<User[]> results = restTemplate.getForEntity("http://localhost:" + this.port + "/users", User[].class);
		assertNotNull(results);
		assertEquals(6, results.getBody().length);


	}

	@Test
	public void testCreateInvalidUser() throws Exception {
		RestTemplate restTemplate = new TestRestTemplate();
		ResponseEntity<String> result = restTemplate.postForEntity("http://localhost:" + this.port + "/users/user", new User(), String.class);
		assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());

	}


	@Test
	public void testDuplicateUser() throws Exception {
		User user = new User();
		user.setFirstName("Lisa");
		user.setLastName("Simpson");
		RestTemplate restTemplate = new TestRestTemplate();
		ResponseEntity<String> result = restTemplate.postForEntity("http://localhost:" + this.port + "/users/user", user, String.class);
		assertEquals(HttpStatus.CONFLICT, result.getStatusCode());

	}


}
