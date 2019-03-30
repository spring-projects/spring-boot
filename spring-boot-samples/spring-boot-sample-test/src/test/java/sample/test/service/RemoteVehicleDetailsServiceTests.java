/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.test.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import sample.test.domain.VehicleIdentificationNumber;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RemoteVehicleDetailsService}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringRunner.class)
@RestClientTest({ RemoteVehicleDetailsService.class, ServiceProperties.class })
public class RemoteVehicleDetailsServiceTests {

	private static final String VIN = "00000000000000000";

	@Autowired
	private RemoteVehicleDetailsService service;

	@Autowired
	private MockRestServiceServer server;

	@Test
	public void getVehicleDetailsWhenVinIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.getVehicleDetails(null))
				.withMessage("VIN must not be null");
	}

	@Test
	public void getVehicleDetailsWhenResultIsSuccessShouldReturnDetails() {
		this.server.expect(requestTo("/vehicle/" + VIN + "/details"))
				.andRespond(withSuccess(getClassPathResource("vehicledetails.json"),
						MediaType.APPLICATION_JSON));
		VehicleDetails details = this.service
				.getVehicleDetails(new VehicleIdentificationNumber(VIN));
		assertThat(details.getMake()).isEqualTo("Honda");
		assertThat(details.getModel()).isEqualTo("Civic");
	}

	@Test
	public void getVehicleDetailsWhenResultIsNotFoundShouldThrowException() {
		this.server.expect(requestTo("/vehicle/" + VIN + "/details"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		assertThatExceptionOfType(VehicleIdentificationNumberNotFoundException.class)
				.isThrownBy(() -> this.service
						.getVehicleDetails(new VehicleIdentificationNumber(VIN)));
	}

	@Test
	public void getVehicleDetailsWhenResultIServerErrorShouldThrowException() {
		this.server.expect(requestTo("/vehicle/" + VIN + "/details"))
				.andRespond(withServerError());
		assertThatExceptionOfType(HttpServerErrorException.class)
				.isThrownBy(() -> this.service
						.getVehicleDetails(new VehicleIdentificationNumber(VIN)));
	}

	private ClassPathResource getClassPathResource(String path) {
		return new ClassPathResource(path, getClass());
	}

}
