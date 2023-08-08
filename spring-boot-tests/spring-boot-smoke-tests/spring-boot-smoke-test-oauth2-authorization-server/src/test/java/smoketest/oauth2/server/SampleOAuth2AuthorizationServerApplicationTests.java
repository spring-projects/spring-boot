/*
 * Copyright 2012-2023 the original author or authors.
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

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationServerMetadata;
import org.springframework.security.oauth2.server.authorization.oidc.OidcProviderConfiguration;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SampleOAuth2AuthorizationServerApplicationTests {

	private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
	};

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void openidConfigurationShouldAllowAccess() {
		ResponseEntity<Map<String, Object>> entity = this.restTemplate.exchange("/.well-known/openid-configuration",
				HttpMethod.GET, null, MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);

		OidcProviderConfiguration config = OidcProviderConfiguration.withClaims(entity.getBody()).build();
		assertThat(config.getIssuer()).hasToString("https://provider.com");
		assertThat(config.getAuthorizationEndpoint()).hasToString("https://provider.com/authorize");
		assertThat(config.getTokenEndpoint()).hasToString("https://provider.com/token");
		assertThat(config.getJwkSetUrl()).hasToString("https://provider.com/jwks");
		assertThat(config.getTokenRevocationEndpoint()).hasToString("https://provider.com/revoke");
		assertThat(config.getEndSessionEndpoint()).hasToString("https://provider.com/logout");
		assertThat(config.getTokenIntrospectionEndpoint()).hasToString("https://provider.com/introspect");
		assertThat(config.getUserInfoEndpoint()).hasToString("https://provider.com/user");
		// OIDC Client Registration is disabled by default
		assertThat(config.getClientRegistrationEndpoint()).isNull();
	}

	@Test
	void authServerMetadataShouldAllowAccess() {
		ResponseEntity<Map<String, Object>> entity = this.restTemplate
			.exchange("/.well-known/oauth-authorization-server", HttpMethod.GET, null, MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);

		OAuth2AuthorizationServerMetadata config = OAuth2AuthorizationServerMetadata.withClaims(entity.getBody())
			.build();
		assertThat(config.getIssuer()).hasToString("https://provider.com");
		assertThat(config.getAuthorizationEndpoint()).hasToString("https://provider.com/authorize");
		assertThat(config.getTokenEndpoint()).hasToString("https://provider.com/token");
		assertThat(config.getJwkSetUrl()).hasToString("https://provider.com/jwks");
		assertThat(config.getTokenRevocationEndpoint()).hasToString("https://provider.com/revoke");
		assertThat(config.getTokenIntrospectionEndpoint()).hasToString("https://provider.com/introspect");
		// OIDC Client Registration is disabled by default
		assertThat(config.getClientRegistrationEndpoint()).isNull();
	}

	@Test
	void anonymousShouldRedirectToLogin() {
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + this.port + "/login"));
	}

	@Test
	void validTokenRequestShouldReturnTokenResponse() {
		HttpHeaders headers = new HttpHeaders();
		headers.setBasicAuth("messaging-client", "secret");
		HttpEntity<Object> request = new HttpEntity<>(headers);
		String requestUri = UriComponentsBuilder.fromUriString("/token")
			.queryParam(OAuth2ParameterNames.CLIENT_ID, "messaging-client")
			.queryParam(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
			.queryParam(OAuth2ParameterNames.SCOPE, "message.read+message.write")
			.toUriString();
		ResponseEntity<Map<String, Object>> entity = this.restTemplate.exchange(requestUri, HttpMethod.POST, request,
				MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> tokenResponse = Objects.requireNonNull(entity.getBody());
		assertThat(tokenResponse.get(OAuth2ParameterNames.ACCESS_TOKEN)).isNotNull();
		assertThat(tokenResponse.get(OAuth2ParameterNames.EXPIRES_IN)).isNotNull();
		assertThat(tokenResponse.get(OAuth2ParameterNames.SCOPE)).isEqualTo("message.read message.write");
		assertThat(tokenResponse.get(OAuth2ParameterNames.TOKEN_TYPE))
			.isEqualTo(OAuth2AccessToken.TokenType.BEARER.getValue());
	}

	@Test
	void anonymousTokenRequestShouldReturnUnauthorized() {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<Object> request = new HttpEntity<>(headers);
		String requestUri = UriComponentsBuilder.fromUriString("/token")
			.queryParam(OAuth2ParameterNames.CLIENT_ID, "messaging-client")
			.queryParam(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
			.queryParam(OAuth2ParameterNames.SCOPE, "message.read+message.write")
			.toUriString();
		ResponseEntity<Map<String, Object>> entity = this.restTemplate.exchange(requestUri, HttpMethod.POST, request,
				MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anonymousTokenRequestWithAcceptHeaderAllShouldReturnUnauthorized() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.ALL));
		HttpEntity<Object> request = new HttpEntity<>(headers);
		String requestUri = UriComponentsBuilder.fromUriString("/token")
			.queryParam(OAuth2ParameterNames.CLIENT_ID, "messaging-client")
			.queryParam(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
			.queryParam(OAuth2ParameterNames.SCOPE, "message.read+message.write")
			.toUriString();
		ResponseEntity<Map<String, Object>> entity = this.restTemplate.exchange(requestUri, HttpMethod.POST, request,
				MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anonymousTokenRequestWithAcceptHeaderTextHtmlShouldRedirectToLogin() {
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(List.of(MediaType.TEXT_HTML));
		HttpEntity<Object> request = new HttpEntity<>(headers);
		String requestUri = UriComponentsBuilder.fromUriString("/token")
			.queryParam(OAuth2ParameterNames.CLIENT_ID, "messaging-client")
			.queryParam(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.CLIENT_CREDENTIALS.getValue())
			.queryParam(OAuth2ParameterNames.SCOPE, "message.read+message.write")
			.toUriString();
		ResponseEntity<Map<String, Object>> entity = this.restTemplate.exchange(requestUri, HttpMethod.POST, request,
				MAP_TYPE_REFERENCE);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(entity.getHeaders().getLocation()).isEqualTo(URI.create("http://localhost:" + this.port + "/login"));
	}

}
