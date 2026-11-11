/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import smoketest.test.domain.VehicleIdentificationNumber;

/**
 * A service to obtain {@link VehicleDetails} given a {@link VehicleIdentificationNumber}.
 *
 * @author Phillip Webb
 */
public interface VehicleDetailsService {

	/**
	 * Get vehicle details for a given {@link VehicleIdentificationNumber}.
	 * @param vin the vehicle identification number
	 * @return vehicle details
	 * @throws VehicleIdentificationNumberNotFoundException if the VIN is not known
	 */
	VehicleDetails getVehicleDetails(VehicleIdentificationNumber vin)
			throws VehicleIdentificationNumberNotFoundException;

}
