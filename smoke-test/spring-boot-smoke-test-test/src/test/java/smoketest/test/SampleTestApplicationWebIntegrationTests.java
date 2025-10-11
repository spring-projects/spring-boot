/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import smoketest.test.domain.VehicleIdentificationNumber;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleDetailsService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * {@code @SpringBootTest} with a random port for {@link SampleTestApplication}.
 *
 * @author Phillip Webb
 * @author Jorge Cordoba
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase
@AutoConfigureTestRestTemplate
class SampleTestApplicationWebIntegrationTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber("01234567890123456");

	@Autowired
	private TestRestTemplate restTemplate;

	@MockitoBean
	private VehicleDetailsService vehicleDetailsService;

	@BeforeEach
	void setup() {
		given(this.vehicleDetailsService.getVehicleDetails(VIN)).willReturn(new VehicleDetails("Honda", "Civic"));
	}

	@Test
	void test() {
		assertThat(this.restTemplate.getForEntity("/{username}/vehicle", String.class, "sframework").getStatusCode())
			.isEqualTo(HttpStatus.OK);
	}

}
