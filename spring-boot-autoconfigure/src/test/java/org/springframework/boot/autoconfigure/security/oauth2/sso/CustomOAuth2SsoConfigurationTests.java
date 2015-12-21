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

package org.springframework.boot.autoconfigure.security.oauth2.sso;

import javax.servlet.Filter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.sso.CustomOAuth2SsoConfigurationTests.TestConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link OAuth2AutoConfiguration} with custom configuration.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(TestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = { "security.oauth2.client.clientId=client",
		"security.oauth2.client.clientSecret=secret",
		"security.oauth2.client.authorizationUri=http://example.com/oauth/authorize",
		"security.oauth2.client.tokenUri=http://example.com/oauth/token",
		"security.oauth2.resource.jwt.keyValue=SSSSHHH" })
public class CustomOAuth2SsoConfigurationTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	@Qualifier("springSecurityFilterChain")
	private Filter filter;

	private MockMvc mvc;

	@Before
	public void init() {
		this.mvc = MockMvcBuilders.webAppContextSetup(this.context)
				.addFilters(this.filter).build();
	}

	@Test
	public void homePageIsBasicAuth() throws Exception {
		this.mvc.perform(get("/")).andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", startsWith("Basic")));
	}

	@Test
	public void uiPageIsSecure() throws Exception {
		this.mvc.perform(get("/ui/")).andExpect(status().isFound())
				.andExpect(header().string("location", "http://localhost/login"));
	}

	@Test
	public void uiTestPageIsAccessible() throws Exception {
		this.mvc.perform(get("/ui/test")).andExpect(status().isOk())
				.andExpect(content().string("test"));
	}

	@Configuration
	@EnableOAuth2Sso
	@Import(OAuth2AutoConfiguration.class)
	@MinimalSecureWebConfiguration
	protected static class TestConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		public void configure(HttpSecurity http) throws Exception {
			http.antMatcher("/ui/**").authorizeRequests().antMatchers("/ui/test")
					.permitAll().anyRequest().authenticated();
		}

		@RestController
		public static class TestController {

			@RequestMapping("/ui/test")
			public String test() {
				return "test";
			}

		}

	}

}
