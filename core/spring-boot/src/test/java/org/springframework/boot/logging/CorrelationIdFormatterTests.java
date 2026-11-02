/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CorrelationIdFormatter}.
 *
 * @author Phillip Webb
 * @author Diego Magalhães
 */
class CorrelationIdFormatterTests {

	private static final String TRACE_ID = "01234567890123456789012345678901";

	private static final String SPAN_ID = "0123456789012345";

	private static final String BLANK = "[" + spaces(49) + "] ";

	@Test
	void formatWithDefaultSpecWhenHasBothParts() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasNoParts() {
		Map<String, String> context = new HashMap<>();
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[                                                 ] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasOnlyFirstPart() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-                ] ");
	}

	@Test
	void formatWithDefaultSpecWhenHasOnlySecondPart() {
		Map<String, String> context = new HashMap<>();
		context.put("spanId", "0123456789012345");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[                                -0123456789012345] ");
	}

	@Test
	void formatWhenPartsAreShort() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "0123456789012345678901234567");
		context.put("spanId", "012345678901");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[0123456789012345678901234567    -012345678901    ] ");
	}

	@Test
	void formatWhenPartsAreLong() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901FFFF");
		context.put("spanId", "0123456789012345FFFF");
		String formatted = CorrelationIdFormatter.DEFAULT.format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901FFFF-0123456789012345FFFF] ");
	}

	@Test
	void formatWithCustomSpec() {
		Map<String, String> context = new HashMap<>();
		context.put("a", "01234567890123456789012345678901");
		context.put("b", "0123456789012345");
		String formatted = CorrelationIdFormatter.of("a(32),b(16)").format(context::get);
		assertThat(formatted).isEqualTo("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void formatToWithDefaultSpec() {
		Map<String, String> context = new HashMap<>();
		context.put("traceId", "01234567890123456789012345678901");
		context.put("spanId", "0123456789012345");
		StringBuilder formatted = new StringBuilder();
		CorrelationIdFormatter.DEFAULT.formatTo(context::get, formatted);
		assertThat(formatted).hasToString("[01234567890123456789012345678901-0123456789012345] ");
	}

	@Test
	void ofWhenSpecIsMalformed() {
		assertThatIllegalStateException().isThrownBy(() -> CorrelationIdFormatter.of("good(12),bad"))
			.withMessage("Unable to parse correlation formatter spec 'good(12),bad'")
			.havingCause()
			.withMessage("Invalid specification part 'bad'");
	}

	@Test
	void ofWhenSpecIsEmpty() {
		assertThat(CorrelationIdFormatter.of("")).isSameAs(CorrelationIdFormatter.DEFAULT);
	}

	@Test
	void toStringReturnsSpec() {
		assertThat(CorrelationIdFormatter.DEFAULT).hasToString("traceId(32),spanId(16)");
		assertThat(CorrelationIdFormatter.of("a(32),b(16)")).hasToString("a(32),b(16)");
	}

	@ParameterizedTest
	@MethodSource("edgeLengths")
	void formatWhenValuesHaveEdgeLengths(CorrelationIdFormatter formatter, Map<String, String> values,
			String expected) {
		assertThat(formatter.format(values::get)).isEqualTo(expected);
		StringBuilder result = new StringBuilder("pre|");
		formatter.formatTo(values::get, result);
		assertThat(result).hasToString("pre|" + expected);
	}

	static Stream<Arguments> edgeLengths() {
		CorrelationIdFormatter formatter = CorrelationIdFormatter.DEFAULT;
		CorrelationIdFormatter custom = CorrelationIdFormatter.of("a(1),b(0),c(3)");
		CorrelationIdFormatter single = CorrelationIdFormatter.of("a(4)");
		return Stream.of(
				Arguments.of(formatter, Map.of("traceId", TRACE_ID, "spanId", SPAN_ID),
						"[" + TRACE_ID + "-" + SPAN_ID + "] "),
				Arguments.of(formatter, Collections.emptyMap(), BLANK),
				Arguments.of(formatter, Map.of("traceId", TRACE_ID), "[" + TRACE_ID + "-" + spaces(16) + "] "),
				Arguments.of(formatter, Map.of("spanId", SPAN_ID), "[" + spaces(32) + "-" + SPAN_ID + "] "),
				Arguments.of(formatter, Map.of("traceId", "", "spanId", ""), BLANK),
				Arguments.of(formatter, Map.of("traceId", " "), "[" + spaces(32) + "-" + spaces(16) + "] "),
				Arguments.of(formatter, Map.of("traceId", TRACE_ID + "FFFF", "spanId", SPAN_ID + "FFFF"),
						"[" + TRACE_ID + "FFFF-" + SPAN_ID + "FFFF] "),
				Arguments.of(custom, Map.of("a", "x", "c", "yy"), "[x--yy ] "),
				Arguments.of(formatter, Map.of("traceId", "caf\u00e9"),
						"[caf\u00e9" + spaces(28) + "-" + spaces(16) + "] "),
				Arguments.of(formatter, Map.of("traceId", "\uD83D\uDE00"),
						"[\uD83D\uDE00" + spaces(30) + "-" + spaces(16) + "] "),
				Arguments.of(formatter, Map.of("traceId", "a-b]c", "spanId", "]-"),
						"[a-b]c" + spaces(27) + "-]-" + spaces(14) + "] "),
				Arguments.of(single, Map.of("a", "z"), "[z   ] "),
				Arguments.of(single, Collections.emptyMap(), "[    ] "));
	}

	@Test
	void formatToWhenFormattingTwiceAppendsTwice() {
		Map<String, String> values = Map.of("traceId", TRACE_ID, "spanId", SPAN_ID);
		StringBuilder result = new StringBuilder();
		CorrelationIdFormatter.DEFAULT.formatTo(values::get, result);
		CorrelationIdFormatter.DEFAULT.formatTo(values::get, result);
		String expected = "[" + TRACE_ID + "-" + SPAN_ID + "] ";
		assertThat(result).hasToString(expected + expected);
	}

	@Test
	void formatAsksResolverForPartsInOrder() {
		CorrelationIdFormatter formatter = CorrelationIdFormatter.DEFAULT;
		assertThat(namesAsked(formatter, Map.of("traceId", TRACE_ID, "spanId", SPAN_ID))).containsExactly("traceId",
				"traceId", "spanId");
		assertThat(namesAsked(formatter, Collections.emptyMap())).containsExactly("traceId", "spanId");
		assertThat(namesAsked(formatter, Map.of("traceId", TRACE_ID))).containsExactly("traceId", "traceId", "spanId");
		assertThat(namesAsked(formatter, Map.of("spanId", SPAN_ID))).containsExactly("traceId", "spanId", "traceId",
				"spanId");
		assertThat(namesAsked(formatter, Map.of("traceId", "", "spanId", ""))).containsExactly("traceId", "spanId");
		assertThat(namesAsked(formatter, Map.of("traceId", " "))).containsExactly("traceId", "traceId", "spanId");
		assertThat(namesAsked(formatter, Map.of("traceId", TRACE_ID + "FFFF", "spanId", SPAN_ID + "FFFF")))
			.containsExactly("traceId", "traceId", "spanId");
		assertThat(namesAsked(CorrelationIdFormatter.of("a(1),b(0),c(3)"), Map.of("a", "x", "c", "yy")))
			.containsExactly("a", "a", "b", "c");
		assertThat(namesAsked(CorrelationIdFormatter.of("a(1),b(1),c(1)"), Map.of("c", "z"))).containsExactly("a", "b",
				"c", "a", "b", "c");
	}

	@Test
	@SuppressWarnings("NullAway") // Test null check
	void formatWithStatefulResolverUsesAnswersInCallOrder() {
		CorrelationIdFormatter formatter = CorrelationIdFormatter.DEFAULT;
		assertThat(formatter.format(RecordingResolver.answering("", "x", "y1", "z1")))
			.isEqualTo("[y1" + spaces(30) + "-z1" + spaces(14) + "] ");
		assertThat(formatter.format(RecordingResolver.answering(null, null))).isEqualTo(BLANK);
		assertThat(formatter.format(RecordingResolver.answering("a", null, null)))
			.isEqualTo("[" + spaces(32) + "-" + spaces(16) + "] ");
	}

	@Test
	void formatToAppendsInOrder() {
		RecordingAppendable both = new RecordingAppendable();
		CorrelationIdFormatter.DEFAULT.formatTo(RecordingResolver.of(Map.of("traceId", TRACE_ID, "spanId", SPAN_ID)),
				both);
		assertThat(both.entries).containsExactly("C:[", "S:" + TRACE_ID, "C:-", "S:" + SPAN_ID, "S:] ");
		RecordingAppendable neither = new RecordingAppendable();
		CorrelationIdFormatter.DEFAULT.formatTo(RecordingResolver.of(Collections.emptyMap()), neither);
		assertThat(neither.entries).containsExactly("S:" + BLANK);
		RecordingAppendable custom = new RecordingAppendable();
		CorrelationIdFormatter.of("a(1),b(0),c(3)").formatTo(RecordingResolver.of(Map.of("a", "x", "c", "yy")), custom);
		assertThat(custom.entries).containsExactly("C:[", "S:x", "C:-", "S:", "C:-", "S:yy ", "S:] ");
	}

	@Test
	void formatToWhenResolverThrowsPropagatesException() {
		RecordingAppendable first = new RecordingAppendable();
		assertThatIllegalStateException()
			.isThrownBy(() -> CorrelationIdFormatter.DEFAULT
				.formatTo(throwingOnCall(1, new IllegalStateException("boom")), first))
			.withMessage("boom");
		assertThat(first.entries).isEmpty();
		RecordingAppendable second = new RecordingAppendable();
		assertThatIllegalStateException()
			.isThrownBy(() -> CorrelationIdFormatter.DEFAULT
				.formatTo(throwingOnCall(2, new IllegalStateException("boom")), second))
			.withMessage("boom");
		assertThat(second.entries).containsExactly("C:[");
		RecordingAppendable third = new RecordingAppendable();
		assertThatIllegalStateException()
			.isThrownBy(() -> CorrelationIdFormatter.DEFAULT
				.formatTo(throwingOnCall(3, new IllegalStateException("boom")), third))
			.withMessage("boom");
		assertThat(third.entries).containsExactly("C:[", "S:" + TRACE_ID, "C:-");
	}

	@Test
	void formatToWhenIOExceptionWrapsInUncheckedIOException() {
		RecordingResolver both = RecordingResolver.of(Map.of("traceId", TRACE_ID, "spanId", SPAN_ID));
		assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(
				() -> CorrelationIdFormatter.DEFAULT.formatTo(both, new FailingAppendable(true, new IOException("io"))))
			.havingCause()
			.withMessage("io");
		assertThat(both.names).containsExactly("traceId");
		RecordingResolver none = RecordingResolver.of(Collections.emptyMap());
		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(() -> CorrelationIdFormatter.DEFAULT.formatTo(none,
					new FailingAppendable(false, new IOException("blank"))))
			.havingCause()
			.withMessage("blank");
		assertThat(none.names).containsExactly("traceId", "spanId");
		IOException exception = new IOException("resolver");
		assertThatExceptionOfType(UncheckedIOException.class)
			.isThrownBy(
					() -> CorrelationIdFormatter.DEFAULT.formatTo(throwingOnCall(1, exception), new StringBuilder()))
			.havingCause()
			.isSameAs(exception);
	}

	private static List<@Nullable String> namesAsked(CorrelationIdFormatter formatter, Map<String, String> values) {
		RecordingResolver resolver = RecordingResolver.of(values);
		formatter.format(resolver);
		return resolver.names;
	}

	private static UnaryOperator<@Nullable String> throwingOnCall(int call, Throwable exception) {
		Map<String, String> values = Map.of("traceId", TRACE_ID, "spanId", SPAN_ID);
		AtomicInteger calls = new AtomicInteger();
		return (name) -> {
			if (calls.incrementAndGet() == call) {
				throw CorrelationIdFormatterTests.<RuntimeException>sneakyThrow(exception);
			}
			return values.get(name);
		};
	}

	@SuppressWarnings("unchecked")
	private static <E extends Throwable> E sneakyThrow(Throwable exception) throws E {
		throw (E) exception;
	}

	private static String spaces(int count) {
		return " ".repeat(count);
	}

	/**
	 * Resolver that records each requested name and answers from a map or from a sequence
	 * of answers.
	 */
	private static final class RecordingResolver implements UnaryOperator<@Nullable String> {

		private final List<@Nullable String> names = new ArrayList<>();

		private final UnaryOperator<@Nullable String> answers;

		private RecordingResolver(UnaryOperator<@Nullable String> answers) {
			this.answers = answers;
		}

		@Override
		public @Nullable String apply(@Nullable String name) {
			this.names.add(name);
			return this.answers.apply(name);
		}

		static RecordingResolver of(Map<String, String> values) {
			return new RecordingResolver(values::get);
		}

		static RecordingResolver answering(@Nullable String... answers) {
			Iterator<@Nullable String> iterator = Arrays.asList(answers).iterator();
			return new RecordingResolver((name) -> iterator.next());
		}

	}

	/**
	 * {@link Appendable} that records each call.
	 */
	private static final class RecordingAppendable implements Appendable {

		private final List<String> entries = new ArrayList<>();

		@Override
		public Appendable append(@Nullable CharSequence csq) {
			this.entries.add("S:" + csq);
			return this;
		}

		@Override
		public Appendable append(@Nullable CharSequence csq, int start, int end) {
			this.entries.add("R:" + csq + "," + start + "," + end);
			return this;
		}

		@Override
		public Appendable append(char c) {
			this.entries.add("C:" + c);
			return this;
		}

	}

	/**
	 * {@link Appendable} that throws on the first {@code append(char)} call or on the
	 * first {@code append(CharSequence)} call.
	 */
	private static final class FailingAppendable implements Appendable {

		private final boolean failOnChar;

		private final IOException exception;

		FailingAppendable(boolean failOnChar, IOException exception) {
			this.failOnChar = failOnChar;
			this.exception = exception;
		}

		@Override
		public Appendable append(@Nullable CharSequence csq) throws IOException {
			if (!this.failOnChar) {
				throw this.exception;
			}
			return this;
		}

		@Override
		public Appendable append(@Nullable CharSequence csq, int start, int end) throws IOException {
			return append(csq);
		}

		@Override
		public Appendable append(char c) throws IOException {
			if (this.failOnChar) {
				throw this.exception;
			}
			return this;
		}

	}

}
