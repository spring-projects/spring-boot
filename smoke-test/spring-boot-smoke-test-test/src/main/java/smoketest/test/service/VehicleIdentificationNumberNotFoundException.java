/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.service;

import org.jspecify.annotations.Nullable;
import smoketest.test.domain.VehicleIdentificationNumber;

/**
 * Exception thrown when a {@link VehicleIdentificationNumber} is not found.
 *
 * @author Phillip Webb
 */
public class VehicleIdentificationNumberNotFoundException extends RuntimeException {

	private final VehicleIdentificationNumber vehicleIdentificationNumber;

	public VehicleIdentificationNumberNotFoundException(VehicleIdentificationNumber vin) {
		this(vin, null);
	}

	public VehicleIdentificationNumberNotFoundException(VehicleIdentificationNumber vin, @Nullable Throwable cause) {
		super("Unable to find VehicleIdentificationNumber " + vin, cause);
		this.vehicleIdentificationNumber = vin;
	}

	public VehicleIdentificationNumber getVehicleIdentificationNumber() {
		return this.vehicleIdentificationNumber;
	}

}
