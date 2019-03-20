/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UserInfoTokenServices}.
 *
 * @author Dave Syer
 */
public class UserInfoTokenServicesTests {

	@Rule
	public ExpectedException expected = ExpectedException.none();

	private UserInfoTokenServices services = new UserInfoTokenServices(
			"http://example.com", "foo");

	private BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();

	private OAuth2RestOperations template = mock(OAuth2RestOperations.class);

	private Map<String, Object> map = new LinkedHashMap<String, Object>();

	@SuppressWarnings("rawtypes")
	@Before
	public void init() {
		this.resource.setClientId("foo");
		given(this.template.getForEntity(any(String.class), eq(Map.class)))
				.willReturn(new ResponseEntity<Map>(this.map, HttpStatus.OK));
		given(this.template.getAccessToken())
				.willReturn(new DefaultOAuth2AccessToken("FOO"));
		given(this.template.getResource()).willReturn(this.resource);
		given(this.template.getOAuth2ClientContext())
				.willReturn(mock(OAuth2ClientContext.class));
	}

	@Test
	public void sunnyDay() {
		this.services.setRestTemplate(this.template);
		assertThat(this.services.loadAuthentication("FOO").getName())
				.isEqualTo("unknown");
	}

	@Test
	public void badToken() {
		this.services.setRestTemplate(this.template);
		given(this.template.getForEntity(any(String.class), eq(Map.class)))
				.willThrow(new UserRedirectRequiredException("foo:bar",
						Collections.<String, String>emptyMap()));
		this.expected.expect(InvalidTokenException.class);
		assertThat(this.services.loadAuthentication("FOO").getName())
				.isEqualTo("unknown");
	}

	@Test
	public void userId() {
		this.map.put("userid", "spencer");
		this.services.setRestTemplate(this.template);
		assertThat(this.services.loadAuthentication("FOO").getName())
				.isEqualTo("spencer");
	}

}
