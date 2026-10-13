/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.springmvctests;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(UserVehicleController.class)
class MyControllerTests {

	@Autowired
	private MockMvcTester mvc;

	@MockitoBean
	private UserVehicleService userVehicleService;

	@Test
	void testExample() {
		// @formatter:off
		given(this.userVehicleService.getVehicleDetails("sboot"))
			.willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get().uri("/sboot/vehicle").accept(MediaType.TEXT_PLAIN))
			.hasStatusOk()
			.hasBodyTextEqualTo("Honda Civic");
		// @formatter:on
	}

}
