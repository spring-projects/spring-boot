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

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.resource.BaseOAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UserInfoTokenServices}.
 *
 * @author Dave Syer
 */
public class UserInfoTokenServicesTests {

	private UserInfoTokenServices services = new UserInfoTokenServices(
			"http://example.com", "foo");

	private BaseOAuth2ProtectedResourceDetails resource = new BaseOAuth2ProtectedResourceDetails();

	private OAuth2RestOperations template = mock(OAuth2RestOperations.class);

	private Map<String, Object> map = new LinkedHashMap<String, Object>();

	@Before
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void init() {
		this.resource.setClientId("foo");
		given(this.template.getForEntity(any(String.class), any(Class.class)))
				.willReturn(new ResponseEntity<Map>(this.map, HttpStatus.OK));
		given(this.template.getAccessToken()).willReturn(
				new DefaultOAuth2AccessToken("FOO"));
		given(this.template.getResource()).willReturn(this.resource);
		given(this.template.getOAuth2ClientContext()).willReturn(
				mock(OAuth2ClientContext.class));
	}

	@Test
	public void sunnyDay() {
		this.services.setRestTemplate(this.template);
		assertEquals("unknown", this.services.loadAuthentication("FOO").getName());
	}

	@Test
	public void userId() {
		this.map.put("userid", "spencer");
		this.services.setRestTemplate(this.template);
		assertEquals("spencer", this.services.loadAuthentication("FOO").getName());
	}

}
