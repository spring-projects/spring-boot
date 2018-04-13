/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { SampleSecureApplication.class })
public class SampleSecureApplicationTests {

	@Autowired
	private SampleService service;

	private Authentication authentication;

	@Before
	public void init() {
		this.authentication = new UsernamePasswordAuthenticationToken("user", "password");
	}

	@After
	public void close() {
		SecurityContextHolder.clearContext();
	}

	@Test(expected = AuthenticationException.class)
	public void secure() {
		assertThat("Hello Security").isEqualTo(this.service.secure());
	}

	@Test
	public void authenticated() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat("Hello Security").isEqualTo(this.service.secure());
	}

	@Test
	public void preauth() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat("Hello World").isEqualTo(this.service.authorized());
	}

	@Test(expected = AccessDeniedException.class)
	public void denied() {
		SecurityContextHolder.getContext().setAuthentication(this.authentication);
		assertThat("Goodbye World").isEqualTo(this.service.denied());
	}

}
