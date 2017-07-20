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

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
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

	private Errors errors = mock(Errors.class);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public void validateWhenClientIdNullShouldNotFail() throws Exception {
		this.properties = new ResourceServerProperties(null, "secret");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenBothJwtAndJwkKeyUrisPresentShouldFail() throws Exception {
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		this.properties.getJwt().setKeyUri("http://my-auth-server/token_key");
		setListableBeanFactory();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expect(getMatcher("Only one of jwt.keyUri (or jwt.keyValue) "
				+ "and jwk.keySetUri should be configured.", null));
		this.properties.validate();
	}

	@Test
	public void validateWhenBothJwtKeyValueAndJwkKeyUriPresentShouldFail()
			throws Exception {
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		this.properties.getJwt().setKeyValue("my-key");
		setListableBeanFactory();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expect(getMatcher("Only one of jwt.keyUri (or jwt.keyValue) "
				+ "and jwk.keySetUri should be configured.", null));
		this.properties.validate();
	}

	@Test
	public void validateWhenJwkKeySetUriProvidedShouldSucceed() throws Exception {
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenKeyValuePresentShouldSucceed() throws Exception {
		this.properties.getJwt().setKeyValue("my-key");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenKeysUriOrValuePresentAndUserInfoAbsentShouldNotFail()
			throws Exception {
		this.properties = new ResourceServerProperties("client", "");
		this.properties.getJwk().setKeySetUri("http://my-auth-server/token_keys");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenKeyConfigAbsentAndInfoUrisNotConfiguredShouldFail()
			throws Exception {
		setListableBeanFactory();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expect(getMatcher("Missing tokenInfoUri and userInfoUri and there"
				+ " is no JWT verifier key", "tokenInfoUri"));
		this.properties.validate();
	}

	@Test
	public void validateWhenTokenUriConfiguredShouldNotFail() throws Exception {
		this.properties.setTokenInfoUri("http://my-auth-server/userinfo");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenUserInfoUriConfiguredShouldNotFail() throws Exception {
		this.properties.setUserInfoUri("http://my-auth-server/userinfo");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenTokenUriPreferredAndClientSecretAbsentShouldFail()
			throws Exception {
		this.properties = new ResourceServerProperties("client", "");
		this.properties.setTokenInfoUri("http://my-auth-server/check_token");
		this.properties.setUserInfoUri("http://my-auth-server/userinfo");
		setListableBeanFactory();
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expect(getMatcher("Missing client secret", "clientSecret"));
		this.properties.validate();
	}

	@Test
	public void validateWhenTokenUriAbsentAndClientSecretAbsentShouldNotFail()
			throws Exception {
		this.properties = new ResourceServerProperties("client", "");
		this.properties.setUserInfoUri("http://my-auth-server/userinfo");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	@Test
	public void validateWhenTokenUriNotPreferredAndClientSecretAbsentShouldNotFail()
			throws Exception {
		this.properties = new ResourceServerProperties("client", "");
		this.properties.setPreferTokenInfo(false);
		this.properties.setTokenInfoUri("http://my-auth-server/check_token");
		this.properties.setUserInfoUri("http://my-auth-server/userinfo");
		setListableBeanFactory();
		this.properties.validate();
		verifyZeroInteractions(this.errors);
	}

	private void setListableBeanFactory() {
		ListableBeanFactory beanFactory = new StaticWebApplicationContext() {

			@Override
			public String[] getBeanNamesForType(Class<?> type,
					boolean includeNonSingletons, boolean allowEagerInit) {
				if (type.isAssignableFrom(
						ResourceServerTokenServicesConfiguration.class)) {
					return new String[] { "ResourceServerTokenServicesConfiguration" };
				}
				return new String[0];
			}

		};
		this.properties.setBeanFactory(beanFactory);
	}

	private BaseMatcher<BindException> getMatcher(String message, String field) {
		return new BaseMatcher<BindException>() {

			@Override
			public void describeTo(Description description) {

			}

			@Override
			public boolean matches(Object item) {
				BindException ex = (BindException) ((Exception) item).getCause();
				ObjectError error = ex.getAllErrors().get(0);
				boolean messageMatches = message.equals(error.getDefaultMessage());
				if (field == null) {
					return messageMatches;
				}
				String fieldErrors = ((FieldError) error).getField();
				return messageMatches && fieldErrors.equals(field);
			}

		};
	}

}
