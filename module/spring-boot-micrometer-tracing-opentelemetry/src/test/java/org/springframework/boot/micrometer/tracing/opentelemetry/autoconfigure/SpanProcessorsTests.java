/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import java.util.List;

import io.opentelemetry.sdk.trace.SpanProcessor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpanProcessors}.
 *
 * @author Moritz Halbritter
 */
class SpanProcessorsTests {

	@Test
	void ofList() {
		SpanProcessor spanProcessor1 = mock(SpanProcessor.class);
		SpanProcessor spanProcessor2 = mock(SpanProcessor.class);
		SpanProcessors spanProcessors = SpanProcessors.of(List.of(spanProcessor1, spanProcessor2));
		assertThat(spanProcessors).containsExactly(spanProcessor1, spanProcessor2);
		assertThat(spanProcessors.list()).containsExactly(spanProcessor1, spanProcessor2);
	}

	@Test
	void ofArray() {
		SpanProcessor spanProcessor1 = mock(SpanProcessor.class);
		SpanProcessor spanProcessor2 = mock(SpanProcessor.class);
		SpanProcessors spanProcessors = SpanProcessors.of(spanProcessor1, spanProcessor2);
		assertThat(spanProcessors).containsExactly(spanProcessor1, spanProcessor2);
		assertThat(spanProcessors.list()).containsExactly(spanProcessor1, spanProcessor2);
	}

}
