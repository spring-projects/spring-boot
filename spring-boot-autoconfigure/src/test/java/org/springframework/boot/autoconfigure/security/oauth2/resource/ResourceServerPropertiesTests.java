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

import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link ResourceServerProperties}.
 *
 * @author Dave Syer
 */
public class ResourceServerPropertiesTests {

	private ResourceServerProperties properties = new ResourceServerProperties("client",
			"secret");

	@Test
	@SuppressWarnings("unchecked")
	public void json() throws Exception {
		this.properties.getJwt().setKeyUri("http://example.com/token_key");
		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(this.properties);
		Map<String, Object> value = mapper.readValue(json, Map.class);
		Map<String, Object> jwt = (Map<String, Object>) value.get("jwt");
		assertNotNull("Wrong json: " + json, jwt.get("keyUri"));
	}

	@Test
	public void tokenKeyDerived() throws Exception {
		this.properties.setUserInfoUri("http://example.com/userinfo");
		assertNotNull("Wrong properties: " + this.properties, this.properties.getJwt()
				.getKeyUri());
	}

}
