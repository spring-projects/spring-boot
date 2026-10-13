/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.metrics;

import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Timer.Builder;
import org.jspecify.annotations.Nullable;

import org.springframework.util.CollectionUtils;

/**
 * Strategy that can be used to apply {@link Timer Timers} automatically instead of using
 * {@link Timed @Timed}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface AutoTimer {

	/**
	 * An {@link AutoTimer} implementation that is enabled but applies no additional
	 * customizations.
	 */
	AutoTimer ENABLED = (builder) -> {
	};

	/**
	 * An {@link AutoTimer} implementation that is disabled and will not record metrics.
	 */
	AutoTimer DISABLED = new AutoTimer() {

		@Override
		public boolean isEnabled() {
			return false;
		}

		@Override
		public void apply(Builder builder) {
			throw new IllegalStateException("AutoTimer is disabled");
		}

	};

	/**
	 * Return if the auto-timer is enabled and metrics should be recorded.
	 * @return if the auto-timer is enabled
	 */
	default boolean isEnabled() {
		return true;
	}

	/**
	 * Factory method to create a new {@link Builder Timer.Builder} with auto-timer
	 * settings {@link #apply(Timer.Builder) applied}.
	 * @param name the name of the timer
	 * @return a new builder instance with auto-settings applied
	 */
	default Timer.Builder builder(String name) {
		return builder(() -> Timer.builder(name));
	}

	/**
	 * Factory method to create a new {@link Builder Timer.Builder} with auto-timer
	 * settings {@link #apply(Timer.Builder) applied}.
	 * @param supplier the builder supplier
	 * @return a new builder instance with auto-settings applied
	 */
	default Timer.Builder builder(Supplier<Timer.Builder> supplier) {
		Timer.Builder builder = supplier.get();
		apply(builder);
		return builder;
	}

	/**
	 * Called to apply any auto-timer settings to the given {@link Builder Timer.Builder}.
	 * @param builder the builder to apply settings to
	 */
	void apply(Timer.Builder builder);

	static void apply(@Nullable AutoTimer autoTimer, String metricName, Set<Timed> annotations,
			Consumer<Timer.Builder> action) {
		if (!CollectionUtils.isEmpty(annotations)) {
			for (Timed annotation : annotations) {
				action.accept(Timer.builder(annotation, metricName));
			}
		}
		else {
			if (autoTimer != null && autoTimer.isEnabled()) {
				action.accept(autoTimer.builder(metricName));
			}
		}
	}

}
