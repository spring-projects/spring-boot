/*
 * Copyright 2012-2016 the original author or authors.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.validation.Errors;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ResourceServerProperties}.
 *
 * @author Dave Syer
 * @author Vedran Pavic
 * @author Madhura Bhave
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
		assertThat(jwt.get("keyUri")).isNotNull();
	}

	@Test
	public void tokenKeyDerivedFromUserInfoUri() throws Exception {
		this.properties.setUserInfoUri("http://example.com/userinfo");
		assertThat(this.properties.getJwt().getKeyUri())
				.isEqualTo("http://example.com/token_key");
	}

	@Test
	public void tokenKeyDerivedFromTokenInfoUri() throws Exception {
		this.properties.setTokenInfoUri("http://example.com/check_token");
		assertThat(this.properties.getJwt().getKeyUri())
				.isEqualTo("http://example.com/token_key");
	}

	@Test
	public void validateWhenBothJwtAndJwtKeyConfigurationPresentShouldFail() throws Exception {
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		this.properties.getJwt().setKeyUri("http://my-auth-server/token_key");
		setListableBeanFactory();
		Errors errors = mock(Errors.class);
		this.properties.validate(this.properties, errors);
		verify(errors).reject("ambiguous.keyUri", "Only one of jwt.keyUri (or jwt.keyValue) and jwk.keySetUri should be configured.");

	}

	@Test
	public void validateWhenKeySetUriProvidedShouldSucceed() throws Exception {
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		setListableBeanFactory();
		Errors errors = mock(Errors.class);
		this.properties.validate(this.properties, errors);
		verifyZeroInteractions(errors);
	}

	private void setListableBeanFactory() {
		ListableBeanFactory beanFactory = new StaticWebApplicationContext() {
			@Override
			public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
				if (type.isAssignableFrom(ResourceServerTokenServicesConfiguration.class)) {
					return new String[]{"ResourceServerTokenServicesConfiguration"};
				}
				return new String[0];
			}
		};
		this.properties.setBeanFactory(beanFactory);
	}
}
