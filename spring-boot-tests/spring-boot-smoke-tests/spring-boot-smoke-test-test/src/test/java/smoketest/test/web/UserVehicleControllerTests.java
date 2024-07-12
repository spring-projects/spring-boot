/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.test.web;

import org.junit.jupiter.api.Test;
import smoketest.test.WelcomeCommandLineRunner;
import smoketest.test.domain.VehicleIdentificationNumber;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * {@code @WebMvcTest} based tests for {@link UserVehicleController}.
 *
 * @author Phillip Webb
 */
@WebMvcTest(UserVehicleController.class)
class UserVehicleControllerTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber("00000000000000000");

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
	void getVehicleWhenRequestingJsonShouldReturnMakeAndModel() {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get().uri("/sboot/vehicle").accept(MediaType.APPLICATION_JSON)).hasStatusOk()
			.bodyJson()
			.isLenientlyEqualTo("{'make':'Honda','model':'Civic'}");
	}

	@Test
	void getVehicleWhenRequestingHtmlShouldReturnMakeAndModel() {
		given(this.userVehicleService.getVehicleDetails("sboot")).willReturn(new VehicleDetails("Honda", "Civic"));
		assertThat(this.mvc.get().uri("/sboot/vehicle.html").accept(MediaType.TEXT_HTML)).hasStatusOk()
			.bodyText()
			.contains("<h1>Honda Civic</h1>");
	}

	@Test
	void getVehicleWhenUserNotFoundShouldReturnNotFound() {
		given(this.userVehicleService.getVehicleDetails("sboot")).willThrow(new UserNameNotFoundException("sboot"));
		assertThat(this.mvc.get().uri("/sboot/vehicle")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void getVehicleWhenVinNotFoundShouldReturnNotFound() {
		given(this.userVehicleService.getVehicleDetails("sboot"))
			.willThrow(new VehicleIdentificationNumberNotFoundException(VIN));
		assertThat(this.mvc.get().uri("/sboot/vehicle")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void welcomeCommandLineRunnerShouldNotBeAvailable() {
		// Since we're a @WebMvcTest WelcomeCommandLineRunner should not be available.
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(WelcomeCommandLineRunner.class));
	}

}
