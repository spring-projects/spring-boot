/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.test.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import sample.test.domain.VehicleIdentificationNumber;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link RemoteVehicleDetailsService}.
 *
 * @author Phillip Webb
 */
public class RemoteVehicleDetailsServiceTests {

	private static final String VIN = "00000000000000000";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private RemoteVehicleDetailsService service;

	private MockRestServiceServer server;

	@Before
	public void setup() {
		ServiceProperties properties = new ServiceProperties();
		properties.setVehicleServiceRootUrl("http://example.com/");
		this.service = new RemoteVehicleDetailsService(properties);
		this.server = MockRestServiceServer.createServer(this.service.getRestTemplate());
	}

	@Test
	public void getVehicleDetailsWhenVinIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("VIN must not be null");
		this.service.getVehicleDetails(null);
	}

	@Test
	public void getVehicleDetailsWhenResultIsSuccessShouldReturnDetails()
			throws Exception {
		this.server.expect(requestTo("http://example.com/vehicle/" + VIN + "/details"))
				.andRespond(withSuccess(getClassPathResource("vehicledetails.json"),
						MediaType.APPLICATION_JSON));
		VehicleDetails details = this.service
				.getVehicleDetails(new VehicleIdentificationNumber(VIN));
		assertThat(details.getMake()).isEqualTo("Honda");
		assertThat(details.getModel()).isEqualTo("Civic");
	}

	@Test
	public void getVehicleDetailsWhenResultIsNotFoundShouldThrowException()
			throws Exception {
		this.server.expect(requestTo("http://example.com/vehicle/" + VIN + "/details"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		this.thrown.expect(VehicleIdentificationNumberNotFoundException.class);
		this.service.getVehicleDetails(new VehicleIdentificationNumber(VIN));
	}

	@Test
	public void getVehicleDetailsWhenResultIServerErrorShouldThrowException()
			throws Exception {
		this.server.expect(requestTo("http://example.com/vehicle/" + VIN + "/details"))
				.andRespond(withServerError());
		this.thrown.expect(HttpServerErrorException.class);
		this.service.getVehicleDetails(new VehicleIdentificationNumber(VIN));
	}

	private ClassPathResource getClassPathResource(String path) {
		return new ClassPathResource(path, getClass());
	}

}
