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

package smoketest.test.domain;

import org.springframework.util.Assert;

/**
 * A Vehicle Identification Number.
 *
 * @author Phillip Webb
 */
public final class VehicleIdentificationNumber {

	private String vin;

	public VehicleIdentificationNumber(String vin) {
		Assert.notNull(vin, "VIN must not be null");
		Assert.isTrue(vin.length() == 17, "VIN must be exactly 17 characters");
		this.vin = vin;
	}

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

	@Override
	public int hashCode() {
		return this.vin.hashCode();
	}

	@Override
	public String toString() {
		return this.vin;
	}

}
