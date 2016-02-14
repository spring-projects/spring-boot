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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Date;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServicesRefreshTokenTests.Application;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link UserInfoTokenServices}.
 *
 * @author Dave Syer
 */
@SpringApplicationConfiguration(classes = Application.class)
@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest({ "server.port=0",
		"security.oauth2.resource.userInfoUri:http://example.com",
		"security.oauth2.client.clientId=foo" })
@DirtiesContext
public class UserInfoTokenServicesRefreshTokenTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Value("${local.server.port}")
	private int port;

	private UserInfoTokenServices services;

	@Before
	public void init() {
		this.services = new UserInfoTokenServices(
				"http://localhost:" + this.port + "/user", "foo");
	}

	@Test
	public void sunnyDay() {
		assertEquals("me", this.services.loadAuthentication("FOO").getName());
	}

	@Test
	public void withRestTemplate() {
		OAuth2ProtectedResourceDetails resource = new AuthorizationCodeResourceDetails();
		OAuth2ClientContext context = new DefaultOAuth2ClientContext();
		DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken("FOO");
		token.setRefreshToken(new DefaultExpiringOAuth2RefreshToken("BAR", new Date(0L)));
		context.setAccessToken(token);
		this.services.setRestTemplate(new OAuth2RestTemplate(resource, context));
		assertEquals("me", this.services.loadAuthentication("FOO").getName());
		assertEquals("FOO", context.getAccessToken().getValue());
		// The refresh token is still intact
		assertEquals(token.getRefreshToken(), context.getAccessToken().getRefreshToken());
	}

	@Test
	public void withRestTemplateChangesState() {
		OAuth2ProtectedResourceDetails resource = new AuthorizationCodeResourceDetails();
		OAuth2ClientContext context = new DefaultOAuth2ClientContext();
		context.setAccessToken(new DefaultOAuth2AccessToken("FOO"));
		this.services.setRestTemplate(new OAuth2RestTemplate(resource, context));
		assertEquals("me", this.services.loadAuthentication("BAR").getName());
		assertEquals("BAR", context.getAccessToken().getValue());
	}

	@Configuration
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
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
