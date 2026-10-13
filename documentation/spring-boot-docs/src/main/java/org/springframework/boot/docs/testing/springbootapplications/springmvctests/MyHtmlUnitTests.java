/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.testing.springbootapplications.springmvctests;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@WebMvcTest(UserVehicleController.class)
class MyHtmlUnitTests {

	@Autowired
	private WebClient webClient;

	@MockitoBean
	private UserVehicleService userVehicleService;

	@Test
	void testExample() throws Exception {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		HtmlPage page = this.webClient.getPage("/sboot/vehicle.html");
		assertThat(page.getBody().getTextContent()).isEqualTo("Honda Civic");
	}

}
