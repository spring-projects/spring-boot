/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.endpoint;

import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointIdTimeToLivePropertyFunction}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class EndpointIdTimeToLivePropertyFunctionTests {

	private final MockEnvironment environment = new MockEnvironment();

	private final Function<EndpointId, @Nullable Long> timeToLive = new EndpointIdTimeToLivePropertyFunction(
			this.environment);

	@Test
	void defaultConfiguration() {
		Long result = this.timeToLive.apply(EndpointId.of("test"));
		assertThat(result).isNull();
	}

	@Test
	void userConfiguration() {
		this.environment.setProperty("management.endpoint.test.cache.time-to-live", "500");
		Long result = this.timeToLive.apply(EndpointId.of("test"));
		assertThat(result).isEqualTo(500L);
	}

	@Test
	void mixedCaseUserConfiguration() {
		this.environment.setProperty("management.endpoint.another-test.cache.time-to-live", "500");
		Long result = this.timeToLive.apply(EndpointId.of("anotherTest"));
		assertThat(result).isEqualTo(500L);
	}

}
