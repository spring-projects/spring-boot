/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure;

import java.util.List;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SpanExporters}.
 *
 * @author Moritz Halbritter
 */
class SpanExportersTests {

	@Test
	void ofList() {
		SpanExporter spanExporter1 = mock(SpanExporter.class);
		SpanExporter spanExporter2 = mock(SpanExporter.class);
		SpanExporters spanExporters = SpanExporters.of(List.of(spanExporter1, spanExporter2));
		assertThat(spanExporters).containsExactly(spanExporter1, spanExporter2);
		assertThat(spanExporters.list()).containsExactly(spanExporter1, spanExporter2);
	}

	@Test
	void ofArray() {
		SpanExporter spanExporter1 = mock(SpanExporter.class);
		SpanExporter spanExporter2 = mock(SpanExporter.class);
		SpanExporters spanExporters = SpanExporters.of(spanExporter1, spanExporter2);
		assertThat(spanExporters).containsExactly(spanExporter1, spanExporter2);
		assertThat(spanExporters.list()).containsExactly(spanExporter1, spanExporter2);
	}

}
