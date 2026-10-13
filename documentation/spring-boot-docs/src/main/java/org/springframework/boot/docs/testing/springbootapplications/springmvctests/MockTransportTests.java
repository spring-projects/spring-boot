/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.springmvctests;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.AutoConfigureRestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.BDDMockito.given;

@WebMvcTest(UserVehicleController.class)
@AutoConfigureRestClient
class MockTransportTests {

	@Autowired
	private RestTestClient restClient;

	@MockitoBean
	private UserVehicleService userVehicleService;

	@Test
	void testExample() {
		// @formatter:off
		given(this.userVehicleService.getVehicleDetails("sboot"))
			.willReturn(new VehicleDetails("Honda", "Civic"));
		this.restClient.get().uri("/sboot/vehicle").accept(MediaType.TEXT_PLAIN)
				.exchangeSuccessfully()
				.expectBody(String.class)
				.isEqualTo("Honda Civic");
		// @formatter:on
	}

}
