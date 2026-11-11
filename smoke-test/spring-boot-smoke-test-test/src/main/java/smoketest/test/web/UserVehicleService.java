/*
 * Copyright 2012-present the original author or authors.
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

	public UserVehicleService(UserRepository userRepository, VehicleDetailsService vehicleDetailsService) {
		this.userRepository = userRepository;
		this.vehicleDetailsService = vehicleDetailsService;
	}

	public VehicleDetails getVehicleDetails(String username)
			throws UserNameNotFoundException, VehicleIdentificationNumberNotFoundException {
		Assert.notNull(username, "'username' must not be null");
		User user = this.userRepository.findByUsername(username);
		if (user == null) {
			throw new UserNameNotFoundException(username);
		}
		return this.vehicleDetailsService.getVehicleDetails(user.getVin());
	}

}
