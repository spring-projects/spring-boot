/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.oauth2.server;

import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.server.authorization.oidc.OidcProviderConfiguration;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleOAuth2AuthorizationServerApplicationTests {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
	};

	@LocalServerPort
	private int port;

	@Autowired
	private RestTestClient rest;

	private RestTestClient nonFollowingRedirect() {
		return RestTestClient
			.bindToServer(ClientHttpRequestFactoryBuilder.detect()
				.build(HttpClientSettings.defaults().withRedirects(HttpRedirects.DONT_FOLLOW)))
			.baseUrl("http://localhost:" + this.port)
			.build();
	}

	@Test
	void openidConfigurationShouldAllowAccess() {
		EntityExchangeResult<Map<String, Object>> result = this.rest.get()
			.uri("/.well-known/openid-configuration")
			.exchange()
			.returnResult(MAP_TYPE_REFERENCE);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = result.getResponseBody();
		assertThat(body).isNotNull();
		OidcProviderConfiguration config = OidcProviderConfiguration.withClaims(body).build();
		assertThat(config.getIssuer()).hasToString("https://provider.com");
		assertThat(config.getAuthorizationEndpoint()).hasToString("https://provider.com/authorize");
		assertThat(config.getTokenEndpoint()).hasToString("https://provider.com/token");
		assertThat(config.getJwkSetUrl()).hasToString("https://provider.com/jwks");
		assertThat(config.getTokenRevocationEndpoint()).hasToString("https://provider.com/revoke");
		assertThat(config.getEndSessionEndpoint()).hasToString("https://provider.com/logout");
		assertThat(config.getTokenIntrospectionEndpoint()).hasToString("https://provider.com/introspect");
		assertThat(config.getUserInfoEndpoint()).hasToString("https://provider.com/user");
		// PAR endpoint and OIDC Client Registration are disabled by default
		assertThat(config.getClientRegistrationEndpoint()).isNull();
		assertThat(config.getPushedAuthorizationRequestEndpoint()).isNull();
	}

	@Test
	void authServerMetadataShouldAllowAccess() {
		EntityExchangeResult<Map<String, Object>> result = this.rest.get()
			.uri("/.well-known/oauth-authorization-server")
			.exchange()
			.returnResult(MAP_TYPE_REFERENCE);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
		Map<String, Object> body = result.getResponseBody();
		assertThat(body).isNotNull();
		OAuth2AuthorizationServerMetadata config = OAuth2AuthorizationServerMetadata.withClaims(body).build();
		assertThat(config.getIssuer()).hasToString("https://provider.com");
		assertThat(config.getAuthorizationEndpoint()).hasToString("https://provider.com/authorize");
		assertThat(config.getTokenEndpoint()).hasToString("https://provider.com/token");
		assertThat(config.getJwkSetUrl()).hasToString("https://provider.com/jwks");
		assertThat(config.getTokenRevocationEndpoint()).hasToString("https://provider.com/revoke");
		assertThat(config.getTokenIntrospectionEndpoint()).hasToString("https://provider.com/introspect");
		// PAR endpoint and OIDC Client Registration are disabled by default
		assertThat(config.getClientRegistrationEndpoint()).isNull();
		assertThat(config.getPushedAuthorizationRequestEndpoint()).isNull();
	}

	@Test
	void anonymousShouldRedirectToLogin() {
		RestTestClient.ResponseSpec response = nonFollowingRedirect().get().uri("/").exchange();
		response.expectStatus().isFound();
		response.expectHeader().location("http://localhost:" + this.port + "/login");
	}

	@Test
	void validTokenRequestShouldReturnTokenResponse() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add(OAuth2ParameterNames.CLIENT_ID, "messaging-client");
		body.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
		body.add(OAuth2ParameterNames.SCOPE, "message.read message.write");
		EntityExchangeResult<Map<String, Object>> result = this.rest.post()
			.uri("/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.headers((headers) -> headers.setBasicAuth("messaging-client", "secret"))
			.body(body)
			.exchange()
			.returnResult(MAP_TYPE_REFERENCE);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.OK);
		Map<String, Object> tokenResponse = Objects.requireNonNull(result.getResponseBody());
		assertThat(tokenResponse.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull();
		assertThat(tokenResponse.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull();
		assertThat(tokenResponse.get(OAuth2ParameterNames.SCOPE)).isEqualTo("message.read message.write");
		assertThat(tokenResponse.get(OAuth2ParameterNames.TOKEN_TYPE))
			.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue());
	}

	@Test
	void anonymousTokenRequestShouldReturnUnauthorized() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add(OAuth2ParameterNames.CLIENT_ID, "messaging-client");
		body.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
		body.add(OAuth2ParameterNames.SCOPE, "message.read message.write");
		EntityExchangeResult<Map<String, Object>> result = this.rest.post()
			.uri("/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(body)
			.exchange()
			.returnResult(MAP_TYPE_REFERENCE);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anonymousTokenRequestWithAcceptHeaderAllShouldReturnUnauthorized() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add(OAuth2ParameterNames.CLIENT_ID, "messaging-client");
		body.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
		body.add(OAuth2ParameterNames.SCOPE, "message.read message.write");
		EntityExchangeResult<Map<String, Object>> result = this.rest.post()
			.uri("/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.ALL)
			.body(body)
			.exchange()
			.returnResult(MAP_TYPE_REFERENCE);
		assertThat(result.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anonymousTokenRequestWithAcceptHeaderTextHtmlShouldRedirectToLogin() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
		body.add(OAuth2ParameterNames.CLIENT_ID, "messaging-client");
		body.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
		body.add(OAuth2ParameterNames.SCOPE, "message.read message.write");
		RestTestClient.ResponseSpec response = nonFollowingRedirect().post()
			.uri("/token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.accept(MediaType.TEXT_HTML)
			.body(body)
			.exchange();
		response.expectStatus().isFound();
		response.expectHeader().location("http://localhost:" + this.port + "/login");
	}

}
