/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
