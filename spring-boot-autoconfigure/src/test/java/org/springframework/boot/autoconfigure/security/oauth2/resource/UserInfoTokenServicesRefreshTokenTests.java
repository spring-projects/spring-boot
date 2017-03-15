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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultExpiringOAuth2RefreshToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UserInfoTokenServices}.
 *
 * @author Dave Syer
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"security.oauth2.resource.userInfoUri:http://example.com",
		"security.oauth2.client.clientId=foo" })
@DirtiesContext
public class UserInfoTokenServicesRefreshTokenTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@LocalServerPort
	private int port;

	private UserInfoTokenServices services;

	@Before
	public void init() {
		this.services = new UserInfoTokenServices(
				"http://localhost:" + this.port + "/user", "foo");
	}

	@Test
	public void sunnyDay() {
		assertThat(this.services.loadAuthentication("FOO").getName()).isEqualTo("me");
	}

	@Test
	public void withRestTemplate() {
		OAuth2ProtectedResourceDetails resource = new AuthorizationCodeResourceDetails();
		OAuth2ClientContext context = new DefaultOAuth2ClientContext();
		DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("FOO");
		token.setRefreshToken(new DefaultExpiringOAuth2RefreshToken("BAR", new Date(0L)));
		context.setAccessToken(token);
		this.services.setRestTemplate(new OAuth2RestTemplate(resource, context));
		assertThat(this.services.loadAuthentication("FOO").getName()).isEqualTo("me");
		assertThat(context.getAccessToken().getValue()).isEqualTo("FOO");
		// The refresh token is still intact
		assertThat(context.getAccessToken().getRefreshToken())
				.isEqualTo(token.getRefreshToken());
	}

	@Test
	public void withRestTemplateChangesState() {
		OAuth2ProtectedResourceDetails resource = new AuthorizationCodeResourceDetails();
		OAuth2ClientContext context = new DefaultOAuth2ClientContext();
		context.setAccessToken(new DefaultOAuth2AccessToken("FOO"));
		this.services.setRestTemplate(new OAuth2RestTemplate(resource, context));
		assertThat(this.services.loadAuthentication("BAR").getName()).isEqualTo("me");
		assertThat(context.getAccessToken().getValue()).isEqualTo("BAR");
	}

	@Configuration
	@Import({ ServletWebServerFactoryAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })

	@RestController
	protected static class Application {

		@RequestMapping("/user")
		public User user(@RequestHeader("Authorization") String authorization) {
			if (authorization.endsWith("EXPIRED")) {
				throw new InvalidTokenException("Expired");
			}
			return new User();
		}

		@ExceptionHandler(InvalidTokenException.class)
		@ResponseStatus(HttpStatus.UNAUTHORIZED)
		public void expired() {
		}

	}

	public static class User {

		private String userid = "me";

		public String getUserid() {
			return this.userid;
		}

		public void setUserid(String userid) {
			this.userid = userid;
		}

	}

}
