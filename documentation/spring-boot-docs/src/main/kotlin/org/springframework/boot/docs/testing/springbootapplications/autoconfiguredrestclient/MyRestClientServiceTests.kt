/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.autoconfiguredrestclient

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers
import org.springframework.test.web.client.response.MockRestResponseCreators

@RestClientTest(RemoteVehicleDetailsService::class)
class MyRestClientServiceTests(
	@Autowired val service: RemoteVehicleDetailsService,
	@Autowired val server: MockRestServiceServer) {

	@Test
	fun getVehicleDetailsWhenResultIsSuccessShouldReturnDetails() {
		server.expect(MockRestRequestMatchers.requestTo("https://example.com/greet/details"))
			.andRespond(MockRestResponseCreators.withSuccess("hello", MediaType.TEXT_PLAIN))
		val greeting = service.callRestService()
		assertThat(greeting).isEqualTo("hello")
	}

}

