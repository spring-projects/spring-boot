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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DigitalAmount}.
 *
 * @author Dmytro Nosan
 */
public class DigitalAmountTests {

	@Test
	public void isNegative() {
		DigitalAmount digitalAmount = DigitalAmount.fromMegabytes(-10);
		assertThat(digitalAmount.isZero()).isFalse();
		assertThat(digitalAmount.isNegative()).isTrue();
		assertThat(digitalAmount.isPositive()).isFalse();
	}

	@Test
	public void isPositive() {
		DigitalAmount digitalAmount = DigitalAmount.fromMegabytes(10);
		assertThat(digitalAmount.isZero()).isFalse();
		assertThat(digitalAmount.isNegative()).isFalse();
		assertThat(digitalAmount.isPositive()).isTrue();
	}

	@Test
	public void isZero() {
		DigitalAmount digitalAmount = DigitalAmount.fromMegabytes(0);
		assertThat(digitalAmount.isZero()).isTrue();
		assertThat(digitalAmount.isNegative()).isFalse();
		assertThat(digitalAmount.isPositive()).isFalse();
	}

	@Test
	public void filter() {
		assertThat(
				DigitalAmount.fromMegabytes(0).filter(DigitalAmount::isZero).isPresent())
						.isTrue();

		assertThat(DigitalAmount.fromMegabytes(0).filter(DigitalAmount::isPositive)
				.isPresent()).isFalse();
	}

	@Test
	public void map() {
		assertThat(DigitalAmount.fromMegabytes(1).map(DigitalAmount::toKilobytes))
				.isEqualTo(1024);
	}

	@Test
	public void amountHashCode() {
		assertThat(DigitalAmount.fromGigabytes(1024).hashCode())
				.isEqualTo(DigitalAmount.fromTerabytes(1).hashCode());
	}

	@Test
	public void compareTo() {
		assertThat(DigitalAmount.fromMegabytes(1)
				.compareTo(DigitalAmount.fromKilobytes(1024))).isZero();

		assertThat(
				DigitalAmount.fromMegabytes(1).compareTo(DigitalAmount.fromBytes(1024)))
						.isGreaterThan(0);

		assertThat(
				DigitalAmount.fromMegabytes(1).compareTo(DigitalAmount.fromGigabytes(1)))
						.isLessThan(0);
	}

	@Test
	public void equalsAmount() {

		assertThat(
				DigitalAmount.fromMegabytes(1).equals(DigitalAmount.fromKilobytes(1024)))
						.isTrue();

		assertThat(DigitalAmount.fromMegabytes(1).equals(DigitalAmount.fromMegabytes(1)))
				.isTrue();

		assertThat(DigitalAmount.fromMegabytes(1).equals(DigitalAmount.fromGigabytes(1)))
				.isFalse();
	}

	@Test
	public void toStringAmount() {
		assertThat(DigitalAmount.fromMegabytes(10).toString()).isEqualTo("10485760B");
	}

	@Test
	public void print() {
		assertThat(DigitalAmount.fromBytes(10).print(DigitalUnit.BYTES)).isEqualTo("10B");

		assertThat(DigitalAmount.fromKilobytes(10).print(DigitalUnit.BYTES))
				.isEqualTo("10240B");

		assertThat(DigitalAmount.fromMegabytes(10).print(DigitalUnit.KILOBYTES))
				.isEqualTo("10240KB");
	}

	@Test
	public void parse() {
		assertThat(DigitalAmount.parse("10")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(DigitalAmount.parse("10 B")).isEqualTo(DigitalAmount.fromBytes(10));
		assertThat(DigitalAmount.parse("10 KB"))
				.isEqualTo(DigitalAmount.fromKilobytes(10));
		assertThat(DigitalAmount.parse("10 MB"))
				.isEqualTo(DigitalAmount.fromMegabytes(10));
		assertThat(DigitalAmount.parse("10 GB"))
				.isEqualTo(DigitalAmount.fromGigabytes(10));
		assertThat(DigitalAmount.parse("-10 TB"))
				.isEqualTo(DigitalAmount.fromTerabytes(-10));
	}

	@Test
	public void toBytes() {
		assertThat(DigitalAmount.fromTerabytes(1).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromGigabytes(1).toBytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1).toBytes()).isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromKilobytes(1).toBytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromBytes(1).toBytes()).isEqualTo(1);
	}

	@Test
	public void toKilobytes() {
		assertThat(DigitalAmount.fromTerabytes(1).toKilobytes())
				.isEqualTo(1024L * 1024 * 1024);
		assertThat(DigitalAmount.fromGigabytes(1).toKilobytes()).isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1).toKilobytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromKilobytes(1).toKilobytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromBytes(1).toKilobytes()).isEqualTo(0);
	}

	@Test
	public void toMegabytes() {

		assertThat(DigitalAmount.fromTerabytes(1).toMegabytes()).isEqualTo(1024L * 1024);
		assertThat(DigitalAmount.fromGigabytes(1).toMegabytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromMegabytes(1).toMegabytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromKilobytes(1).toMegabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromBytes(1).toMegabytes()).isEqualTo(0);
	}

	@Test
	public void toGigabytes() {

		assertThat(DigitalAmount.fromTerabytes(1).toGigabytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromGigabytes(1).toGigabytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromMegabytes(1).toGigabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromKilobytes(1).toGigabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromBytes(1).toGigabytes()).isEqualTo(0);
	}

	@Test
	public void toTerabytes() {
		assertThat(DigitalAmount.fromTerabytes(1).toTerabytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromGigabytes(1).toTerabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromMegabytes(1).toTerabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromKilobytes(1).toTerabytes()).isEqualTo(0);
		assertThat(DigitalAmount.fromBytes(1).toTerabytes()).isEqualTo(0);
	}

	@Test
	public void fromBytes() {
		assertThat(DigitalAmount.fromBytes(1024L * 1024 * 1024 * 1024).toTerabytes())
				.isEqualTo(1);
		assertThat(DigitalAmount.fromBytes(1024L * 1024 * 1024 * 1024).toGigabytes())
				.isEqualTo(1024);
		assertThat(DigitalAmount.fromBytes(1024L * 1024 * 1024 * 1024).toMegabytes())
				.isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromBytes(1024L * 1024 * 1024 * 1024).toKilobytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromBytes(1024L * 1024 * 1024 * 1024).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
	}

	@Test
	public void fromKilobytes() {
		assertThat(DigitalAmount.fromKilobytes(1024L * 1024 * 1024 * 1024).toTerabytes())
				.isEqualTo(1024);
		assertThat(DigitalAmount.fromKilobytes(1024L * 1024 * 1024 * 1024).toGigabytes())
				.isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromKilobytes(1024L * 1024 * 1024 * 1024).toMegabytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromKilobytes(1024L * 1024 * 1024 * 1024).toKilobytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromKilobytes(1024L * 1024 * 1024 * 1024).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024 * 1024);
	}

	@Test
	public void fromMegabytes() {
		assertThat(DigitalAmount.fromMegabytes(1024L * 1024 * 1024 * 1024).toTerabytes())
				.isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1024L * 1024 * 1024 * 1024).toGigabytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1024L * 1024 * 1024 * 1024).toMegabytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1024L * 1024 * 1024 * 1024).toKilobytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromMegabytes(1024L * 1024 * 1024 * 1024).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024 * 1024 * 1024);
	}

	@Test
	public void fromGigabytes() {
		assertThat(DigitalAmount.fromGigabytes(1024L).toTerabytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromGigabytes(1024L).toGigabytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromGigabytes(1024L).toMegabytes())
				.isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromGigabytes(1024L).toKilobytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromGigabytes(1024L).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
	}

	@Test
	public void fromTerabytes() {
		assertThat(DigitalAmount.fromTerabytes(1).toTerabytes()).isEqualTo(1);
		assertThat(DigitalAmount.fromTerabytes(1).toGigabytes()).isEqualTo(1024);
		assertThat(DigitalAmount.fromTerabytes(1).toMegabytes()).isEqualTo(1024 * 1024);
		assertThat(DigitalAmount.fromTerabytes(1).toKilobytes())
				.isEqualTo(1024 * 1024 * 1024);
		assertThat(DigitalAmount.fromTerabytes(1).toBytes())
				.isEqualTo(1024L * 1024 * 1024 * 1024);
	}

	@Test
	public void fromUnit() {
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.BYTES))
				.isEqualTo(DigitalAmount.fromBytes(1));
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.KILOBYTES))
				.isEqualTo(DigitalAmount.fromKilobytes(1));
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.MEGABYTES))
				.isEqualTo(DigitalAmount.fromMegabytes(1));
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.GIGABYTES))
				.isEqualTo(DigitalAmount.fromGigabytes(1));
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.TERABYTES))
				.isEqualTo(DigitalAmount.fromTerabytes(1));
	}

	@Test
	public void add() {
		assertThat(DigitalAmount.fromTerabytes(1).add(DigitalAmount.fromGigabytes(1))
				.add(DigitalAmount.fromMegabytes(10))
				.add(DigitalAmount.fromKilobytes(10).add(DigitalAmount.fromBytes(10))))
						.isEqualTo(DigitalAmount.fromBytes(1100595865610L));
	}

	@Test
	public void addUnit() {
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.TERABYTES)
				.add(1, DigitalUnit.GIGABYTES).add(10, DigitalUnit.MEGABYTES)
				.add(10, DigitalUnit.KILOBYTES).add(10, DigitalUnit.BYTES))
						.isEqualTo(DigitalAmount.fromBytes(1100595865610L));
	}

	@Test
	public void subtract() {

		assertThat(DigitalAmount.fromTerabytes(1).subtract(DigitalAmount.fromGigabytes(1))
				.subtract(DigitalAmount.fromMegabytes(10))
				.subtract(DigitalAmount.fromKilobytes(10)
						.subtract(DigitalAmount.fromBytes(10))))
								.isEqualTo(DigitalAmount.fromBytes(1098427389962L));
	}

	@Test
	public void subtractUnit() {
		assertThat(DigitalAmount.fromUnit(1, DigitalUnit.TERABYTES)
				.subtract(1, DigitalUnit.GIGABYTES).subtract(10, DigitalUnit.MEGABYTES)
				.subtract(10, DigitalUnit.KILOBYTES).subtract(10, DigitalUnit.BYTES))
						.isEqualTo(DigitalAmount.fromBytes(1098427389942L));
	}

}
