/*
 * Copyright 2012-2019 the original author or authors.
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

import smoketest.test.domain.User;
import smoketest.test.domain.UserRepository;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleDetailsService;
import smoketest.test.service.VehicleIdentificationNumberNotFoundException;

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

	/**
     * Constructs a new UserVehicleService with the specified UserRepository and VehicleDetailsService.
     * 
     * @param userRepository the UserRepository to be used by the service
     * @param vehicleDetailsService the VehicleDetailsService to be used by the service
     */
    public UserVehicleService(UserRepository userRepository, VehicleDetailsService vehicleDetailsService) {
		this.userRepository = userRepository;
		this.vehicleDetailsService = vehicleDetailsService;
	}

	/**
     * Retrieves the vehicle details for a given username.
     * 
     * @param username the username of the user
     * @return the vehicle details for the user
     * @throws UserNameNotFoundException if the username is not found in the user repository
     * @throws VehicleIdentificationNumberNotFoundException if the vehicle identification number (VIN) is not found for the user
     * @throws IllegalArgumentException if the username is null
     */
    public VehicleDetails getVehicleDetails(String username)
			throws UserNameNotFoundException, VehicleIdentificationNumberNotFoundException {
		Assert.notNull(username, "Username must not be null");
		User user = this.userRepository.findByUsername(username);
		if (user == null) {
			throw new UserNameNotFoundException(username);
		}
		return this.vehicleDetailsService.getVehicleDetails(user.getVin());
	}

}
