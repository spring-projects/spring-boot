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

package sample;

import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = HelloWebSecurityApplication.class)
@IntegrationTest("server.port:0")
@WebAppConfiguration
public class HelloWebSecurityApplicationTests {

	@Autowired
	private FilterChainProxy springSecurityFilterChain;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain chain;

	@Before
	public void setup() {
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
		this.chain = new MockFilterChain();
	}

	@Test
	public void requiresAuthentication() throws Exception {
		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);

		assertThat(this.response.getStatus(),
				equalTo(HttpServletResponse.SC_UNAUTHORIZED));
	}

	@Test
	public void userAuthenticates() throws Exception {
		this.request.addHeader("Authorization",
				"Basic " + new String(Base64.encode("user:password".getBytes("UTF-8"))));

		this.springSecurityFilterChain.doFilter(this.request, this.response, this.chain);

		assertThat(this.response.getStatus(), equalTo(HttpServletResponse.SC_OK));
	}

}
