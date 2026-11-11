/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.brave.autoconfigure;

import brave.baggage.BaggageField;
import brave.baggage.BaggagePropagation;
import brave.baggage.BaggagePropagation.FactoryBuilder;
import brave.baggage.BaggagePropagationConfig;
import brave.propagation.Propagation;
import brave.propagation.Propagation.Factory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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

	private static FactoryBuilder createBuilder() {
		return BaggagePropagation.newFactoryBuilder(new Factory() {
			@Override
			@SuppressWarnings("unchecked")
			public Propagation<String> get() {
				return mock(Propagation.class);
			}
		});
	}

}
