/*
 * Copyright 2012-2022 the original author or authors.
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

package smoketest.test.domain;

import org.springframework.util.Assert;

/**
 * A Vehicle Identification Number.
 *
 * @author Phillip Webb
 */
public final class VehicleIdentificationNumber {

	private final String vin;

	/**
	 * Constructs a new VehicleIdentificationNumber object with the specified VIN.
	 * @param vin the Vehicle Identification Number to be assigned to the object
	 * @throws IllegalArgumentException if the VIN is null or not exactly 17 characters
	 * long
	 */
	public VehicleIdentificationNumber(String vin) {
		Assert.notNull(vin, "VIN must not be null");
		Assert.isTrue(vin.length() == 17, "VIN must be exactly 17 characters");
		this.vin = vin;
	}

	/**
	 * Compares this VehicleIdentificationNumber object to the specified object. The
	 * result is true if and only if the argument is not null and is a
	 * VehicleIdentificationNumber object that represents the same VIN as this object.
	 * @param obj the object to compare this VehicleIdentificationNumber against
	 * @return true if the given object represents a VehicleIdentificationNumber
	 * equivalent to this VehicleIdentificationNumber, false otherwise
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		return this.vin.equals(((VehicleIdentificationNumber) obj).vin);
	}

	/**
	 * Returns the hash code value for the VehicleIdentificationNumber object.
	 * @return the hash code value for the VehicleIdentificationNumber object
	 */
	@Override
	public int hashCode() {
		return this.vin.hashCode();
	}

	/**
	 * Returns the string representation of the VehicleIdentificationNumber object.
	 * @return the VIN (Vehicle Identification Number) as a string
	 */
	@Override
	public String toString() {
		return this.vin;
	}

}
