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

package sample.test.domain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link VehicleIdentificationNumber}.
 *
 * @author Phillip Webb
 * @see <a href="http://osherove.com/blog/2005/4/3/naming-standards-for-unit-tests.html">
 * Naming standards for unit tests</a>
 * @see <a href="http://joel-costigliola.github.io/assertj/">AssertJ</a>
 */
public class VehicleIdentificationNumberTests {

	private static final String SAMPLE_VIN = "41549485710496749";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenVinIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("VIN must not be null");
		new VehicleIdentificationNumber(null);
	}

	@Test
	public void createWhenVinIsMoreThan17CharsShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("VIN must be exactly 17 characters");
		new VehicleIdentificationNumber("012345678901234567");
	}

	@Test
	public void createWhenVinIsLessThan17CharsShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("VIN must be exactly 17 characters");
		new VehicleIdentificationNumber("0123456789012345");
	}

	@Test
	public void toStringShouldReturnVin() throws Exception {
		VehicleIdentificationNumber vin = new VehicleIdentificationNumber(SAMPLE_VIN);
		assertThat(vin.toString()).isEqualTo(SAMPLE_VIN);
	}

	@Test
	public void equalsAndHashCodeShouldBeBasedOnVin() throws Exception {
		VehicleIdentificationNumber vin1 = new VehicleIdentificationNumber(SAMPLE_VIN);
		VehicleIdentificationNumber vin2 = new VehicleIdentificationNumber(SAMPLE_VIN);
		VehicleIdentificationNumber vin3 = new VehicleIdentificationNumber(
				"00000000000000000");
		assertThat(vin1.hashCode()).isEqualTo(vin2.hashCode());
		assertThat(vin1).isEqualTo(vin1).isEqualTo(vin2).isNotEqualTo(vin3);
	}

}
