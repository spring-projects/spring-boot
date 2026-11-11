/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.web;

import org.junit.jupiter.api.Test;
import smoketest.test.WelcomeCommandLineRunner;
import smoketest.test.service.VehicleDetails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@code @SpringBootTest} based tests for {@link UserVehicleController}.
 *
 * @author Phillip Webb
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
class UserVehicleControllerApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private ApplicationContext applicationContext;

	@MockitoBean
	private UserVehicleService userVehicleService;

	@Test
	void getVehicleWhenRequestingTextShouldReturnMakeAndModel() {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get().uri("/sboot/vehicle").accept(MediaType.TEXT_PLAIN)).hasStatusOk()
			.hasBodyTextEqualTo("Honda Civic");
	}

	@Test
	void welcomeCommandLineRunnerShouldBeAvailable() {
		// Since we're a @SpringBootTest all beans should be available.
		assertThat(this.applicationContext.getBean(WelcomeCommandLineRunner.class)).isNotNull();
	}

}
