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

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A {@code DigitalUnit} represents digital amount at a given unit of granularity and
 * provides utility methods to convert across units.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
public enum DigitalUnit {

	/**
	 * A unit that represents the concept of a byte.
	 */
	BYTES("Bytes", "B") {
		@Override
		public long fromBytes(long amount) {
			return amount;
		}

		@Override
		public long fromKilobytes(long amount) {
			return KILOBYTES.toBytes(amount);
		}

		@Override
		public long fromMegabytes(long amount) {
			return MEGABYTES.toBytes(amount);
		}

		@Override
		public long fromGigabytes(long amount) {
			return GIGABYTES.toBytes(amount);
		}

		@Override
		public long fromTerabytes(long amount) {
			return TERABYTES.toBytes(amount);
		}

		@Override
		public long toBytes(long amount) {
			return amount;
		}

		@Override
		public long toKilobytes(long amount) {
			return amount / KB;
		}

		@Override
		public long toMegabytes(long amount) {
			return amount / MB;

		}

		@Override
		public long toGigabytes(long amount) {
			return amount / GB;

		}

		@Override
		public long toTerabytes(long amount) {
			return amount / TB;

		}

	},
	/**
	 * A unit that represents the concept of a kilobyte.
	 */
	KILOBYTES("Kilobytes", "KB") {
		@Override
		public long fromBytes(long amount) {
			return BYTES.toKilobytes(amount);
		}

		@Override
		public long fromKilobytes(long amount) {
			return amount;
		}

		@Override
		public long fromMegabytes(long amount) {
			return MEGABYTES.toKilobytes(amount);
		}

		@Override
		public long fromGigabytes(long amount) {
			return GIGABYTES.toKilobytes(amount);
		}

		@Override
		public long fromTerabytes(long amount) {
			return TERABYTES.toKilobytes(amount);
		}

		@Override
		public long toBytes(long amount) {
			return Math.multiplyExact(amount, KB);
		}

		@Override
		public long toKilobytes(long amount) {
			return amount;
		}

		@Override
		public long toMegabytes(long amount) {
			return amount / (MB / KB);

		}

		@Override
		public long toGigabytes(long amount) {
			return amount / (GB / KB);

		}

		@Override
		public long toTerabytes(long amount) {
			return amount / (TB / KB);
		}
	},
	/**
	 * A unit that represents the concept of megabyte.
	 */
	MEGABYTES("Megabytes", "MB") {
		@Override
		public long fromBytes(long amount) {
			return BYTES.toMegabytes(amount);
		}

		@Override
		public long fromKilobytes(long amount) {
			return KILOBYTES.toMegabytes(amount);
		}

		@Override
		public long fromMegabytes(long amount) {
			return amount;
		}

		@Override
		public long fromGigabytes(long amount) {
			return GIGABYTES.toMegabytes(amount);
		}

		@Override
		public long fromTerabytes(long amount) {
			return TERABYTES.toMegabytes(amount);
		}

		@Override
		public long toBytes(long amount) {
			return Math.multiplyExact(amount, MB);
		}

		@Override
		public long toKilobytes(long amount) {
			return Math.multiplyExact(amount, KB);
		}

		@Override
		public long toMegabytes(long amount) {
			return amount;

		}

		@Override
		public long toGigabytes(long amount) {
			return amount / (GB / MB);

		}

		@Override
		public long toTerabytes(long amount) {
			return amount / (TB / MB);
		}
	},
	/**
	 * A unit that represents the concept of gigabyte.
	 */
	GIGABYTES("Gigabytes", "GB") {
		@Override
		public long fromBytes(long amount) {
			return BYTES.toGigabytes(amount);
		}

		@Override
		public long fromKilobytes(long amount) {
			return KILOBYTES.toGigabytes(amount);
		}

		@Override
		public long fromMegabytes(long amount) {
			return MEGABYTES.toGigabytes(amount);
		}

		@Override
		public long fromGigabytes(long amount) {
			return amount;
		}

		@Override
		public long fromTerabytes(long amount) {
			return TERABYTES.toGigabytes(amount);
		}

		@Override
		public long toBytes(long amount) {
			return Math.multiplyExact(amount, GB);
		}

		@Override
		public long toKilobytes(long amount) {
			return Math.multiplyExact(amount, MB);
		}

		@Override
		public long toMegabytes(long amount) {
			return Math.multiplyExact(amount, KB);
		}

		@Override
		public long toGigabytes(long amount) {
			return amount;

		}

		@Override
		public long toTerabytes(long amount) {
			return amount / (TB / GB);
		}
	},
	/**
	 * A unit that represents the concept of terabyte.
	 */
	TERABYTES("Terabytes", "TB") {
		@Override
		public long fromBytes(long amount) {
			return BYTES.toTerabytes(amount);
		}

		@Override
		public long fromKilobytes(long amount) {
			return KILOBYTES.toTerabytes(amount);
		}

		@Override
		public long fromMegabytes(long amount) {
			return MEGABYTES.toTerabytes(amount);
		}

		@Override
		public long fromGigabytes(long amount) {
			return GIGABYTES.toTerabytes(amount);
		}

		@Override
		public long fromTerabytes(long amount) {
			return amount;
		}

		@Override
		public long toBytes(long amount) {
			return Math.multiplyExact(amount, TB);
		}

		@Override
		public long toKilobytes(long amount) {
			return Math.multiplyExact(amount, GB);
		}

		@Override
		public long toMegabytes(long amount) {
			return Math.multiplyExact(amount, MB);
		}

		@Override
		public long toGigabytes(long amount) {
			return Math.multiplyExact(amount, KB);
		}

		@Override
		public long toTerabytes(long amount) {
			return amount;
		}
	};

	private static final long KB = 1024;

	private static final long MB = KB * 1024;

	private static final long GB = MB * 1024;

	private static final long TB = GB * 1024;

	private final String name;

	private final String abbreviation;

	DigitalUnit(String name, String abbreviation) {
		this.name = name;
		this.abbreviation = abbreviation;
	}

	public final String getName() {
		return this.name;
	}

	public final String getAbbreviation() {
		return this.abbreviation;
	}

	/**
	 * Converts the given amount of the given unit to this unit.
	 * @param amount the amount to convert
	 * @param unit the unit of the amount
	 * @return converted amount.
	 */
	public final long toUnit(long amount, @NonNull DigitalUnit unit) {
		Assert.notNull(unit, "Digital Unit must not be null");
		switch (unit) {
		case BYTES:
			return toBytes(amount);
		case KILOBYTES:
			return toKilobytes(amount);
		case MEGABYTES:
			return toMegabytes(amount);
		case GIGABYTES:
			return toGigabytes(amount);
		case TERABYTES:
			return toTerabytes(amount);
		}
		throw new IllegalArgumentException("Unknown Digital Unit '" + unit + "'");
	}

	/**
	 * Converts the given amount of this unit to bytes.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long toBytes(long amount);

	/**
	 * Converts the given amount of this unit to kilobytes.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long toKilobytes(long amount);

	/**
	 * Converts the given amount of this unit to megabytes.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long toMegabytes(long amount);

	/**
	 * Converts the given amount of this unit to gigabytes.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long toGigabytes(long amount);

	/**
	 * Converts the given amount of this unit to terabytes.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long toTerabytes(long amount);

	/**
	 * Converts the given amount of the given unit to this unit.
	 * @param amount the amount to convert
	 * @param unit the unit of the amount
	 * @return converted amount.
	 * @throws ArithmeticException if the result overflows a long
	 */
	public final long fromUnit(long amount, @NonNull DigitalUnit unit) {
		Assert.notNull(unit, "Digital Unit must not be null");
		switch (unit) {
		case BYTES:
			return fromBytes(amount);
		case KILOBYTES:
			return fromKilobytes(amount);
		case MEGABYTES:
			return fromMegabytes(amount);
		case GIGABYTES:
			return fromGigabytes(amount);
		case TERABYTES:
			return fromTerabytes(amount);
		}
		throw new IllegalArgumentException("Unknown unit '" + unit + "'");
	}

	/**
	 * Converts the given amount of bytes to this unit.
	 * @param amount the amount to convert
	 * @return converted amount.
	 */
	public abstract long fromBytes(long amount);

	/**
	 * Converts the given amount of kilobytes to this unit.
	 * @param amount the amount to convert
	 * @return converted amount.
	 * @throws ArithmeticException if the result overflows a long
	 */
	public abstract long fromKilobytes(long amount);

	/**
	 * Converts the given amount of megabytes to this unit.
	 * @param amount the amount to convert
	 * @return converted amount.
	 * @throws ArithmeticException if the result overflows a long
	 */
	public abstract long fromMegabytes(long amount);

	/**
	 * Converts the given amount of gigabytes to this unit.
	 * @param amount the amount to convert
	 * @return converted amount.
	 * @throws ArithmeticException if the result overflows a long
	 */
	public abstract long fromGigabytes(long amount);

	/**
	 * Converts the given amount of terabytes to this unit.
	 * @param amount the amount to convert
	 * @return converted amount.
	 * @throws ArithmeticException if the result overflows a long
	 */
	public abstract long fromTerabytes(long amount);

	/**
	 * Detect the {@code DigitalUnit} from the given abbreviation value.
	 * @param value the abbreviation value
	 * @return the {@code DigitalUnit}
	 * @throws IllegalStateException if the value is not a known abbreviation
	 */
	public static DigitalUnit fromAbbreviation(@Nullable String value) {
		if (StringUtils.hasText(value)) {
			for (DigitalUnit unit : values()) {
				if (unit.abbreviation.equalsIgnoreCase(value.trim())) {
					return unit;
				}
			}
		}
		throw new IllegalArgumentException("Unknown abbreviation '" + value + "'");
	}

	/**
	 * Detect the {@code DigitalUnit} from the given name value.
	 * @param value the name value
	 * @return the {@code DigitalUnit}
	 * @throws IllegalStateException if the value is not a known name
	 */
	public static DigitalUnit fromName(@Nullable String value) {
		if (StringUtils.hasText(value)) {
			for (DigitalUnit unit : values()) {
				if (unit.name.equalsIgnoreCase(value.trim())) {
					return unit;
				}
			}
		}
		throw new IllegalArgumentException("Unknown name '" + value + "'");
	}

	@Override
	public String toString() {
		return this.name;
	}

}
