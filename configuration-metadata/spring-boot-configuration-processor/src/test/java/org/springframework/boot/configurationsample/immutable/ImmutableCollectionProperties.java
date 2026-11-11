/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationsample.immutable;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.configurationsample.TestDefaultValue;

/**
 * Simple immutable properties with collections types and defaults.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("unused")
public class ImmutableCollectionProperties {

	private final List<String> names;

	private final List<Boolean> flags;

	private final List<Duration> durations;

	public ImmutableCollectionProperties(List<String> names, @TestDefaultValue({ "true", "false" }) List<Boolean> flags,
			@TestDefaultValue({ "10s", "1m", "1h" }) List<Duration> durations) {
		this.names = names;
		this.flags = flags;
		this.durations = durations;
	}

}
