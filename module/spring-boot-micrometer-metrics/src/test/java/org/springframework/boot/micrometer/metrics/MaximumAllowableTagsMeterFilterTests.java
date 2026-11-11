/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics;

import java.util.Collections;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Type;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MaximumAllowableTagsMeterFilter}.
 *
 * @author Phillip Webb
 */
class MaximumAllowableTagsMeterFilterTests {

	@Test
	void applyWhenNameDoesNotHavePrefixReturnsNeutral() {
		MaximumAllowableTagsMeterFilter filter = new MaximumAllowableTagsMeterFilter("test", "k", 1);
		assertThat(filter.accept(meterId("tset", "k", "v"))).isEqualTo(MeterFilterReply.NEUTRAL);
		assertThat(filter).extracting("observedTagValues").asInstanceOf(InstanceOfAssertFactories.COLLECTION).isEmpty();
	}

	@Test
	void applyWhenNameHasPrefixButNoTagKeyReturnsNeutral() {
		MaximumAllowableTagsMeterFilter filter = new MaximumAllowableTagsMeterFilter("test", "k", 1);
		assertThat(filter.accept(meterId("test", "k", "v"))).isEqualTo(MeterFilterReply.NEUTRAL);
		assertThat(filter).extracting("observedTagValues")
			.asInstanceOf(InstanceOfAssertFactories.COLLECTION)
			.containsExactly("v");
	}

	@Test
	void applyWhenNameHasPrefixAndTagKeyReturnsNeutralUntilLimit() {
		MaximumAllowableTagsMeterFilter filter = new MaximumAllowableTagsMeterFilter("test", "k", 1);
		assertThat(filter.accept(meterId("test", "k", "v1"))).isEqualTo(MeterFilterReply.NEUTRAL);
		assertThat(filter.accept(meterId("test", "k", "v2"))).isEqualTo(MeterFilterReply.DENY);
		assertThat(filter.accept(meterId("test", "k", "v3"))).isEqualTo(MeterFilterReply.DENY);
		assertThat(filter).extracting("observedTagValues")
			.asInstanceOf(InstanceOfAssertFactories.COLLECTION)
			.containsExactly("v1");
	}

	private Meter.Id meterId(String name, String tagKey, String tagValue) {
		MeterRegistry registry = new SimpleMeterRegistry();
		Meter meter = Meter.builder(name, Type.COUNTER, Collections.emptyList())
			.tag(tagKey, tagValue)
			.register(registry);
		return meter.getId();
	}

}
