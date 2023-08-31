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

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagation.FactoryBuilder;
import brave.baggage.BaggagePropagationConfig;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import brave.propagation.Propagation.KeyFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalBaggageFields}.
 *
 * @author Moritz Halbritter
 */
class LocalBaggageFieldsTests {

	@Test
	void extractFromBuilder() {
		FactoryBuilder builder = createBuilder();
		builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create("remote-field-1")));
		builder.add(BaggagePropagationConfig.SingleBaggageField.remote(BaggageField.create("remote-field-2")));
		builder.add(BaggagePropagationConfig.SingleBaggageField.local(BaggageField.create("local-field-1")));
		builder.add(BaggagePropagationConfig.SingleBaggageField.local(BaggageField.create("local-field-2")));
		LocalBaggageFields fields = LocalBaggageFields.extractFrom(builder);
		assertThat(fields.asList()).containsExactlyInAnyOrder("local-field-1", "local-field-2");
	}

	@Test
	void empty() {
		assertThat(LocalBaggageFields.empty().asList()).isEmpty();
	}

	@SuppressWarnings("deprecation")
	private static FactoryBuilder createBuilder() {
		return BaggagePropagation.newFactoryBuilder(new Factory() {
			@Override
			public <K> Propagation<K> create(KeyFactory<K> keyFactory) {
				return null;
			}
		});
	}

}
