/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure;

import java.time.Duration;

import io.micrometer.core.instrument.Meter;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * A boundary for a service-level objective (SLO) for use when configuring Micrometer. Can
 * be specified as either a {@link Double} (applicable to timers and distribution
 * summaries) or a {@link Duration} (applicable to only timers).
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class ServiceLevelObjectiveBoundary {

	private final MeterValue value;

	ServiceLevelObjectiveBoundary(MeterValue value) {
		this.value = value;
	}

	/**
	 * Return the underlying value of the SLO in form suitable to apply to the given meter
	 * type.
	 * @param meterType the meter type
	 * @return the value or {@code null} if the value cannot be applied
	 */
	public @Nullable Double getValue(Meter.Type meterType) {
		return this.value.getValue(meterType);
	}

	/**
	 * Return a new {@link ServiceLevelObjectiveBoundary} instance for the given double
	 * value.
	 * @param value the source value
	 * @return a {@link ServiceLevelObjectiveBoundary} instance
	 */
	public static ServiceLevelObjectiveBoundary valueOf(double value) {
		return new ServiceLevelObjectiveBoundary(MeterValue.valueOf(value));
	}

	/**
	 * Return a new {@link ServiceLevelObjectiveBoundary} instance for the given String
	 * value.
	 * @param value the source value
	 * @return a {@link ServiceLevelObjectiveBoundary} instance
	 */
	public static ServiceLevelObjectiveBoundary valueOf(String value) {
		return new ServiceLevelObjectiveBoundary(MeterValue.valueOf(value));
	}

	static class ServiceLevelObjectiveBoundaryHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection().registerType(ServiceLevelObjectiveBoundary.class, MemberCategory.INVOKE_PUBLIC_METHODS);
		}

	}

}
