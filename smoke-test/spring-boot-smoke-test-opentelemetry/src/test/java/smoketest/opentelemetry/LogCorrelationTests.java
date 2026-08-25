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

package smoketest.opentelemetry;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.classpath.ForkedClassPath;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the correlation ID in the log output follows the MDC keys configured through
 * {@code management.tracing.mdc.*}.
 * <p>
 * Runs with a forked class path because {@code LOG_CORRELATION_PATTERN} is a JVM-wide
 * system property that is only ever written once, so a context started by another test
 * class would pin it to the default pattern.
 *
 * @author Moritz Halbritter
 */
@SpringBootTest(properties = { "management.tracing.mdc.trace-id-key=customTraceId",
		"management.tracing.mdc.span-id-key=customSpanId" })
@ExtendWith(OutputCaptureExtension.class)
@ForkedClassPath
class LogCorrelationTests {

	private static final Log logger = LogFactory.getLog(LogCorrelationTests.class);

	@Autowired
	private Tracer tracer;

	@Test
	void shouldUseCustomMdcKeysForCorrelationId(CapturedOutput output) {
		Span span = this.tracer.nextSpan().name("test");
		try (Tracer.SpanInScope scope = this.tracer.withSpan(span.start())) {
			logger.info("Hello from a span");
		}
		finally {
			span.end();
		}
		assertThat(output).containsPattern(
				"\\[%s-%s\\].*Hello from a span".formatted(span.context().traceId(), span.context().spanId()));
	}

}
