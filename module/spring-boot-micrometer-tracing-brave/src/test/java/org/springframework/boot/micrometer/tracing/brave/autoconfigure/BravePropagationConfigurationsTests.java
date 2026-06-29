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

package org.springframework.boot.micrometer.tracing.brave.autoconfigure;

import brave.baggage.BaggageFields;
import brave.baggage.CorrelationScopeConfig.SingleCorrelationField;
import brave.baggage.CorrelationScopeDecorator;
import brave.context.slf4j.MDCScopeDecorator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BravePropagationConfigurations}.
 */
class BravePropagationConfigurationsTests {

	/**
	 * Validates the assumption that {@link MDCScopeDecorator#newBuilder()} only
	 * configures traceId and spanId by default. The
	 * {@link BravePropagationConfigurations.PropagationWithBaggage#mdcCorrelationScopeDecoratorBuilder}
	 * method clears the builder's defaults and re-adds only these two fields so their MDC
	 * key names can be customized. If Brave adds new default fields in the future, the
	 * {@code .clear()} call in that method would silently drop them, and this test will
	 * catch that.
	 */
	@Test
	void mdcScopeDecoratorBuilderShouldOnlyHaveTraceIdAndSpanIdByDefault() {
		CorrelationScopeDecorator.Builder builder = MDCScopeDecorator.newBuilder();
		assertThat(builder.configs()).extracting((config) -> ((SingleCorrelationField) config).name())
			.containsExactlyInAnyOrder(BaggageFields.TRACE_ID.name(), BaggageFields.SPAN_ID.name());
	}

}
