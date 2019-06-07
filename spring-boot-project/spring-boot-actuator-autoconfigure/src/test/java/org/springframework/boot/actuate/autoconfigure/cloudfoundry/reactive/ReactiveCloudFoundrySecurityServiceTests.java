/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.reactive;

import java.util.function.Consumer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.test.StepVerifier;

import org.springframework.boot.actuate.autoconfigure.cloudfoundry.AccessLevel;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException;
import org.springframework.boot.actuate.autoconfigure.cloudfoundry.CloudFoundryAuthorizationException.Reason;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveCloudFoundrySecurityService}.
 *
 * @author Madhura Bhave
 */
public class ReactiveCloudFoundrySecurityServiceTests {

	private static final String CLOUD_CONTROLLER = "/my-cloud-controller.com";

	private static final String CLOUD_CONTROLLER_PERMISSIONS = CLOUD_CONTROLLER + "/v2/apps/my-app-id/permissions";

	private static final String UAA_URL = "https://my-cloud-controller.com/uaa";

	private ReactiveCloudFoundrySecurityService securityService;

	private MockWebServer server;

	private WebClient.Builder builder;

	@Before
	public void setup() {
		this.server = new MockWebServer();
		this.builder = WebClient.builder().baseUrl(this.server.url("/").toString());
		this.securityService = new ReactiveCloudFoundrySecurityService(this.builder, CLOUD_CONTROLLER, false);
	}

	@After
	public void shutdown() throws Exception {
		this.server.shutdown();
	}

	@Test
	public void getAccessLevelWhenSpaceDeveloperShouldReturnFull() throws Exception {
		String responseBody = "{\"read_sensitive_data\": true,\"read_basic_data\": true}";
		prepareResponse((response) -> response.setBody(responseBody).setHeader("Content-Type", "application/json"));
		StepVerifier.create(this.securityService.getAccessLevel("my-access-token", "my-app-id"))
				.consumeNextWith((accessLevel) -> assertThat(accessLevel).isEqualTo(AccessLevel.FULL)).expectComplete()
				.verify();
		expectRequest((request) -> {
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("bearer my-access-token");
			assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER_PERMISSIONS);
		});
	}

	@Test
	public void getAccessLevelWhenNotSpaceDeveloperShouldReturnRestricted() throws Exception {
		String responseBody = "{\"read_sensitive_data\": false,\"read_basic_data\": true}";
		prepareResponse((response) -> response.setBody(responseBody).setHeader("Content-Type", "application/json"));
		StepVerifier.create(this.securityService.getAccessLevel("my-access-token", "my-app-id"))
				.consumeNextWith((accessLevel) -> assertThat(accessLevel).isEqualTo(AccessLevel.RESTRICTED))
				.expectComplete().verify();
		expectRequest((request) -> {
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("bearer my-access-token");
			assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER_PERMISSIONS);
		});
	}

	@Test
	public void getAccessLevelWhenTokenIsNotValidShouldThrowException() throws Exception {
		prepareResponse((response) -> response.setResponseCode(401));
		StepVerifier.create(this.securityService.getAccessLevel("my-access-token", "my-app-id"))
				.consumeErrorWith((throwable) -> {
					assertThat(throwable).isInstanceOf(CloudFoundryAuthorizationException.class);
					assertThat(((CloudFoundryAuthorizationException) throwable).getReason())
							.isEqualTo(Reason.INVALID_TOKEN);
				}).verify();
		expectRequest((request) -> {
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("bearer my-access-token");
			assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER_PERMISSIONS);
		});
	}

	@Test
	public void getAccessLevelWhenForbiddenShouldThrowException() throws Exception {
		prepareResponse((response) -> response.setResponseCode(403));
		StepVerifier.create(this.securityService.getAccessLevel("my-access-token", "my-app-id"))
				.consumeErrorWith((throwable) -> {
					assertThat(throwable).isInstanceOf(CloudFoundryAuthorizationException.class);
					assertThat(((CloudFoundryAuthorizationException) throwable).getReason())
							.isEqualTo(Reason.ACCESS_DENIED);
				}).verify();
		expectRequest((request) -> {
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("bearer my-access-token");
			assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER_PERMISSIONS);
		});
	}

	@Test
	public void getAccessLevelWhenCloudControllerIsNotReachableThrowsException() throws Exception {
		prepareResponse((response) -> response.setResponseCode(500));
		StepVerifier.create(this.securityService.getAccessLevel("my-access-token", "my-app-id"))
				.consumeErrorWith((throwable) -> {
					assertThat(throwable).isInstanceOf(CloudFoundryAuthorizationException.class);
					assertThat(((CloudFoundryAuthorizationException) throwable).getReason())
							.isEqualTo(Reason.SERVICE_UNAVAILABLE);
				}).verify();
		expectRequest((request) -> {
			assertThat(request.getHeader(HttpHeaders.AUTHORIZATION)).isEqualTo("bearer my-access-token");
			assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER_PERMISSIONS);
		});
	}

	@Test
	public void fetchTokenKeysWhenSuccessfulShouldReturnListOfKeysFromUAA() throws Exception {
		String tokenKeyValue = "-----BEGIN PUBLIC KEY-----\n"
				+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0m59l2u9iDnMbrXHfqkO\n"
				+ "rn2dVQ3vfBJqcDuFUK03d+1PZGbVlNCqnkpIJ8syFppW8ljnWweP7+LiWpRoz0I7\n"
				+ "fYb3d8TjhV86Y997Fl4DBrxgM6KTJOuE/uxnoDhZQ14LgOU2ckXjOzOdTsnGMKQB\n"
				+ "LCl0vpcXBtFLMaSbpv1ozi8h7DJyVZ6EnFQZUWGdgTMhDrmqevfx95U/16c5WBDO\n"
				+ "kqwIn7Glry9n9Suxygbf8g5AzpWcusZgDLIIZ7JTUldBb8qU2a0Dl4mvLZOn4wPo\n"
				+ "jfj9Cw2QICsc5+Pwf21fP+hzf+1WSRHbnYv8uanRO0gZ8ekGaghM/2H6gqJbo2nI\n"
				+ "JwIDAQAB\n-----END PUBLIC KEY-----";
		prepareResponse((response) -> {
			response.setBody("{\"token_endpoint\":\"/my-uaa.com\"}");
			response.setHeader("Content-Type", "application/json");
		});
		String responseBody = "{\"keys\" : [ {\"kid\":\"test-key\",\"value\" : \"" + tokenKeyValue.replace("\n", "\\n")
				+ "\"} ]}";
		prepareResponse((response) -> {
			response.setBody(responseBody);
			response.setHeader("Content-Type", "application/json");
		});
		StepVerifier.create(this.securityService.fetchTokenKeys())
				.consumeNextWith((tokenKeys) -> assertThat(tokenKeys.get("test-key")).isEqualTo(tokenKeyValue))
				.expectComplete().verify();
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-cloud-controller.com/info"));
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-uaa.com/token_keys"));
	}

	@Test
	public void fetchTokenKeysWhenNoKeysReturnedFromUAA() throws Exception {
		prepareResponse((response) -> {
			response.setBody("{\"token_endpoint\":\"/my-uaa.com\"}");
			response.setHeader("Content-Type", "application/json");
		});
		String responseBody = "{\"keys\": []}";
		prepareResponse((response) -> {
			response.setBody(responseBody);
			response.setHeader("Content-Type", "application/json");
		});
		StepVerifier.create(this.securityService.fetchTokenKeys())
				.consumeNextWith((tokenKeys) -> assertThat(tokenKeys).hasSize(0)).expectComplete().verify();
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-cloud-controller.com/info"));
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-uaa.com/token_keys"));
	}

	@Test
	public void fetchTokenKeysWhenUnsuccessfulShouldThrowException() throws Exception {
		prepareResponse((response) -> {
			response.setBody("{\"token_endpoint\":\"/my-uaa.com\"}");
			response.setHeader("Content-Type", "application/json");
		});
		prepareResponse((response) -> response.setResponseCode(500));
		StepVerifier.create(this.securityService.fetchTokenKeys())
				.consumeErrorWith(
						(throwable) -> assertThat(((CloudFoundryAuthorizationException) throwable).getReason())
								.isEqualTo(Reason.SERVICE_UNAVAILABLE))
				.verify();
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-cloud-controller.com/info"));
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo("/my-uaa.com/token_keys"));
	}

	@Test
	public void getUaaUrlShouldCallCloudControllerInfoOnlyOnce() throws Exception {
		prepareResponse((response) -> {
			response.setBody("{\"token_endpoint\":\"" + UAA_URL + "\"}");
			response.setHeader("Content-Type", "application/json");
		});
		StepVerifier.create(this.securityService.getUaaUrl())
				.consumeNextWith((uaaUrl) -> assertThat(uaaUrl).isEqualTo(UAA_URL)).expectComplete().verify();
		// this.securityService.getUaaUrl().block(); //FIXME subscribe again to check that
		// it isn't called again
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER + "/info"));
		expectRequestCount(1);
	}

	@Test
	public void getUaaUrlWhenCloudControllerUrlIsNotReachableShouldThrowException() throws Exception {
		prepareResponse((response) -> response.setResponseCode(500));
		StepVerifier.create(this.securityService.getUaaUrl()).consumeErrorWith((throwable) -> {
			assertThat(throwable).isInstanceOf(CloudFoundryAuthorizationException.class);
			assertThat(((CloudFoundryAuthorizationException) throwable).getReason())
					.isEqualTo(Reason.SERVICE_UNAVAILABLE);
		}).verify();
		expectRequest((request) -> assertThat(request.getPath()).isEqualTo(CLOUD_CONTROLLER + "/info"));
	}

	private void prepareResponse(Consumer<MockResponse> consumer) {
		MockResponse response = new MockResponse();
		consumer.accept(response);
		this.server.enqueue(response);
	}

	private void expectRequest(Consumer<RecordedRequest> consumer) throws InterruptedException {
		consumer.accept(this.server.takeRequest());
	}

	private void expectRequestCount(int count) {
		assertThat(count).isEqualTo(this.server.getRequestCount());
	}

}
