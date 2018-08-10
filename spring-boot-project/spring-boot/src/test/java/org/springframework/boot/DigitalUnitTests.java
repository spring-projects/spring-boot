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

package org.springframework.boot;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DigitalUnit}.
 *
 * @author Dmytro Nosan
 */
public class DigitalUnitTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void bytesUnit() {
		assertBytesToUnit();
		assertFromUnitToBytes();
		assertCompareBytes();
	}

	@Test
	public void kilobytesUnit() {
		assertKilobytesToUnit();
		assertFromUnitToKilobytes();
		assertCompareKilobytes();

	}

	@Test
	public void megabytesUnit() {
		assertMegabytesToUnit();
		assertFromUnitToMegabytes();
		assertCompareMegabytes();
	}

	@Test
	public void gigabytesUnit() {
		assertGigabytesToUnit();
		assertUnitToGigabytes();
		assertCompareGigabytes();

	}

	@Test
	public void terabytesUnit() {
		assertTerabytesToUnit();
		assertUnitToTerabytes();
		assertCompareTerabytes();

	}

	@Test
	public void fromNameUnknown() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Unknown name 'TTT'");
		DigitalUnit.fromName("TTT");
	}

	@Test
	public void fromName() {
		assertThat(DigitalUnit.fromName("Bytes")).isEqualTo(DigitalUnit.BYTES);
		assertThat(DigitalUnit.fromName("Kilobytes")).isEqualTo(DigitalUnit.KILOBYTES);
		assertThat(DigitalUnit.fromName("Megabytes")).isEqualTo(DigitalUnit.MEGABYTES);
		assertThat(DigitalUnit.fromName("Gigabytes")).isEqualTo(DigitalUnit.GIGABYTES);
		assertThat(DigitalUnit.fromName("Terabytes")).isEqualTo(DigitalUnit.TERABYTES);
	}

	@Test
	public void fromAbbreviation() {
		assertThat(DigitalUnit.fromAbbreviation("B")).isEqualTo(DigitalUnit.BYTES);
		assertThat(DigitalUnit.fromAbbreviation("KB")).isEqualTo(DigitalUnit.KILOBYTES);
		assertThat(DigitalUnit.fromAbbreviation("MB")).isEqualTo(DigitalUnit.MEGABYTES);
		assertThat(DigitalUnit.fromAbbreviation("GB")).isEqualTo(DigitalUnit.GIGABYTES);
		assertThat(DigitalUnit.fromAbbreviation("TB")).isEqualTo(DigitalUnit.TERABYTES);
	}

	@Test
	public void fromNameAbbreviation() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Unknown abbreviation 'TTT'");
		DigitalUnit.fromAbbreviation("TTT");
	}

	private static void assertBytesToUnit() {
		assertThat(DigitalUnit.BYTES.toBytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.toKilobytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toMegabytes(1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toGigabytes(1024 * 1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toTerabytes(1024L * 1024 * 1024 * 1024))
				.isEqualTo(1);

		assertThat(DigitalUnit.BYTES.toUnit(1024, DigitalUnit.BYTES)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.toUnit(1024, DigitalUnit.KILOBYTES)).isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toUnit(1024 * 1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toUnit(1024 * 1024 * 1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.BYTES.toUnit(1024L * 1024 * 1024 * 1024,
				DigitalUnit.TERABYTES)).isEqualTo(1);
	}

	private static void assertFromUnitToBytes() {
		assertThat(DigitalUnit.BYTES.fromBytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.fromKilobytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.fromMegabytes(1)).isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.BYTES.fromGigabytes(1)).isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.BYTES.fromTerabytes(1))
				.isEqualTo(1024L * 1024 * 1024 * 1024);

		assertThat(DigitalUnit.BYTES.fromUnit(1024, DigitalUnit.BYTES)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.fromUnit(1, DigitalUnit.KILOBYTES)).isEqualTo(1024);
		assertThat(DigitalUnit.BYTES.fromUnit(1, DigitalUnit.MEGABYTES))
				.isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.BYTES.fromUnit(1, DigitalUnit.GIGABYTES))
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.BYTES.fromUnit(1, DigitalUnit.TERABYTES))
				.isEqualTo(1024L * 1024 * 1024 * 1024);
	}

	private static void assertCompareBytes() {
		assertThat(DigitalUnit.BYTES.compareTo(DigitalUnit.BYTES)).isZero();
		assertThat(DigitalUnit.BYTES.compareTo(DigitalUnit.KILOBYTES)).isLessThan(0);
		assertThat(DigitalUnit.BYTES.compareTo(DigitalUnit.MEGABYTES)).isLessThan(0);
		assertThat(DigitalUnit.BYTES.compareTo(DigitalUnit.GIGABYTES)).isLessThan(0);
		assertThat(DigitalUnit.BYTES.compareTo(DigitalUnit.TERABYTES)).isLessThan(0);

	}

	private static void assertFromUnitToKilobytes() {
		assertThat(DigitalUnit.KILOBYTES.fromBytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.KILOBYTES.fromKilobytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.fromMegabytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.fromGigabytes(1)).isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.KILOBYTES.fromTerabytes(1)).isEqualTo(1024 * 1024 * 1024);

		assertThat(DigitalUnit.KILOBYTES.fromUnit(1024, DigitalUnit.BYTES)).isEqualTo(1);
		assertThat(DigitalUnit.KILOBYTES.fromUnit(1024, DigitalUnit.KILOBYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.fromUnit(1, DigitalUnit.MEGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.fromUnit(1, DigitalUnit.GIGABYTES))
				.isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.KILOBYTES.fromUnit(1, DigitalUnit.TERABYTES))
				.isEqualTo(1024 * 1024 * 1024);
	}

	private static void assertKilobytesToUnit() {
		assertThat(DigitalUnit.KILOBYTES.toBytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.toKilobytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.toMegabytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.KILOBYTES.toGigabytes(1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.KILOBYTES.toTerabytes(1024 * 1024 * 1024)).isEqualTo(1);

		assertThat(DigitalUnit.KILOBYTES.toUnit(1, DigitalUnit.BYTES)).isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.toUnit(1024, DigitalUnit.KILOBYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.KILOBYTES.toUnit(1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.KILOBYTES.toUnit(1024 * 1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1);
		assertThat(
				DigitalUnit.KILOBYTES.toUnit(1024 * 1024 * 1024, DigitalUnit.TERABYTES))
						.isEqualTo(1);
	}

	private static void assertCompareKilobytes() {
		assertThat(DigitalUnit.KILOBYTES.compareTo(DigitalUnit.BYTES)).isGreaterThan(0);
		assertThat(DigitalUnit.KILOBYTES.compareTo(DigitalUnit.KILOBYTES)).isZero();
		assertThat(DigitalUnit.KILOBYTES.compareTo(DigitalUnit.MEGABYTES)).isLessThan(0);
		assertThat(DigitalUnit.KILOBYTES.compareTo(DigitalUnit.GIGABYTES)).isLessThan(0);
		assertThat(DigitalUnit.KILOBYTES.compareTo(DigitalUnit.TERABYTES)).isLessThan(0);
	}

	private void assertFromUnitToMegabytes() {
		assertThat(DigitalUnit.MEGABYTES.fromBytes(1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.fromKilobytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.fromMegabytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.fromGigabytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.fromTerabytes(1)).isEqualTo(1024 * 1024);

		assertThat(DigitalUnit.MEGABYTES.fromUnit(1024 * 1024, DigitalUnit.BYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.fromUnit(1024, DigitalUnit.KILOBYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.fromUnit(1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.fromUnit(1, DigitalUnit.GIGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.fromUnit(1, DigitalUnit.TERABYTES))
				.isEqualTo(1024 * 1024);
	}

	private static void assertMegabytesToUnit() {
		assertThat(DigitalUnit.MEGABYTES.toBytes(1)).isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.MEGABYTES.toKilobytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.toMegabytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.toGigabytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.toTerabytes(1024 * 1024)).isEqualTo(1);

		assertThat(DigitalUnit.MEGABYTES.toUnit(1, DigitalUnit.BYTES))
				.isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.MEGABYTES.toUnit(1, DigitalUnit.KILOBYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.toUnit(1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.MEGABYTES.toUnit(1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.MEGABYTES.toUnit(1024 * 1024, DigitalUnit.TERABYTES))
				.isEqualTo(1);
	}

	private static void assertCompareMegabytes() {
		assertThat(DigitalUnit.MEGABYTES.compareTo(DigitalUnit.BYTES)).isGreaterThan(0);
		assertThat(DigitalUnit.MEGABYTES.compareTo(DigitalUnit.KILOBYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.MEGABYTES.compareTo(DigitalUnit.MEGABYTES)).isZero();
		assertThat(DigitalUnit.MEGABYTES.compareTo(DigitalUnit.GIGABYTES)).isLessThan(0);
		assertThat(DigitalUnit.MEGABYTES.compareTo(DigitalUnit.TERABYTES)).isLessThan(0);
	}

	private static void assertUnitToGigabytes() {
		assertThat(DigitalUnit.GIGABYTES.fromBytes(1024 * 1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromKilobytes(1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromMegabytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromGigabytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.fromTerabytes(1)).isEqualTo(1024);

		assertThat(DigitalUnit.GIGABYTES.fromUnit(1024 * 1024 * 1024, DigitalUnit.BYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromUnit(1024 * 1024, DigitalUnit.KILOBYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromUnit(1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.GIGABYTES.fromUnit(1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.fromUnit(1, DigitalUnit.TERABYTES))
				.isEqualTo(1024);
	}

	private static void assertGigabytesToUnit() {
		assertThat(DigitalUnit.GIGABYTES.toBytes(1)).isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.GIGABYTES.toKilobytes(1)).isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.GIGABYTES.toMegabytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.toGigabytes(1024)).isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.toTerabytes(1024)).isEqualTo(1);

		assertThat(DigitalUnit.GIGABYTES.toUnit(1, DigitalUnit.BYTES))
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.GIGABYTES.toUnit(1, DigitalUnit.KILOBYTES))
				.isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.GIGABYTES.toUnit(1, DigitalUnit.MEGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.toUnit(1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.GIGABYTES.toUnit(1024, DigitalUnit.TERABYTES))
				.isEqualTo(1);
	}

	private static void assertCompareGigabytes() {
		assertThat(DigitalUnit.GIGABYTES.compareTo(DigitalUnit.BYTES)).isGreaterThan(0);
		assertThat(DigitalUnit.GIGABYTES.compareTo(DigitalUnit.KILOBYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.GIGABYTES.compareTo(DigitalUnit.MEGABYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.GIGABYTES.compareTo(DigitalUnit.GIGABYTES)).isZero();
		assertThat(DigitalUnit.GIGABYTES.compareTo(DigitalUnit.TERABYTES)).isLessThan(0);
	}

	private static void assertUnitToTerabytes() {
		assertThat(DigitalUnit.TERABYTES.fromBytes(1024L * 1024 * 1024 * 1024))
				.isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromKilobytes(1024 * 1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromMegabytes(1024 * 1024)).isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromGigabytes(1024)).isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromTerabytes(1024)).isEqualTo(1024);

		assertThat(DigitalUnit.TERABYTES.fromUnit(1024L * 1024 * 1024 * 1024,
				DigitalUnit.BYTES)).isEqualTo(1);
		assertThat(
				DigitalUnit.TERABYTES.fromUnit(1024 * 1024 * 1024, DigitalUnit.KILOBYTES))
						.isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromUnit(1024 * 1024, DigitalUnit.MEGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromUnit(1024, DigitalUnit.GIGABYTES))
				.isEqualTo(1);
		assertThat(DigitalUnit.TERABYTES.fromUnit(1024, DigitalUnit.TERABYTES))
				.isEqualTo(1024);
	}

	private static void assertTerabytesToUnit() {
		assertThat(DigitalUnit.TERABYTES.toBytes(1))
				.isEqualTo(1024L * 1024 * 1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toKilobytes(1)).isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toMegabytes(1)).isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toGigabytes(1)).isEqualTo(1024);
		assertThat(DigitalUnit.TERABYTES.toTerabytes(1024)).isEqualTo(1024);

		assertThat(DigitalUnit.TERABYTES.toUnit(1, DigitalUnit.BYTES))
				.isEqualTo(1024L * 1024 * 1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toUnit(1, DigitalUnit.KILOBYTES))
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toUnit(1, DigitalUnit.MEGABYTES))
				.isEqualTo(1024 * 1024);
		assertThat(DigitalUnit.TERABYTES.toUnit(1, DigitalUnit.GIGABYTES))
				.isEqualTo(1024);
		assertThat(DigitalUnit.TERABYTES.toUnit(1024, DigitalUnit.TERABYTES))
				.isEqualTo(1024);
	}

	private static void assertCompareTerabytes() {
		assertThat(DigitalUnit.TERABYTES.compareTo(DigitalUnit.BYTES)).isGreaterThan(0);
		assertThat(DigitalUnit.TERABYTES.compareTo(DigitalUnit.KILOBYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.TERABYTES.compareTo(DigitalUnit.MEGABYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.TERABYTES.compareTo(DigitalUnit.GIGABYTES))
				.isGreaterThan(0);
		assertThat(DigitalUnit.TERABYTES.compareTo(DigitalUnit.TERABYTES)).isZero();
	}

}
