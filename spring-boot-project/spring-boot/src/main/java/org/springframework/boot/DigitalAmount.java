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

import java.io.Serializable;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * This class models a quantity or amount of digital information in terms of bytes. This
 * class is immutable and thread-safe.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 * @see DigitalAmountStyle
 * @see DigitalUnit
 */
public final class DigitalAmount implements Comparable<DigitalAmount>, Serializable {

	private final long bytes;

	private DigitalAmount(long bytes) {
		this.bytes = bytes;
	}

	/**
	 * Converts this {@code DigitalAmount} to the bytes.
	 * @return the total amount of the bytes.
	 */
	public long toBytes() {
		return this.bytes;
	}

	/**
	 * Converts this {@code DigitalAmount} to the kilobytes.
	 * @return the total amount of the kilobytes.
	 */
	public long toKilobytes() {
		return DigitalUnit.BYTES.toKilobytes(this.bytes);
	}

	/**
	 * Converts this {@code DigitalAmount} to the megabytes.
	 * @return the total amount of the megabytes.
	 */
	public long toMegabytes() {
		return DigitalUnit.BYTES.toMegabytes(this.bytes);
	}

	/**
	 * Converts this {@code DigitalAmount} to the gigabytes.
	 * @return the total amount of the gigabytes.
	 */
	public long toGigabytes() {
		return DigitalUnit.BYTES.toGigabytes(this.bytes);
	}

	/**
	 * Converts this {@code DigitalAmount} to the terabytes.
	 * @return the total amount of the terabytes.
	 */
	public long toTerabytes() {
		return DigitalUnit.BYTES.toTerabytes(this.bytes);
	}

	/**
	 * Checks if this duration is {@code DigitalAmount} negative, excluding zero.
	 * @return true if this {@code DigitalAmount} has a total amount less than zero
	 */
	public boolean isNegative() {
		return this.bytes < 0;
	}

	/**
	 * Checks if this duration is {@code DigitalAmount} positive, excluding zero.
	 * @return true if this {@code DigitalAmount} has a total amount greater than zero
	 */
	public boolean isPositive() {
		return this.bytes > 0;
	}

	/**
	 * Checks if this {@code DigitalAmount} is zero.
	 * @return true if this {@code DigitalAmount} has a total amount equal to zero
	 */
	public boolean isZero() {
		return this.bytes == 0;
	}

	/**
	 * If the value matches the given predicate, return an {@code Optional} describing the
	 * value, otherwise return an empty {@code Optional}.
	 * @param predicate a predicate to apply to the value.
	 * @return an {@code Optional} describing the value if a value matches the given
	 * predicate, otherwise an empty {@code Optional}
	 */
	public Optional<DigitalAmount> filter(Predicate<? super DigitalAmount> predicate) {
		Assert.notNull(predicate, () -> "Predicate must not be null");
		if (predicate.test(this)) {
			return Optional.of(this);
		}
		return Optional.empty();
	}

	/**
	 * Apply the provided mapping function to this.
	 * @param <T> the type of the result of the mapping function
	 * @param mapper a mapping function to apply to the value.
	 * @return the result of applying a mapping function
	 */
	public <T> T map(Function<? super DigitalAmount, ? extends T> mapper) {
		Assert.notNull(mapper, () -> "Mapper must not be null");
		return mapper.apply(this);
	}

	/**
	 * Returns a {@code DigitalAmount} whose value is {@code (this +
	 * other)}.
	 * @param amount value to be added to this {@code DigitalAmount}.
	 * @param unit {@code DigitalUnit} of this amount.
	 * @return {@code this + other}
	 */
	public DigitalAmount add(long amount, DigitalUnit unit) {
		return add(fromUnit(amount, unit));
	}

	/**
	 * Returns a {@code DigitalAmount} whose value is {@code (this -
	 * other)}.
	 * @param amount value to be subtracted to this {@code DigitalAmount}.
	 * @param unit {@code DigitalUnit} of this amount.
	 * @return {@code this - other}
	 */
	public DigitalAmount subtract(long amount, DigitalUnit unit) {
		return subtract(fromUnit(amount, unit));
	}

	/**
	 * Returns a new {@code DigitalAmount} whose value is {@code (this +
	 * other)}.
	 * @param other value to be added to this {@code DigitalAmount}.
	 * @return {@code this + other}
	 */
	public DigitalAmount add(DigitalAmount other) {
		Assert.notNull(other, () -> "Digital Amount must not be null");
		return new DigitalAmount(Math.addExact(this.bytes, other.bytes));
	}

	/**
	 * Returns a new {@code DigitalAmount} whose value is {@code (this -
	 * other)}.
	 * @param other value to be subtracted from this {@code DigitalAmount}.
	 * @return {@code this - other}
	 */
	public DigitalAmount subtract(DigitalAmount other) {
		Assert.notNull(other, () -> "Digital Amount must not be null");
		return new DigitalAmount(Math.subtractExact(this.bytes, other.bytes));
	}

	@Override
	public int compareTo(@NonNull DigitalAmount other) {
		Assert.notNull(other, () -> "Digital Amount must not be null");
		return Long.compare(this.bytes, other.bytes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DigitalAmount that = (DigitalAmount) o;
		return this.bytes == that.bytes;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(this.bytes);
	}

	/**
	 * Print this {@code DigitalAmount} using the given unit.
	 * @param unit the {@code DigitalUnit} to use if the value doesn't specify,
	 * {@link DigitalUnit#BYTES} will be used
	 * @return the printed result
	 * @see DigitalAmountStyle
	 */
	public String print(DigitalUnit unit) {
		return DigitalAmountStyle.SIMPLE.print(this, unit);
	}

	@Override
	public String toString() {
		return print(DigitalUnit.BYTES);
	}

	/**
	 * Parse the given value to a {@code DigitalAmount} using
	 * {@link DigitalAmountStyle#SIMPLE}.
	 * @param value the value to parse
	 * @return a {@code DigitalAmount}
	 */
	public static DigitalAmount parse(CharSequence value) {
		return parse(value, DigitalUnit.BYTES);
	}

	/**
	 * Parse the given value to a {@code DigitalAmount} using
	 * {@link DigitalAmountStyle#SIMPLE}.
	 * @param value the value to parse
	 * @param unit {@code DigitalUnit} to use if one is not specified, default value is
	 * {@link DigitalUnit#BYTES}
	 * @return a {@code DigitalAmount}
	 */
	public static DigitalAmount parse(CharSequence value, DigitalUnit unit) {
		return DigitalAmountStyle.SIMPLE.parse(value, unit);
	}

	/**
	 * Creates a new {@code DigitalAmount} from the bytes.
	 * @param amount amount of the bytes
	 * @return a {@code DigitalAmount}
	 */
	public static DigitalAmount fromBytes(long amount) {
		return new DigitalAmount(amount);
	}

	/**
	 * Creates a new {@code DigitalAmount} from the kilobytes.
	 * @param amount amount of the kilobytes
	 * @return a {@code DigitalAmount}
	 * @throws ArithmeticException if the result overflows a long
	 */
	public static DigitalAmount fromKilobytes(long amount) {
		return new DigitalAmount(DigitalUnit.KILOBYTES.toBytes(amount));
	}

	/**
	 * Creates a new {@code DigitalAmount} from the megabytes.
	 * @param amount amount of the megabytes
	 * @return a {@code DigitalAmount}
	 * @throws ArithmeticException if the result overflows a long
	 */
	public static DigitalAmount fromMegabytes(long amount) {
		return new DigitalAmount(DigitalUnit.MEGABYTES.toBytes(amount));
	}

	/**
	 * Creates a new {@code DigitalAmount} from the gigabytes.
	 * @param amount amount of the gigabytes
	 * @return a {@code DigitalAmount}
	 * @throws ArithmeticException if the result overflows a long
	 */
	public static DigitalAmount fromGigabytes(long amount) {
		return new DigitalAmount(DigitalUnit.GIGABYTES.toBytes(amount));
	}

	/**
	 * Creates a new {@code DigitalAmount} from the terabytes.
	 * @param amount amount of the terabytes
	 * @return a {@code DigitalAmount}
	 * @throws ArithmeticException if the result overflows a long
	 */
	public static DigitalAmount fromTerabytes(long amount) {
		return new DigitalAmount(DigitalUnit.TERABYTES.toBytes(amount));
	}

	/**
	 * Creates a new {@code DigitalAmount} from the given amount and unit.
	 * @param amount amount of the unit
	 * @param unit unit of the amount
	 * @return a {@code DigitalAmount}
	 * @throws ArithmeticException if the result overflows a long
	 */
	public static DigitalAmount fromUnit(long amount, DigitalUnit unit) {
		Assert.notNull(unit, () -> "Digital Unit must not be null");
		return new DigitalAmount(unit.toBytes(amount));
	}

}
