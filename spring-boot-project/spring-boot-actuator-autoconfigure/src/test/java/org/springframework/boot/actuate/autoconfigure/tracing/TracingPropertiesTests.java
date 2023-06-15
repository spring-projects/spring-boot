/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.tracing;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TracingProperties}.
 *
 * @author Moritz Halbritter
 */
class TracingPropertiesTests {

	@Test
	void propagationTypeShouldOverrideProduceTypes() {
		TracingProperties.Propagation propagation = new TracingProperties.Propagation();
		propagation.setProduce(List.of(TracingProperties.Propagation.PropagationType.W3C));
		propagation.setType(List.of(TracingProperties.Propagation.PropagationType.B3));
		assertThat(propagation.getEffectiveProducedTypes())
			.containsExactly(TracingProperties.Propagation.PropagationType.B3);
	}

	@Test
	void propagationTypeShouldOverrideConsumeTypes() {
		TracingProperties.Propagation propagation = new TracingProperties.Propagation();
		propagation.setConsume(List.of(TracingProperties.Propagation.PropagationType.W3C));
		propagation.setType(List.of(TracingProperties.Propagation.PropagationType.B3));
		assertThat(propagation.getEffectiveConsumedTypes())
			.containsExactly(TracingProperties.Propagation.PropagationType.B3);
	}

	@Test
	void getEffectiveConsumeTypes() {
		TracingProperties.Propagation propagation = new TracingProperties.Propagation();
		propagation.setConsume(List.of(TracingProperties.Propagation.PropagationType.W3C));
		assertThat(propagation.getEffectiveConsumedTypes())
			.containsExactly(TracingProperties.Propagation.PropagationType.W3C);
	}

	@Test
	void getEffectiveProduceTypes() {
		TracingProperties.Propagation propagation = new TracingProperties.Propagation();
		propagation.setProduce(List.of(TracingProperties.Propagation.PropagationType.W3C));
		assertThat(propagation.getEffectiveProducedTypes())
			.containsExactly(TracingProperties.Propagation.PropagationType.W3C);
	}

}
