/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.micrometer.tracing.autoconfigure.TracingProperties.Mdc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link TracingProperties}.
 *
 * @author Moritz Halbritter
 */
class TracingPropertiesTests {

	private final Mdc mdc = new TracingProperties().getMdc();

	@Test
	void shouldUseTraceIdAndSpanIdAsDefaultMdcKeys() {
		assertThat(this.mdc.getTraceIdKey()).isEqualTo("traceId");
		assertThat(this.mdc.getSpanIdKey()).isEqualTo("spanId");
	}

	@Test
	void shouldNotBeCustomizedWhenMdcKeysAreDefault() {
		assertThat(this.mdc.isCustomized()).isFalse();
	}

	@Test
	void shouldBeCustomizedWhenAnMdcKeyDiffersFromItsDefault() {
		this.mdc.setSpanIdKey("customSpanId");
		assertThat(this.mdc.isCustomized()).isTrue();
	}

	@Test
	void shouldRejectBlankTraceIdKey() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.mdc.setTraceIdKey(" "));
	}

	@Test
	void shouldRejectBlankSpanIdKey() {
		assertThatIllegalArgumentException().isThrownBy(() -> this.mdc.setSpanIdKey(" "));
	}

}
