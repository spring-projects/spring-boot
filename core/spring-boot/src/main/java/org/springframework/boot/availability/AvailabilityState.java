/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.availability;

/**
 * Tagging interface used on {@link ApplicationAvailability} states. This interface is
 * usually implemented on an {@code enum} type.
 *
 * @author Phillip Webb
 * @since 2.3.0
 * @see LivenessState
 * @see ReadinessState
 */
public interface AvailabilityState {

}
