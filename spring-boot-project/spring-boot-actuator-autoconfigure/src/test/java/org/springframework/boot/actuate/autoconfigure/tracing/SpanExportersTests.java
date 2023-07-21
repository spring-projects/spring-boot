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
