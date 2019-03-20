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

package org.springframework.boot.autoconfigure.security.oauth2.sso;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test to validate that a custom {@link RestTemplate} can be defined with OAuth2 SSO.
 *
 * @author Stephane Nicoll
 */
@RunWith(SpringRunner.class)
@DirtiesContext
@SpringBootTest
@TestPropertySource(properties = { "security.oauth2.client.clientId=client",
		"security.oauth2.client.clientSecret=secret",
		"security.oauth2.client.userAuthorizationUri=http://example.com/oauth/authorize",
		"security.oauth2.client.accessTokenUri=http://example.com/oauth/token",
		"security.oauth2.resource.jwt.keyValue=SSSSHHH" })
public class CustomRestTemplateBasicOAuth2SsoConfigurationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private ObjectProvider<RestTemplate> restTemplate;

	@Test
	public void customRestTemplateCanBePrimary() {
		RestTemplate restTemplate = this.restTemplate.getIfAvailable();
		verifyZeroInteractions(restTemplate);
		assertThat(this.applicationContext.getBeansOfType(RestTemplate.class)).hasSize(1);
	}

	@Configuration
	@Import(OAuth2AutoConfiguration.class)
	@EnableOAuth2Sso
	@MinimalSecureWebConfiguration
	protected static class TestConfiguration {

		@Bean
		@Primary
		public RestTemplate myRestTemplate() {
			return mock(RestTemplate.class);
		}

	}

}
