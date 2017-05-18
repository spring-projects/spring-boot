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

package sample.test.web;

import sample.test.domain.User;
import sample.test.domain.UserRepository;
import sample.test.service.VehicleDetails;
import sample.test.service.VehicleDetailsService;
import sample.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Controller service used to provide vehicle information for a given user.
 *
 * @author Phillip Webb
 */
@Component
public class UserVehicleService {

	private final UserRepository userRepository;

	private final VehicleDetailsService vehicleDetailsService;

	public UserVehicleService(UserRepository userRepository,
			VehicleDetailsService vehicleDetailsService) {
		this.userRepository = userRepository;
		this.vehicleDetailsService = vehicleDetailsService;
	}

	public VehicleDetails getVehicleDetails(String username)
			throws UserNameNotFoundException,
			VehicleIdentificationNumberNotFoundException {
		Assert.notNull(username, "Username must not be null");
		User user = this.userRepository.findByUsername(username);
		if (user == null) {
			throw new UserNameNotFoundException(username);
		}
		return this.vehicleDetailsService.getVehicleDetails(user.getVin());
	}

}
