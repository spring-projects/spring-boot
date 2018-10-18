/*
 * Copyright 2012-2018 the original author or authors.
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

package sample.test.web;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import sample.test.domain.User;
import sample.test.domain.UserRepository;
import sample.test.domain.VehicleIdentificationNumber;
import sample.test.service.VehicleDetails;
import sample.test.service.VehicleDetailsService;

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
public class UserVehicleServiceTests {

	private static final VehicleIdentificationNumber VIN = new VehicleIdentificationNumber(
			"00000000000000000");

	@Mock
	private VehicleDetailsService vehicleDetailsService;

	@Mock
	private UserRepository userRepository;

	private UserVehicleService service;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		this.service = new UserVehicleService(this.userRepository,
				this.vehicleDetailsService);
	}

	@Test
	public void getVehicleDetailsWhenUsernameIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.service.getVehicleDetails(null))
				.withMessage("Username must not be null");
	}

	@Test
	public void getVehicleDetailsWhenUsernameNotFoundShouldThrowException() {
		given(this.userRepository.findByUsername(anyString())).willReturn(null);
		assertThatExceptionOfType(UserNameNotFoundException.class)
				.isThrownBy(() -> this.service.getVehicleDetails("sboot"));
	}

	@Test
	public void getVehicleDetailsShouldReturnMakeAndModel() {
		given(this.userRepository.findByUsername(anyString()))
				.willReturn(new User("sboot", VIN));
		VehicleDetails details = new VehicleDetails("Honda", "Civic");
		given(this.vehicleDetailsService.getVehicleDetails(VIN)).willReturn(details);
		VehicleDetails actual = this.service.getVehicleDetails("sboot");
		assertThat(actual).isEqualTo(details);
	}

}
