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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AuthorizationExceptionMatcher;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

/**
 * Tests for {@link CloudFoundrySecurityService}.
 *
 * @author Madhura Bhave
 */
public class CloudFoundrySecurityServiceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String CLOUD_CONTROLLER = "http://my-cloud-controller.com";

	private static final String CLOUD_CONTROLLER_PERMISSIONS = CLOUD_CONTROLLER
			+ "/v2/apps/my-app-id/permissions";

	private static final String UAA_URL = "http://my-uaa.com";

	private CloudFoundrySecurityService securityService;

	private MockRestServiceServer server;

	@Before
	public void setup() {
		MockServerRestTemplateCustomizer mockServerCustomizer = new MockServerRestTemplateCustomizer();
		RestTemplateBuilder builder = new RestTemplateBuilder(mockServerCustomizer);
		this.securityService = new CloudFoundrySecurityService(builder, CLOUD_CONTROLLER,
				false);
		this.server = mockServerCustomizer.getServer();
	}

	@Test
	public void skipSslValidationWhenTrue() {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		this.securityService = new CloudFoundrySecurityService(builder, CLOUD_CONTROLLER,
				true);
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils
				.getField(this.securityService, "restTemplate");
		assertThat(restTemplate.getRequestFactory())
				.isInstanceOf(SkipSslVerificationHttpRequestFactory.class);
	}

	@Test
	public void doNotskipSslValidationWhenFalse() {
		RestTemplateBuilder builder = new RestTemplateBuilder();
		this.securityService = new CloudFoundrySecurityService(builder, CLOUD_CONTROLLER,
				false);
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils
				.getField(this.securityService, "restTemplate");
		assertThat(restTemplate.getRequestFactory())
				.isNotInstanceOf(SkipSslVerificationHttpRequestFactory.class);
	}

	@Test
	public void getAccessLevelWhenSpaceDeveloperShouldReturnFull() {
		String responseBody = "{\"read_sensitive_data\": true,\"read_basic_data\": true}";
		this.server.expect(requestTo(CLOUD_CONTROLLER_PERMISSIONS))
				.andExpect(header("Authorization", "bearer my-access-token"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		AccessLevel accessLevel = this.securityService.getAccessLevel("my-access-token",
				"my-app-id");
		this.server.verify();
		assertThat(accessLevel).isEqualTo(AccessLevel.FULL);
	}

	@Test
	public void getAccessLevelWhenNotSpaceDeveloperShouldReturnRestricted() {
		String responseBody = "{\"read_sensitive_data\": false,\"read_basic_data\": true}";
		this.server.expect(requestTo(CLOUD_CONTROLLER_PERMISSIONS))
				.andExpect(header("Authorization", "bearer my-access-token"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		AccessLevel accessLevel = this.securityService.getAccessLevel("my-access-token",
				"my-app-id");
		this.server.verify();
		assertThat(accessLevel).isEqualTo(AccessLevel.RESTRICTED);
	}

	@Test
	public void getAccessLevelWhenTokenIsNotValidShouldThrowException() {
		this.server.expect(requestTo(CLOUD_CONTROLLER_PERMISSIONS))
				.andExpect(header("Authorization", "bearer my-access-token"))
				.andRespond(withUnauthorizedRequest());
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.INVALID_TOKEN));
		this.securityService.getAccessLevel("my-access-token", "my-app-id");
	}

	@Test
	public void getAccessLevelWhenForbiddenShouldThrowException() {
		this.server.expect(requestTo(CLOUD_CONTROLLER_PERMISSIONS))
				.andExpect(header("Authorization", "bearer my-access-token"))
				.andRespond(withStatus(HttpStatus.FORBIDDEN));
		this.thrown
				.expect(AuthorizationExceptionMatcher.withReason(Reason.ACCESS_DENIED));
		this.securityService.getAccessLevel("my-access-token", "my-app-id");
	}

	@Test
	public void getAccessLevelWhenCloudControllerIsNotReachableThrowsException() {
		this.server.expect(requestTo(CLOUD_CONTROLLER_PERMISSIONS))
				.andExpect(header("Authorization", "bearer my-access-token"))
				.andRespond(withServerError());
		this.thrown.expect(
				AuthorizationExceptionMatcher.withReason(Reason.SERVICE_UNAVAILABLE));
		this.securityService.getAccessLevel("my-access-token", "my-app-id");
	}

	@Test
	public void fetchTokenKeysWhenSuccessfulShouldReturnListOfKeysFromUAA() {
		this.server.expect(requestTo(CLOUD_CONTROLLER + "/info"))
				.andRespond(withSuccess("{\"token_endpoint\":\"http://my-uaa.com\"}",
						MediaType.APPLICATION_JSON));
		String tokenKeyValue = "-----BEGIN PUBLIC KEY-----\n"
				+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0m59l2u9iDnMbrXHfqkO\n"
				+ "rn2dVQ3vfBJqcDuFUK03d+1PZGbVlNCqnkpIJ8syFppW8ljnWweP7+LiWpRoz0I7\n"
				+ "fYb3d8TjhV86Y997Fl4DBrxgM6KTJOuE/uxnoDhZQ14LgOU2ckXjOzOdTsnGMKQB\n"
				+ "LCl0vpcXBtFLMaSbpv1ozi8h7DJyVZ6EnFQZUWGdgTMhDrmqevfx95U/16c5WBDO\n"
				+ "kqwIn7Glry9n9Suxygbf8g5AzpWcusZgDLIIZ7JTUldBb8qU2a0Dl4mvLZOn4wPo\n"
				+ "jfj9Cw2QICsc5+Pwf21fP+hzf+1WSRHbnYv8uanRO0gZ8ekGaghM/2H6gqJbo2nI\n"
				+ "JwIDAQAB\n-----END PUBLIC KEY-----";
		String responseBody = "{\"keys\" : [ {\"kid\":\"test-key\",\"value\" : \""
				+ tokenKeyValue.replace("\n", "\\n") + "\"} ]}";
		this.server.expect(requestTo(UAA_URL + "/token_keys"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		Map<String, String> tokenKeys = this.securityService.fetchTokenKeys();
		this.server.verify();
		assertThat(tokenKeys.get("test-key")).isEqualTo(tokenKeyValue);
	}

	@Test
	public void fetchTokenKeysWhenNoKeysReturnedFromUAA() {
		this.server.expect(requestTo(CLOUD_CONTROLLER + "/info")).andRespond(withSuccess(
				"{\"token_endpoint\":\"" + UAA_URL + "\"}", MediaType.APPLICATION_JSON));
		String responseBody = "{\"keys\": []}";
		this.server.expect(requestTo(UAA_URL + "/token_keys"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		Map<String, String> tokenKeys = this.securityService.fetchTokenKeys();
		this.server.verify();
		assertThat(tokenKeys).hasSize(0);
	}

	@Test
	public void fetchTokenKeysWhenUnsuccessfulShouldThrowException() {
		this.server.expect(requestTo(CLOUD_CONTROLLER + "/info")).andRespond(withSuccess(
				"{\"token_endpoint\":\"" + UAA_URL + "\"}", MediaType.APPLICATION_JSON));
		this.server.expect(requestTo(UAA_URL + "/token_keys"))
				.andRespond(withServerError());
		this.thrown.expect(
				AuthorizationExceptionMatcher.withReason(Reason.SERVICE_UNAVAILABLE));
		this.securityService.fetchTokenKeys();
	}

	@Test
	public void getUaaUrlShouldCallCloudControllerInfoOnlyOnce() {
		this.server.expect(requestTo(CLOUD_CONTROLLER + "/info")).andRespond(withSuccess(
				"{\"token_endpoint\":\"" + UAA_URL + "\"}", MediaType.APPLICATION_JSON));
		String uaaUrl = this.securityService.getUaaUrl();
		this.server.verify();
		assertThat(uaaUrl).isEqualTo(UAA_URL);
		// Second call should not need to hit server
		uaaUrl = this.securityService.getUaaUrl();
		assertThat(uaaUrl).isEqualTo(UAA_URL);
	}

	@Test
	public void getUaaUrlWhenCloudControllerUrlIsNotReachableShouldThrowException() {
		this.server.expect(requestTo(CLOUD_CONTROLLER + "/info"))
				.andRespond(withServerError());
		this.thrown.expect(
				AuthorizationExceptionMatcher.withReason(Reason.SERVICE_UNAVAILABLE));
		this.securityService.getUaaUrl();
	}

}
