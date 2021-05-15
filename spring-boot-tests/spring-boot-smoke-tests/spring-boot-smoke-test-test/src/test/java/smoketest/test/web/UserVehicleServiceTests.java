/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import smoketest.test.domain.User;
import smoketest.test.domain.UserRepository;
import smoketest.test.domain.VehicleIdentificationNumber;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link UserVehicleService}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class UserVehicleServiceTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber("00000000000000000");

	@Mock
	private VehicleDetailsService vehicleDetailsService;

	@Mock
	private UserRepository userRepository;

	private UserVehicleService service;

	@BeforeEach
	void setup() {
		this.service = new UserVehicleService(this.userRepository, this.vehicleDetailsService);
	}

	@Test
	void getVehicleDetailsWhenUsernameIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.service.getVehicleDetails(null))
				.withMessage("Username must not be null");
	}

	@Test
	void getVehicleDetailsWhenUsernameNotFoundShouldThrowException() {
		given(this.userRepository.findByUsername(anyString())).willReturn(null);
		assertThatExceptionOfType(UserNameNotFoundException.class)
				.isThrownBy(() -> this.service.getVehicleDetails("sboot"));
	}

	@Test
	void getVehicleDetailsShouldReturnMakeAndModel() {
		given(this.userRepository.findByUsername(anyString())).willReturn(new User("sboot", VIN));
		VehicleDetails details = new VehicleDetails("Honda", "Civic");
		given(this.vehicleDetailsService.getVehicleDetails(VIN)).willReturn(details);
		VehicleDetails actual = this.service.getVehicleDetails("sboot");
		assertThat(actual).isEqualTo(details);
	}

}
