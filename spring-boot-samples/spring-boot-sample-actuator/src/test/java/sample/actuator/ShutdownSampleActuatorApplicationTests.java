/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.actuator;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for separate management and main service ports.
 *
 * @author Dave Syer
 */
@SpringBootTest(classes = { ShutdownSampleActuatorApplicationTests.SecurityConfiguration.class,
		SampleActuatorApplication.class }, webEnvironment = WebEnvironment.RANDOM_PORT)
class ShutdownSampleActuatorApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void testHome() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = this.restTemplate.withBasicAuth("user", getPassword()).getForEntity("/",
				Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertThat(body.get("message")).isEqualTo("Hello Phil");
	}

	@Test
	@DirtiesContext
	void testShutdown() {
		@SuppressWarnings("rawtypes")
		ResponseEntity<Map> entity = this.restTemplate.withBasicAuth("user", getPassword())
				.postForEntity("/actuator/shutdown", null, Map.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = entity.getBody();
		assertThat(((String) body.get("message"))).contains("Shutting down");
	}

	private String getPassword() {
		return "password";
	}

	@Configuration(proxyBeanMethods = false)
	static class SecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.csrf().disable();
		}

	}

}
