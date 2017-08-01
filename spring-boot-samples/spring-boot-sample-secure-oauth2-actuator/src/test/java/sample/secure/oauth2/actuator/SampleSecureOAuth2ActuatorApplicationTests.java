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

package sample.secure.oauth2.actuator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Series of automated integration tests to verify proper behavior of auto-configured,
 * OAuth2-secured system
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class SampleSecureOAuth2ActuatorApplicationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private FilterChainProxy filterChain;

	private MockMvc mvc;

	@Before
	public void setUp() {
		this.mvc = webAppContextSetup(this.context).addFilters(this.filterChain).build();
		SecurityContextHolder.clearContext();
	}

	@Test
	public void homePageSecuredByDefault() throws Exception {
		this.mvc.perform(get("/")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("Bearer")))
				.andDo(print());
	}

	@Test
	public void healthSecured() throws Exception {
		this.mvc.perform(get("/application/health")).andExpect(status().isUnauthorized());
	}

	@Test
	public void healthWithBasicAuthorization() throws Exception {
		this.mvc.perform(get("/application/health").header("Authorization",
				"Basic " + Base64Utils.encodeToString("user:password".getBytes())))
				.andExpect(status().isOk());
	}

	@Test
	public void envSecured() throws Exception {
		this.mvc.perform(get("/application/env")).andExpect(status().isUnauthorized());
	}

	@Test
	public void envWithBasicAuthorization() throws Exception {
		this.mvc.perform(get("/application/env").header("Authorization",
				"Basic " + Base64Utils.encodeToString("user:password".getBytes())))
				.andExpect(status().isOk()).andDo(print());
	}

}
