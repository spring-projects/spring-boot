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

package sample.secure;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import sample.secure.SampleSecureApplicationTests.TestConfiguration;

/**
 * Basic integration tests for demo application.
 * 
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { SampleSecureApplication.class,
		TestConfiguration.class })
public class SampleSecureApplicationTests {

	@Autowired
	private SampleService service;

	@Autowired
	private Authentication authentication;

	@After
	public void close() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthenticationException.class)
	public void secure() throws Exception {
		assertEquals(service.secure(), "Hello Security");
	}

	@Test
	public void authenticated() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(authentication);
		assertEquals(service.secure(), "Hello Security");
	}

	@Test
	public void preauth() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(authentication);
		assertEquals(service.authorized(), "Hello World");
	}

	@Test(expected = AccessDeniedException.class)
	public void denied() throws Exception {
		SecurityContextHolder.getContext().setAuthentication(authentication);
		assertEquals(service.denied(), "Goodbye World");
	}

	@PropertySource("classpath:test.properties")
	@Configuration
	protected static class TestConfiguration {

		@Autowired
		private AuthenticationManager authenticationManager;

		@Bean
		public Authentication user() {
			return authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken("user",
							"password"));
		}

	}

}
