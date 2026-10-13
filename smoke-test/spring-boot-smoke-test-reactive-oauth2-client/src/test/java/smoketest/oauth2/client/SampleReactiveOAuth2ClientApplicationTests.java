/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.oauth2.client;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "APP-CLIENT-ID=my-client-id", "APP-CLIENT-SECRET=my-client-secret",
				"YAHOO-CLIENT-ID=my-google-client-id", "YAHOO-CLIENT-SECRET=my-google-client-secret" })
@AutoConfigureWebTestClient
class SampleReactiveOAuth2ClientApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void everythingShouldRedirectToLogin() {
		this.webTestClient.get()
			.uri("/")
			.exchange()
			.expectStatus()
			.isFound()
			.expectHeader()
			.valueEquals("Location", "/login");
	}

	@Test
	void loginShouldHaveBothOAuthClientsToChooseFrom() {
		byte[] body = this.webTestClient.get()
			.uri("/login")
			.exchange()
			.expectStatus()
			.isOk()
			.returnResult(String.class)
			.getResponseBodyContent();
		String bodyString = new String(body);
		assertThat(bodyString).contains("/oauth2/authorization/yahoo");
		assertThat(bodyString).contains("/oauth2/authorization/github-client-1");
		assertThat(bodyString).contains("/oauth2/authorization/github-client-2");
	}

	@Test
	void actuatorShouldBeSecuredByOAuth() {
		this.webTestClient.get()
			.uri("/actuator/health")
			.exchange()
			.expectStatus()
			.isFound()
			.expectHeader()
			.valueEquals("Location", "/login");
	}

}
