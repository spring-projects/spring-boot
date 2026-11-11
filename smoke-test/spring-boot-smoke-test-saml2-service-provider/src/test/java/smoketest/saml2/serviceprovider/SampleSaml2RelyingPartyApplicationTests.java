/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.saml2.serviceprovider;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.http.client.HttpRedirects;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleSaml2RelyingPartyApplicationTests {

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
	void everythingShouldRedirectToLogin() {
		RestTestClient.ResponseSpec response = nonFollowingRedirect().get().uri("/").exchange();
		response.expectStatus().isFound();
		response.expectHeader().location("/login");
	}

	@Test
	void loginShouldHaveAllAssertingPartiesToChooseFrom() {
		this.rest.get()
			.uri("/login")
			.exchangeSuccessfully()
			.expectBody(String.class)
			.value((body) -> assertThat(body).contains("/saml2/authenticate?registrationId=simplesamlphp")
				.contains("/saml2/authenticate?registrationId=okta"));
	}

}
